"""Publish one validated PR APK as data; never execute source-run content."""

import json
import os
import re
import shutil
import stat
import subprocess
import sys
import zipfile
from datetime import datetime
from pathlib import Path

PRODUCTS = {name: f"pr-checks-{name}.yml" for name in ("oquturbo", "sansprint", "kenkoz", "baspa")}
MAX_APK_BYTES = 1024 * 1024 * 1024


def require(condition, message):
    if not condition:
        raise ValueError(message)


def positive_id(value):
    require(isinstance(value, (int, str)) and not isinstance(value, bool), "Missing numeric ID")
    require(re.fullmatch(r"[1-9][0-9]{0,19}", str(value)), "Invalid numeric ID")
    return int(value)


def product_name(value):
    require(isinstance(value, str) and value in PRODUCTS, "Unsupported product")
    return value


def branch_path(value):
    require(isinstance(value, str) and 0 < len(value) <= 240, "Missing or oversized source branch")
    # Slash branches retain their existing preview directories; reject special/path syntax.
    require(all(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,99}", part) for part in value.split("/")),
            "Source branch must contain safe alphanumeric path segments")
    require(".." not in value and not any(part.endswith((".", ".lock")) for part in value.split("/")),
            "Unsafe source branch")
    return value


def timestamp(value):
    require(isinstance(value, str), "Missing source timestamp")
    result = datetime.fromisoformat(value.replace("Z", "+00:00"))
    require(result.tzinfo is not None, "Source timestamp needs a timezone")
    return result


def mapping(value):
    require(isinstance(value, dict), "Missing API object")
    return value


def validate_run(run, workflow, repository, run_id, product):
    run, workflow = mapping(run), mapping(workflow)
    require(run.get("id") == run_id, "Source run ID mismatch")
    require(mapping(run.get("repository")).get("full_name") == repository, "Source repository mismatch")
    expected = f".github/workflows/{PRODUCTS[product]}"
    require(workflow.get("path") == expected and run.get("path") == expected, "Source workflow path mismatch")
    require(positive_id(workflow.get("id")) == run.get("workflow_id"), "Source workflow ID mismatch")
    require(run.get("event") == "pull_request" and run.get("status") == "completed", "Source must be a completed PR run")
    require(isinstance(run.get("head_sha"), str) and re.fullmatch(r"[0-9a-f]{40}", run["head_sha"]), "Missing source SHA")
    return branch_path(run.get("head_branch")), positive_id(run.get("run_attempt"))


def validate_build(jobs, run_id, attempt):
    require(isinstance(jobs, list) and all(isinstance(job, dict) for job in jobs), "Missing jobs")
    matches = [job for job in jobs if job.get("name") == "build-android"]
    require(len(matches) == 1, "Expected one Android build in the current attempt")
    job = matches[0]
    require(job.get("run_id") == run_id and job.get("run_attempt") == attempt, "Build run/attempt mismatch")
    require(job.get("status") == "completed" and job.get("conclusion") == "success", "Android build did not succeed")
    require(timestamp(job.get("started_at")) <= timestamp(job.get("completed_at")), "Invalid build timestamps")
    return job


def validate_artifact(artifacts, run, job):
    require(isinstance(artifacts, list) and all(isinstance(item, dict) for item in artifacts), "Missing artifacts")
    matches = [item for item in artifacts if item.get("name") == "app-release"]
    require(len(matches) == 1, "Expected one app-release artifact")
    artifact = matches[0]
    require(artifact.get("expired") is False, "Artifact expired or missing expiry metadata")
    source = mapping(artifact.get("workflow_run"))
    require(source.get("id") == run["id"] and source.get("head_sha") == run["head_sha"], "Artifact source mismatch")
    # A rerun must not silently publish an artifact left by a different build attempt.
    created = timestamp(artifact.get("created_at"))
    require(timestamp(job["started_at"]) <= created <= timestamp(job["completed_at"]), "Artifact is outside successful build attempt")
    require(type(artifact.get("size_in_bytes")) is int and 0 < artifact["size_in_bytes"] <= MAX_APK_BYTES,
            "Invalid artifact size")
    return positive_id(artifact.get("id"))


def api(endpoint):
    return json.loads(subprocess.check_output(["gh", "api", endpoint]))


