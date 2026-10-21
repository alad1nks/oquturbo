"""Exercise release allocation in real isolated Git repositories (no network)."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

import prepare_release as release

SCRIPT = Path(release.__file__).resolve()


class PrepareReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git('init', '-b', 'main')
        self.git('config', 'user.name', 'Test')
        self.git('config', 'user.email', 'test@example.com')
        for app in release.APPS:
            android = self.root / f'app/{app}/androidApp/build.gradle.kts'
            ios = self.root / f'app/{app}/iosApp/Configuration/Config.xcconfig'
            android.parent.mkdir(parents=True)
            ios.parent.mkdir(parents=True)
            android.write_text('versionCode = 1\nversionName = "1.0.0"\n')
            ios.write_text('MARKETING_VERSION=1.0\n')
        self.git('add', '.')
        self.git('commit', '-m', 'Initial')
        self.git('update-ref', 'refs/remotes/origin/main', 'HEAD')

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.root, text=True, stderr=subprocess.DEVNULL).strip()

    def run_release(self, app='oquturbo', run='123', success=True):
        # Outputs live outside the checkout so they cannot make it dirty on retry.
        with tempfile.TemporaryDirectory() as output:
            env = dict(os.environ, RELEASE_APP=app, GITHUB_RUN_ID=run,
                       GITHUB_OUTPUT=output+'/output', GITHUB_STEP_SUMMARY=output+'/summary')
            result = subprocess.run(['python3', str(SCRIPT)], cwd=self.root, env=env,
                                    capture_output=True, text=True)
            if success:
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                return Path(output+'/output').read_text()
            self.assertNotEqual(result.returncode, 0)
            return result.stderr

    def remote_release(self, app, version, code=10, marker=None):
        self.git('switch', '-C', 'fixture', 'origin/main')
        (self.root / f'app/{app}/androidApp/build.gradle.kts').write_text(
            f'versionCode = {code}\nversionName = "{version}"\n')
        self.git('add', '.')
        self.git('commit', '-m', marker or 'Release fixture')
        ref = f'refs/remotes/origin/release/{app}/{version}'
        self.git('update-ref', ref, 'HEAD')
        self.git('switch', 'main')
        return ref

    def test_each_app_changes_only_its_versions_and_leaves_main(self):
        main = self.git('rev-parse', 'main')
        for app in release.APPS:
            with self.subTest(app=app):
                self.git('switch', 'main')
                output = self.run_release(app)
                self.assertIn(f'branch=release/{app}/1.1\n', output)
                changed = self.git('diff', '--name-only', 'origin/main').splitlines()
                self.assertEqual(changed, [f'app/{app}/androidApp/build.gradle.kts',
                                          f'app/{app}/iosApp/Configuration/Config.xcconfig'])
                self.assertIn('versionCode = 2', (self.root / changed[0]).read_text())
                self.assertIn('MARKETING_VERSION=1.1', (self.root / changed[1]).read_text())
                self.assertEqual(self.git('rev-parse', 'HEAD^'), main)
                self.assertEqual(self.git('rev-parse', 'main'), main)

    def test_numeric_order_max_code_and_other_apps_are_isolated(self):
        self.remote_release('oquturbo', '1.9', 50)
        self.remote_release('oquturbo', '1.10', 20)
        self.remote_release('baspa', '9.99', 999)
        self.remote_release('oquturbo', '99.0-beta', 999)
        self.assertIn('branch=release/oquturbo/1.11', self.run_release())
        self.assertIn('versionCode = 51', (self.root / 'app/oquturbo/androidApp/build.gradle.kts').read_text())

    def test_retry_reuses_pushed_branch_but_rejects_advanced_branch(self):
        self.run_release()
        self.git('update-ref', 'refs/remotes/origin/release/oquturbo/1.1', 'HEAD')
        self.git('switch', 'main')
        self.assertIn('created=false', self.run_release())
        self.git('switch', 'release/oquturbo/1.1')
        self.git('commit', '--allow-empty', '-m', 'Later change')
        self.git('update-ref', 'refs/remotes/origin/release/oquturbo/1.1', 'HEAD')
        self.git('switch', 'main')
        self.assertIn('advanced', self.run_release(success=False))

    def test_rejects_downgrade_overflow_dirty_checkout_and_invalid_app(self):
        ref = self.remote_release('oquturbo', '0.1')
        self.assertIn('below main', self.run_release(success=False))
        self.git('update-ref', '-d', ref)
        ref = self.remote_release('oquturbo', '1.1', 2100000000)
        self.assertIn('Google Play limit', self.run_release(success=False))
        self.git('update-ref', '-d', ref)
        (self.root / 'untracked').write_text('dirty')
        self.assertIn('clean', self.run_release(success=False))
        self.assertIn('Unknown', self.run_release('../bad', success=False))

    def test_app_inventory_matches_modules(self):
        root = SCRIPT.parents[2]
        self.assertEqual(set(release.APPS), {p.parent.parent.name for p in root.glob('app/*/androidApp/build.gradle.kts')})
        for app in release.APPS:
            for prefix in ('create-release', 'release'):
                self.assertTrue((root / f'.github/workflows/{prefix}-{app}.yml').is_file())


if __name__ == '__main__':
    unittest.main()
