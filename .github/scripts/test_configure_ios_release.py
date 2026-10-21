"""Portable signing tests plus real Xcode project checks on macOS."""
import copy
import hashlib
import json
import os
from pathlib import Path
import plistlib
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import datetime

from configure_ios_release import configure_project, signing_identity, main
from prepare_release import APPS

ROOT = Path(__file__).resolve().parents[2]


def fixture():
    return {'rootObject': 'root', 'objects': {
        'root': {'targets': ['app']},
        'app': {'name': 'iosApp', 'productType': 'com.apple.product-type.application',
                'buildConfigurationList': 'configs'},
        'configs': {'buildConfigurations': ['debug', 'release']},
        'debug': {'name': 'Debug', 'buildSettings': {}},
        'release': {'name': 'Release', 'buildSettings': {'PROVISIONING_PROFILE': 'old'}},
    }}


class IosReleaseTests(unittest.TestCase):
    def test_only_release_app_settings_change(self):
        project = fixture()
        before = copy.deepcopy(project)
        configure_project(project, 'TEAM', 'Profile name', '2.1.1', framework='AppBaspa')
        self.assertEqual([key for key in project['objects'] if project['objects'][key] != before['objects'][key]], ['release'])
        settings = project['objects']['release']['buildSettings']
        self.assertNotIn('PROVISIONING_PROFILE', settings)
        self.assertEqual(settings['OTHER_LDFLAGS'], ['$(inherited)', '-framework', 'AppBaspa'])
        self.assertEqual(settings['CURRENT_PROJECT_VERSION'], '2.1.1')
        self.assertEqual(plistlib.loads(plistlib.dumps(project)), project)

    def test_identity_must_match(self):
        cert = b'certificate'
        fingerprint = hashlib.sha1(cert).hexdigest().upper()
        self.assertEqual(signing_identity({'DeveloperCertificates': [cert]}, fingerprint), fingerprint)
        with self.assertRaises(SystemExit):
            signing_identity({'DeveloperCertificates': [cert]}, '0 valid identities')

    def test_profiles_and_exports_for_all_apps(self):
        import re
        for app in APPS:
            with self.subTest(app=app), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                config = (ROOT / f'app/{app}/iosApp/Configuration/Config.xcconfig').read_text()
                bundle = re.search(r'PRODUCT_BUNDLE_IDENTIFIER=(.*)', config)[1].replace('$(TEAM_ID)', '')
                profile = {'UUID': '11111111-2222-3333-4444-555555555555', 'Name': 'Distribution',
                           'TeamIdentifier': ['TEAM'], 'ApplicationIdentifierPrefix': ['TEAM'],
                           'ExpirationDate': datetime.datetime.now() + datetime.timedelta(days=30),
                           'DeveloperCertificates': [b'cert'],
                           'Entitlements': {'application-identifier': 'TEAM.'+bundle, 'beta-reports-active': True}}
                args = ['script', str(root/'profile'), str(root/'export'), str(root/'project'), '2.1.1', 'keychain']
                fingerprint = hashlib.sha1(b'cert').hexdigest().upper()
                def command(args, **kwargs):
                    return json.dumps(fixture()).encode() if args[0] == 'plutil' else fingerprint
                def invoke():
                    (root/'profile').write_bytes(plistlib.dumps(profile))
                    with patch.dict(os.environ, RELEASE_APP=app, APPLE_TEAM_ID='TEAM'), patch.object(sys, 'argv', args), patch('subprocess.check_output', side_effect=command), patch('builtins.print'):
                        main()
                invoke()
                self.assertEqual(plistlib.loads((root/'export').read_bytes())['provisioningProfiles'], {bundle: profile['UUID']})
                for bad in ({'TeamIdentifier': ['WRONG']}, {'ProvisionedDevices': ['device']},
                            {'ExpirationDate': datetime.datetime(2000, 1, 1)},
                            {'Entitlements': {'application-identifier': 'TEAM.wrong'}}):
                    original = copy.deepcopy(profile)
                    profile.update(bad)
                    with self.assertRaises(SystemExit):
                        invoke()
                    profile = original

    @unittest.skipUnless(sys.platform == 'darwin', 'Real project conversion needs macOS plutil')
    def test_real_xcode_projects(self):
        import re
        for app in APPS:
            project = json.loads(subprocess.check_output(['plutil', '-convert', 'json', '-o', '-',
                str(ROOT / f'app/{app}/iosApp/iosApp.xcodeproj/project.pbxproj')]))
            before = copy.deepcopy(project)
            framework = re.search(r'baseName\s*=\s*"([^"]+)"', (ROOT / f'app/{app}/shared/build.gradle.kts').read_text())[1]
            configure_project(project, 'TEAM', 'Profile', '2.1.1', framework=framework)
            self.assertEqual(sum(before['objects'][key] != value for key, value in project['objects'].items()), 1)


if __name__ == '__main__':
    unittest.main()
