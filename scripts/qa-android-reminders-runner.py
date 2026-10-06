#!/usr/bin/env python3
"""Own one fresh accelerated AVD. No software fallback, no runtime PASS without harness success."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import time


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    sdk = Path(os.environ['ANDROID_HOME'])
    avd_home = Path(os.environ['RUNNER_TEMP']) / 'oquturbo-reminders-avd'
    avd_home.mkdir(exist_ok=False)
    env = dict(os.environ, ANDROID_AVD_HOME=str(avd_home))
    serial = 'emulator-5580'
    process = None
    log = args.output.joinpath('emulator.log').open('wb')
    def adb(*argv, timeout=40):
        return subprocess.check_output(['adb', '-s', serial, *argv], timeout=timeout).decode(errors='replace').strip()
    try:
        assert os.access('/dev/kvm', os.R_OK | os.W_OK), 'Hardware acceleration unavailable'
        assert serial not in subprocess.check_output(['adb', 'devices']).decode(), 'Owned serial is already in use'
        subprocess.run([str(sdk / 'cmdline-tools/latest/bin/avdmanager'), 'create', 'avd', '-n', 'oquturbo_reminders',
                        '-k', 'system-images;android-33;default;x86_64', '--device', 'pixel_2'],
                       input=b'no\n', check=True, env=env)
        subprocess.run([str(sdk / 'emulator/emulator'), '-accel-check'], check=True, env=env,
                       stdout=log, stderr=subprocess.STDOUT)
        process = subprocess.Popen([str(sdk / 'emulator/emulator'), '-avd', 'oquturbo_reminders', '-port', '5580',
                                    '-accel', 'on', '-no-window', '-no-audio', '-no-snapshot', '-no-boot-anim',
                                    '-gpu', 'swiftshader_indirect', '-memory', '2048'],
                                   env=env, stdout=log, stderr=subprocess.STDOUT)
        start = time.monotonic()
        while time.monotonic() - start < 600:
            if process.poll() is not None: raise RuntimeError('Accelerated emulator exited during boot')
            try:
                if adb('shell', 'getprop', 'sys.boot_completed') == '1': break
            except (subprocess.SubprocessError, OSError): pass
            print('Waiting for owned accelerated Android boot', flush=True)
            time.sleep(15)
        else: raise TimeoutError('Android boot exceeded ten minutes')
        adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
        adb('shell', 'wm', 'dismiss-keyguard')
        # Bounded pre-app settling; ANR/crash is an environment failure, never a passed app check.
        for _ in range(4):
            time.sleep(15)
            logcat = adb('logcat', '-d', '-v', 'threadtime')
            args.output.joinpath('pre-app-logcat.txt').write_text(logcat)
            if 'ANR in ' in logcat or 'FATAL EXCEPTION' in logcat:
                raise RuntimeError('Pre-app Android instability; app acceptance NOT RUN')
        args.output.joinpath('runtime.json').write_text(json.dumps({
            'pid': process.pid, 'serial': serial, 'sdk': adb('shell', 'getprop', 'ro.build.version.sdk'),
            'fingerprint': adb('shell', 'getprop', 'ro.build.fingerprint'), 'acceleration_required': True,
            'boot_seconds': time.monotonic() - start, 'source_sha': os.environ.get('GITHUB_SHA'),
        }, indent=2))
        subprocess.run([sys.executable, 'scripts/qa-android-reminders.py', '--serial', serial,
                        '--apk', str(args.apk), '--output', str(args.output)], check=True, timeout=306 * 60)
    finally:
        if process is not None:
            try:
                args.output.joinpath('shutdown-logcat.txt').write_text(adb('logcat', '-d', '-v', 'threadtime'))
                adb('emu', 'kill')
            except (subprocess.SubprocessError, OSError): pass
            try: process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                process.terminate()
                try: process.wait(timeout=10)
                except subprocess.TimeoutExpired: process.kill(); process.wait()
        log.close()


if __name__ == '__main__':
    main()
