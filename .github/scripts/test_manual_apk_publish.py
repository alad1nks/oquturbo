import io
import stat
import tempfile
import unittest
import zipfile
from pathlib import Path

import manual_apk_publish as publisher


class PublicationValidationTest(unittest.TestCase):
    def setUp(self):
        self.run = {
            "id": 42, "repository": {"full_name": "alad1nks/oquturbo"},
            "path": ".github/workflows/pr-checks-oquturbo.yml", "workflow_id": 7,
            "status": "completed", "conclusion": "failure", "event": "pull_request",
            "head_sha": "a" * 40, "head_branch": "feature/result-card", "run_attempt": 2,
        }
        self.workflow = {"id": 7, "path": self.run["path"]}
        self.job = {
            "name": "build-android", "run_id": 42, "run_attempt": 2,
            "status": "completed", "conclusion": "success",
            "started_at": "2026-10-04T10:00:00Z", "completed_at": "2026-10-04T10:05:00Z",
        }
        self.artifact = {
            "id": 81, "name": "app-release", "expired": False, "size_in_bytes": 100,
            "created_at": "2026-10-04T10:04:00Z", "workflow_run": {"id": 42, "head_sha": "a" * 40},
        }

    def validate_run(self, run):
        return publisher.validate_run(run, self.workflow, "alad1nks/oquturbo", 42, "oquturbo")

    def test_old_publication_failure_does_not_reject_successful_android_build(self):
        self.assertEqual(("feature/result-card", 2), self.validate_run(self.run))
        self.assertEqual(self.job, publisher.validate_build([self.job], 42, 2))
        self.assertEqual(81, publisher.validate_artifact([self.artifact], self.run, self.job))

    def test_inputs_and_branch_paths_reject_injection_and_traversal(self):
        for value in [None, "", "0", "-1", "42/../9", "42\n1", "$(id)", True]:
            with self.subTest(run_id=value), self.assertRaises(ValueError):
                publisher.positive_id(value)
        for value in [None, "", "/main", "../main", "a/../b", "a//b", "a/.git/b", "a\\b",
                      "a\nb", "$(id)", "a;id", "a@{b}", "a.lock", "a/", "a/‮b"]:
            with self.subTest(branch=value), self.assertRaises(ValueError):
                publisher.branch_path(value)
        for value in [None, "../../oquturbo", "wordflow"]:
            with self.subTest(product=value), self.assertRaises(ValueError):
                publisher.product_name(value)
        self.assertEqual("42", str(publisher.positive_id("42")))
        self.assertEqual("feature/result-card.v2", publisher.branch_path("feature/result-card.v2"))

    def test_wrong_source_or_incomplete_metadata_fails_closed(self):
        for key, value in [
            ("id", 43), ("repository", None), ("repository", {"full_name": "other/repo"}),
            ("workflow_id", 9), ("path", ".github/workflows/pr-checks-baspa.yml"),
            ("event", "push"), ("status", "in_progress"), ("head_sha", None), ("run_attempt", None),
        ]:
            run = dict(self.run, **{key: value})
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                self.validate_run(run)
        for value in [None, {}, []]:
            with self.subTest(run=value), self.assertRaises(ValueError):
                self.validate_run(value)

    def test_current_attempt_job_must_have_succeeded(self):
        for key, value in [("run_attempt", 1), ("run_id", 41), ("conclusion", "failure"),
                           ("conclusion", "skipped"), ("status", "in_progress"), ("completed_at", None)]:
            with self.subTest(key=key), self.assertRaises(ValueError):
                publisher.validate_build([dict(self.job, **{key: value})], 42, 2)
        for jobs in [None, [], [self.job, self.job], [None]]:
            with self.subTest(jobs=jobs), self.assertRaises(ValueError):
                publisher.validate_build(jobs, 42, 2)

    def test_expired_duplicate_wrong_run_and_previous_attempt_artifacts_fail(self):
        for key, value in [
            ("expired", True), ("expired", None), ("id", None), ("size_in_bytes", 0),
            ("workflow_run", None), ("workflow_run", {"id": 41, "head_sha": "a" * 40}),
            ("workflow_run", {"id": 42, "head_sha": "b" * 40}),
            ("created_at", "2026-10-03T10:04:00Z"), ("created_at", None),
        ]:
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                publisher.validate_artifact([dict(self.artifact, **{key: value})], self.run, self.job)
        for artifacts in [None, [], [self.artifact, self.artifact], [None]]:
            with self.subTest(artifacts=artifacts), self.assertRaises(ValueError):
                publisher.validate_artifact(artifacts, self.run, self.job)

    @staticmethod
    def archive(entries):
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w") as zipped:
            for name, data, mode in entries:
                info = zipfile.ZipInfo(name)
                info.external_attr = mode << 16
                zipped.writestr(info, data)
        buffer.seek(0)
        return buffer

    def test_only_expected_nonempty_regular_apk_is_unpacked_without_execution_bits(self):
        with tempfile.TemporaryDirectory() as folder:
            destination = Path(folder) / "oquturbo.apk"
            archive = self.archive([("oquturbo.apk", b"opaque APK bytes", stat.S_IFREG | 0o755)])
            publisher.unpack_apk(archive, destination, "oquturbo")
            self.assertEqual(b"opaque APK bytes", destination.read_bytes())
            self.assertEqual(0o644, stat.S_IMODE(destination.stat().st_mode))

    def test_archive_paths_symlinks_extra_files_and_empty_apks_are_rejected(self):
        fixtures = [
            [("../oquturbo.apk", b"x", stat.S_IFREG)],
            [("/oquturbo.apk", b"x", stat.S_IFREG)],
            [("oquturbo.apk", b"outside", stat.S_IFLNK)],
            [("oquturbo.apk", b"", stat.S_IFREG)],
            [("oquturbo.apk", b"x", stat.S_IFREG), ("run.sh", b"code", stat.S_IFREG)],
            [("baspa.apk", b"x", stat.S_IFREG)],
        ]
        for entries in fixtures:
            with self.subTest(entries=entries), tempfile.TemporaryDirectory() as folder:
                destination = Path(folder) / "oquturbo.apk"
                with self.assertRaises(ValueError):
                    publisher.unpack_apk(self.archive(entries), destination, "oquturbo")
                self.assertFalse(destination.exists())

    def test_all_existing_product_destination_mappings_preserve_slash_branches(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            for product in publisher.PRODUCTS:
                path, relative = publisher.publication_path(root, product, "feature/result-card")
                self.assertEqual(f"oquturbo/{product}/feature/result-card/{product}.apk", str(relative))
                self.assertEqual(root / relative, path)

    def test_existing_target_symlink_cannot_escape_or_redirect_publication(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder) / "site"
            root.mkdir()
            (root / "oquturbo").symlink_to(Path(folder), target_is_directory=True)
            with self.assertRaises(ValueError):
                publisher.publication_path(root, "oquturbo", "main")


if __name__ == "__main__":
    unittest.main()
