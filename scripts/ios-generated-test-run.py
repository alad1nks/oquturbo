"""Bind locale invocations to the original, unmodified current-build test specification."""
import hashlib
import json
import plistlib
import time
from pathlib import Path

PACKAGE = "com.alad1nks.oquturbo.OquTurbo"
METHODS = {"testLocalizedPendingAndLargeTextPicker", "testSystemRegionTransition",
           "testSystem24HourAcceptance", "testSystemRegionRestored"}


class GeneratedTestRun:
    def __init__(self, products, identity, source, evidence, cutoff):
        self.products = Path(products).absolute()
        self.cutoff = cutoff
        if identity.get("source") != source or len(source) != 40:
            raise ValueError("Current source differs from built identity")
        self.identity = identity
        self.state = self.inspect()
        if self.state["payloads"]["Debug-iphonesimulator/OquTurbo.app/OquTurbo"] != identity["sha256"]:
            raise ValueError("Built app identity changed")
        evidence.mkdir(exist_ok=False)
        (evidence / "generated.xctestrun").write_bytes(self.path.read_bytes())
        (evidence / "binding.json").write_text(json.dumps({"source": source, "originalPath": str(self.path),
            "executionPath": "originalPath (never archived copy)", **self.state}, indent=2))

    def budget(self):
        if time.monotonic() >= self.cutoff:
            raise TimeoutError("Single locale phase deadline exhausted during build binding")

    def digest(self, path):
        value = hashlib.sha256()
        with path.open("rb") as stream:
            while chunk := stream.read(1024 * 1024):
                self.budget()
                value.update(chunk)
        return value.hexdigest()

    def safe(self, path):
        path = Path(path)
        if not path.is_relative_to(self.products) or path.resolve() != path or not path.exists():
            raise ValueError("Product path escapes current build or is missing/symlinked")
        return path

    def inspect(self):
        self.budget()
        self.safe(self.products)
        files = sorted(self.products.glob("*.xctestrun"))
        if len(files) != 1:
            raise ValueError("Expected exactly one generated test specification")
        self.path = self.safe(files[0])
        document = plistlib.loads(self.path.read_bytes())
        if set(document) != {"OquTurboUITests", "__xctestrun_metadata__"}:
            raise ValueError("Unexpected test target set")
        metadata = document["__xctestrun_metadata__"]
        if metadata != {"FormatVersion": 1, "ContainerInfo": {"ContainerName": "iosApp", "SchemeName": "OquTurboRuntime"}}:
            raise ValueError("Unsupported generated format/container/scheme")
        target = document["OquTurboUITests"]
        required = {"BlueprintName": "OquTurboUITests", "BlueprintProviderName": "iosApp",
                    "BlueprintProviderRelativePath": "iosApp.xcodeproj", "ProductModuleName": "OquTurboUITests",
                    "IsUITestBundle": True, "IsXCTRunnerHostedTestBundle": True,
                    "TestHostBundleIdentifier": PACKAGE + ".UITests.xctrunner",
                    "DiagnosticCollectionPolicy": 1, "TestLanguage": "", "TestRegion": "",
                    "CommandLineArguments": [], "UITargetAppCommandLineArguments": []}
        if any(target.get(key) != value for key, value in required.items()):
            raise ValueError("Unsupported generated configuration or diagnostic/locale override")
        app = "__TESTROOT__/Debug-iphonesimulator/OquTurbo.app"
        host = "__TESTROOT__/Debug-iphonesimulator/OquTurboUITests-Runner.app"
        bundle = host + "/PlugIns/OquTurboUITests.xctest"
        if (target.get("UITargetAppPath") != app or target.get("TestHostPath") != host or
                target.get("TestBundlePath") != "__TESTHOST__/PlugIns/OquTurboUITests.xctest" or
                target.get("DependentProductPaths") != [app, host, bundle]):
            raise ValueError("Unexpected app/host/test-bundle product path")
        environments = [target.get(key) for key in ("EnvironmentVariables", "TestingEnvironmentVariables", "UITargetAppEnvironmentVariables")]
        if any(not isinstance(env, dict) or any(any(word in key.lower() for word in ("locale", "language", "region")) for key in env) for env in environments):
            raise ValueError("Unknown generated environment/locale override")
        if environments[0].get("OS_ACTIVITY_DT_MODE") != "YES" or environments[0].get("PERFC_ENABLE_EXTENDED_DIAGNOSTIC_FORMAT") != "1":
            raise ValueError("Missing full activity diagnostics environment")
        payloads = {}
        for relative, identifier, executable in (
                ("Debug-iphonesimulator/OquTurbo.app", PACKAGE, "OquTurbo"),
                ("Debug-iphonesimulator/OquTurboUITests-Runner.app", PACKAGE + ".UITests.xctrunner", "OquTurboUITests-Runner"),
                ("Debug-iphonesimulator/OquTurboUITests-Runner.app/PlugIns/OquTurboUITests.xctest", PACKAGE + ".UITests", "OquTurboUITests")):
            root = self.safe(self.products / relative)
            info = plistlib.loads(self.safe(root / "Info.plist").read_bytes())
            if info.get("CFBundleIdentifier") != identifier or info.get("CFBundleExecutable") != executable:
                raise ValueError("Unexpected product bundle identity")
            self.safe(root / executable)
            for path in sorted(root.rglob("*")):
                self.budget()
                self.safe(path)
                if path.is_file():
                    key = str(path.relative_to(self.products))
                    if key not in payloads:
                        payloads[key] = self.digest(path)
        # The app's actual Debug payload must be bound as well as its launcher stub.
        self.safe(self.products / "Debug-iphonesimulator/OquTurbo.app/OquTurbo.debug.dylib")
        return {"specification": self.digest(self.path), "payloads": payloads}

    def command(self, udid, method, result):
        self.budget()
        previous_path = self.path
        if self.inspect() != self.state or self.path != previous_path:
            raise ValueError("Generated specification or current payload changed")
        if method not in METHODS or Path(result).exists():
            raise ValueError("Unexpected locale method or reused result path")
        return ["xcodebuild", "test-without-building", "-xctestrun", str(self.path),
                "-destination", "platform=iOS Simulator,id=" + udid, "-parallel-testing-enabled", "NO",
                "-maximum-concurrent-test-simulator-destinations", "1", "CODE_SIGNING_ALLOWED=NO",
                "-only-testing:OquTurboUITests/LocalReminderRuntimeTests/" + method, "-resultBundlePath", str(result)]