def api_items(endpoint, key):
    pages = json.loads(subprocess.check_output(["gh", "api", "--paginate", "--slurp", endpoint]))
    require(isinstance(pages, list), "Missing API pages")
    items = []
    for page in pages:
        values = mapping(page).get(key)
        require(isinstance(values, list), f"Missing API {key}")
        items.extend(values)
    return items


def unpack_apk(archive, destination, product):
    expected = f"{product_name(product)}.apk"
    with zipfile.ZipFile(archive) as zipped:
        files = zipped.infolist()
        require(len(files) == 1 and files[0].filename == expected, "Artifact must contain only the expected APK")
        info = files[0]
        file_type = stat.S_IFMT(info.external_attr >> 16)
        require(not info.is_dir() and file_type in (0, stat.S_IFREG), "APK must be a regular file")
        require(0 < info.file_size <= MAX_APK_BYTES, "APK is empty or oversized")
        # No extractall: archive paths and executable bits never reach the filesystem.
        with zipped.open(info) as source, destination.open("xb") as target:
            shutil.copyfileobj(source, target)
        require(destination.stat().st_size == info.file_size, "APK size mismatch")
        destination.chmod(0o644)


def prepare():
    product = product_name(os.environ.get("PRODUCT"))
    run_id = positive_id(os.environ.get("SOURCE_RUN_ID"))
    repository = os.environ.get("GITHUB_REPOSITORY", "")
    require(re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository), "Invalid repository")
    root = f"repos/{repository}/actions"
    run = api(f"{root}/runs/{run_id}")
    workflow = api(f"{root}/workflows/{PRODUCTS[product]}")
    branch, attempt = validate_run(run, workflow, repository, run_id, product)
    jobs = api_items(f"{root}/runs/{run_id}/attempts/{attempt}/jobs?per_page=100", "jobs")
    job = validate_build(jobs, run_id, attempt)
    artifacts = api_items(f"{root}/runs/{run_id}/artifacts?per_page=100", "artifacts")
    artifact_id = validate_artifact(artifacts, run, job)
    staging = Path(os.environ["RUNNER_TEMP"]) / "preview-apk"
    staging.mkdir()
    archive = staging / "artifact.zip"
    with archive.open("xb") as output:
        subprocess.run(["gh", "api", f"{root}/artifacts/{artifact_id}/zip"], stdout=output, check=True)
    require(0 < archive.stat().st_size <= MAX_APK_BYTES, "Invalid downloaded archive size")
    unpack_apk(archive, staging / f"{product}.apk", product)
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"branch={branch}\n")
    print(f"Validated {product} run {run_id} attempt {attempt} artifact {artifact_id}")


def publication_path(root, product, branch):
    relative = Path("oquturbo") / product_name(product) / branch_path(branch) / f"{product}.apk"
    path = root
    for part in relative.parts:
        path = path / part
        require(not path.is_symlink(), "Publication path contains a symlink")
    require(path.resolve().is_relative_to(root.resolve()), "Publication path escaped checkout")
    return path, relative


def publish():
    product = product_name(os.environ.get("PRODUCT"))
    root = Path(os.environ["GITHUB_WORKSPACE"]) / "publication"
    destination, relative = publication_path(root, product, os.environ.get("SOURCE_BRANCH"))
    source = Path(os.environ["RUNNER_TEMP"]) / "preview-apk" / f"{product}.apk"
    require(source.is_file() and not source.is_symlink() and source.stat().st_size > 0, "Missing validated APK")
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, destination)
    # Argument arrays and literal pathspecs: metadata never becomes shell or Git pathspec code.
    def git(*args, **kwargs):
        return subprocess.run(["git", "--literal-pathspecs", "-C", str(root), *args], check=True, **kwargs)
    git("config", "--local", "user.name", "alad1nks")
    git("config", "--local", "user.email", "alad1nks@users.noreply.github.com")
    git("add", "--", str(relative))
    changed = git("diff", "--cached", "--name-only", capture_output=True, text=True).stdout
    if not changed:
        print("APK already published; no commit needed")
        return
    require(changed.strip() == str(relative), "Unexpected staged publication files")
    git("commit", "-m", f"Add generated APK file for {product}")
    git("push")


if __name__ == "__main__":
    try:
        require(len(sys.argv) == 2 and sys.argv[1] in ("prepare", "publish"), "Expected prepare or publish")
        {"prepare": prepare, "publish": publish}[sys.argv[1]]()
    except (ValueError, KeyError, OSError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        print(f"Publication failed: {error}", file=sys.stderr)
        sys.exit(1)
