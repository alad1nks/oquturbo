#!/usr/bin/env python3
"""Validate release inputs and configure app-only signing in an ephemeral CI checkout."""
import datetime
import hashlib
import json
import os
import pathlib
import plistlib
import re
import subprocess
import sys
import uuid


def fail(message):
    raise SystemExit(f"::error::{message}")


def signing_identity(profile, identities):
    valid = set(re.findall(r"\b[0-9A-Fa-f]{40}\b", identities.upper()))
    for certificate in profile.get("DeveloperCertificates", []):
        fingerprint = hashlib.sha1(certificate).hexdigest().upper()
        if fingerprint in valid:
            return fingerprint
    fail("No valid signing identity matches the provisioning profile. Check the distribution "
         "certificate/private key and regenerate the profile for that certificate if necessary.")


def configure_project(project, team_id, profile_name, build_number, identity="Apple Distribution", framework="AppOquTurbo"):
    """Override only the app's Release target, never project or package settings."""
    objects = project["objects"]
    targets = [objects[key] for key in objects[project["rootObject"]]["targets"]
               if objects[key].get("name") == "iosApp"
               and objects[key].get("productType") == "com.apple.product-type.application"]
    if len(targets) != 1:
        fail("Expected exactly one iosApp application target")
    configs = objects[targets[0]["buildConfigurationList"]]["buildConfigurations"]
    releases = [objects[key] for key in configs if objects[key]["name"] == "Release"]
    if len(releases) != 1:
        fail("Expected exactly one iosApp Release configuration")
    settings = releases[0]["buildSettings"]
    settings.update({
        "DEVELOPMENT_TEAM": team_id,
        "CODE_SIGN_STYLE": "Manual",
        "CODE_SIGN_IDENTITY": identity,
        "PROVISIONING_PROFILE_SPECIFIER": profile_name,
        "CURRENT_PROJECT_VERSION": build_number,
    })
    # Remove any legacy UUID override so it cannot conflict with the selected profile name.
    settings.pop("PROVISIONING_PROFILE", None)
    for key, additions in {
        "FRAMEWORK_SEARCH_PATHS": [
            "$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
        ],
        "OTHER_LDFLAGS": ["-framework", framework],
    }.items():
        existing = settings.get(key, ["$(inherited)"])
        if isinstance(existing, str):
            existing = [existing]
        settings[key] = existing + additions


def main():
    app = os.environ["RELEASE_APP"]
    from prepare_release import APPS
    if app not in APPS:
        fail("Unknown RELEASE_APP")
    root = pathlib.Path("app") / app
    config = (root / "iosApp/Configuration/Config.xcconfig").read_text()
    match = re.search(r"^PRODUCT_BUNDLE_IDENTIFIER\s*=\s*(\S+)\s*$", config, re.MULTILINE)
    if not match:
        fail("Cannot read PRODUCT_BUNDLE_IDENTIFIER from Config.xcconfig")
    team = re.search(r"^TEAM_ID[ \t]*=[ \t]*(.*)$", config, re.MULTILINE)
    bundle_id = match[1].replace("$(TEAM_ID)", team[1].strip() if team else "")
    if "$" in bundle_id:
        fail("Unresolved bundle ID variable")
    team_id = os.environ["APPLE_TEAM_ID"]
    with open(sys.argv[1], "rb") as source:
        profile = plistlib.load(source)
    entitlements = profile.get("Entitlements", {})
    if team_id not in profile.get("TeamIdentifier", []):
        fail("Provisioning profile does not belong to APPLE_TEAM_ID")
    prefixes = profile.get("ApplicationIdentifierPrefix", [])
    if entitlements.get("application-identifier") not in [f"{prefix}.{bundle_id}" for prefix in prefixes]:
        fail("Provisioning profile must match the explicit app bundle ID")
    if (entitlements.get("get-task-allow", False) or "ProvisionedDevices" in profile
            or profile.get("ProvisionsAllDevices", False) or not entitlements.get("beta-reports-active", False)):
        fail("Use an App Store Connect distribution provisioning profile")
    if profile["ExpirationDate"] <= datetime.datetime.now(datetime.timezone.utc).replace(tzinfo=None):
        fail("Provisioning profile has expired")
    uuid.UUID(profile["UUID"])
    profile_uuid = profile["UUID"]
    if not isinstance(profile.get("Name"), str) or not profile["Name"].strip():
        fail("Provisioning profile must have a name")
    with open(sys.argv[2], "wb") as target:
        plistlib.dump({
            "method": "app-store-connect",
            "destination": "export",
            "signingStyle": "manual",
            "teamID": team_id,
            "signingCertificate": "Apple Distribution",
            "provisioningProfiles": {bundle_id: profile_uuid},
            "manageAppVersionAndBuildNumber": False,
            "uploadSymbols": True,
        }, target)
    project_path = pathlib.Path(sys.argv[3])
    project = json.loads(subprocess.check_output([
        "plutil", "-convert", "json", "-o", "-", str(project_path),
    ]))
    identities = subprocess.check_output([
        "security", "find-identity", "-v", "-p", "codesigning", sys.argv[5],
    ], text=True)
    identity = signing_identity(profile, identities)
    framework = re.search(r'baseName\s*=\s*"([^"\n]+)"', (root / "shared/build.gradle.kts").read_text())
    if not framework:
        fail("Cannot read shared framework name")
    configure_project(project, team_id, profile["Name"], sys.argv[4], identity, framework[1])
    # Xcode accepts XML property lists; only the ephemeral CI checkout is rewritten.
    project_path.write_bytes(plistlib.dumps(project))
    print(profile_uuid)


if __name__ == "__main__":
    main()
