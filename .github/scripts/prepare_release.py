"""Prepare one release branch/commit from origin/main; the workflow pushes and dispatches it."""

import os
from pathlib import Path
import re
import subprocess
import sys


APP = os.environ.get("RELEASE_APP", "")
APPS = ('baspa', 'dualfocus', 'kenkoz', 'numbertrail', 'oquturbo', 'rotationmatch', 'ruleswitch', 'sansprint', 'symbolcount', 'wordflow')
ANDROID = f"app/{APP}/androidApp/build.gradle.kts"
IOS = f"app/{APP}/iosApp/Configuration/Config.xcconfig"
VERSION = r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"
RELEASE_REF = re.compile(rf"refs/remotes/origin/release/{APP}/{VERSION}")
VERSION_NAME = r'(?m)^(\s*versionName\s*=\s*")([^"\r\n]+)("[ \t]*)$'
VERSION_CODE = r"(?m)^(\s*versionCode\s*=\s*)([0-9]+)([ \t]*)$"
MARKETING_VERSION = r"(?m)^(MARKETING_VERSION[ \t]*=[ \t]*)([^\s]+)([ \t]*)$"


def git(*args, strip=True):
    output = subprocess.check_output(["git", *args], text=True, encoding="utf-8")
    return output.strip() if strip else output


def value(text, pattern):
    matches = list(re.finditer(pattern, text))
    if len(matches) != 1:
        raise ValueError(f"Expected exactly one version setting matching {pattern}")
    return matches[0][2]


def version_tuple(version):
    if not re.fullmatch(VERSION + r"(?:\.(0|[1-9][0-9]*))?", version):
        raise ValueError(f"Expected a numeric major.minor[.patch] version, got {version!r}")
    return tuple(map(int, version.split("."))) + ((0,) if version.count(".") == 1 else ())


def replace(text, pattern, replacement):
    value(text, pattern)
    return re.sub(pattern, lambda match: match[1] + str(replacement) + match[3], text)


def prepare(run_id):
    if APP not in APPS:
        raise ValueError("Unknown RELEASE_APP")
    if not run_id.isdecimal():
        raise ValueError("GITHUB_RUN_ID must be numeric")
    if git("status", "--porcelain"):
        raise ValueError("The checkout must be clean")
    refs = git("for-each-ref", "--format=%(refname)", "refs/remotes/origin/release/").splitlines()
    releases = sorted(
        (version_tuple(ref.removeprefix(f"refs/remotes/origin/release/{APP}/")), ref)
        for ref in refs if RELEASE_REF.fullmatch(ref)
    )
    marker = f"Release-Workflow-Run: {run_id}"
    # A retry after a successful push must reuse that branch, rather than allocate another version.
    for version, ref in releases:
        previous = git("log", "-1", "--format=%H", f"--grep=^{marker}$", ref)
        if previous:
            if previous != git("rev-parse", ref):
                raise ValueError(f"{ref} advanced after creation; run Release manually on that branch")
            return ref.removeprefix("refs/remotes/origin/"), ".".join(map(str, version[:2])), False

    android = git("show", f"origin/main:{ANDROID}", strip=False)
    ios = git("show", f"origin/main:{IOS}", strip=False)
    main_version = max(version_tuple(value(android, VERSION_NAME)), version_tuple(value(ios, MARKETING_VERSION)))
    latest = releases[-1][0] if releases else main_version
    next_version = (latest[0], latest[1] + 1, 0)
    if next_version < main_version:
        raise ValueError("Next release version would be below main; reconcile release branches and main first")
    version = ".".join(map(str, next_version[:2]))
    codes = [int(value(android, VERSION_CODE))]
    for _, ref in releases:
        codes.append(int(value(git("show", f"{ref}:{ANDROID}"), VERSION_CODE)))
    code = max(codes) + 1
    if code > 2100000000:
        raise ValueError("Android versionCode exceeds the Google Play limit")
    updated_android = replace(replace(android, VERSION_NAME, version), VERSION_CODE, code)
    updated_ios = replace(ios, MARKETING_VERSION, version)
    branch = f"release/{APP}/{version}"
    git("switch", "--create", branch, "origin/main")
    for path, contents in ((ANDROID, updated_android), (IOS, updated_ios)):
        with Path(path).open("w", encoding="utf-8", newline="\n") as target:
            target.write(contents)
    git("add", "--", ANDROID, IOS)
    git("-c", "user.name=github-actions[bot]", "-c", "user.email=41898282+github-actions[bot]@users.noreply.github.com",
        "commit", "-m", f"chore: bump release version to {version}", "-m", marker)
    return branch, version, True


def main():
    branch, version, created = prepare(os.environ["GITHUB_RUN_ID"])
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"branch={branch}\nversion={version}\ncreated={str(created).lower()}\n")
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
        summary.write(f"## Release {version}\n\nBranch: `{branch}`. "
                      f"{'Prepared from main' if created else 'Reused from this run'}.\n\n"
                      "If push succeeds but dispatch fails, re-run this workflow run to reuse the branch.\n")
    print(f"{'Prepared' if created else 'Reusing'} {branch}")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, subprocess.CalledProcessError) as error:
        sys.exit(f"::error::{error}")
