#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE_DIR="${OQUTURBO_QA_ANDROID_CACHE:-$ROOT_DIR/.qa}"
SDK_DIR="${ANDROID_SDK_ROOT:-$CACHE_DIR/android-sdk}"
ANDROID_USER_HOME="${ANDROID_USER_HOME:-$CACHE_DIR/android-user}"
ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$ANDROID_USER_HOME/avd}"
CLI_BIN="${OQUTURBO_ANDROID_CLI:-$CACHE_DIR/android-cli/android}"
CLI_URL="https://dl.google.com/android/cli/latest/linux_x86_64/android"
AVD_NAME="${OQUTURBO_QA_AVD:-medium_phone}"
RUNTIME_DIR="$CACHE_DIR/emulator-runtime"
ARTIFACT_DIR="$ROOT_DIR/build/qa/android"
EMULATOR_LOG="$ARTIFACT_DIR/emulator.log"

export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
export ANDROID_USER_HOME
export ANDROID_AVD_HOME

usage() {
    echo "Usage: $0 <provision|start|status|build|install|capture|adb|logs|stop> [arguments]"
    echo "  provision                  Download Android CLI/SDK and create the QA AVD"
    echo "  start                      Start the AVD headlessly in a long-running session"
    echo "  status                     Print device and boot status"
    echo "  build [product]            Assemble a debug APK (default: oquturbo)"
    echo "  install [product]          Install and launch a built debug APK"
    echo "  capture [name]             Save PNG, annotated PNG, and layout JSON"
    echo "  adb <arguments...>         Run the cached adb against the emulator"
    echo "  logs                       Print the emulator log"
    echo "  stop                       Stop the running emulator"
}

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Required command is unavailable: $1" >&2
        exit 1
    fi
}

ensure_cli() {
    if [[ -x "$CLI_BIN" ]]; then
        return
    fi
    require_command curl
    mkdir -p "$(dirname "$CLI_BIN")"
    curl -fsSL "$CLI_URL" -o "$CLI_BIN"
    chmod +x "$CLI_BIN"
}

android_cli() {
    ensure_cli
    "$CLI_BIN" --no-metrics --sdk="$SDK_DIR" "$@"
}

adb_bin() {
    local executable="$SDK_DIR/platform-tools/adb"
    if [[ ! -x "$executable" ]]; then
        echo "adb is unavailable. Run '$0 provision' first." >&2
        exit 1
    fi
    echo "$executable"
}

emulator_bin() {
    local executable="$SDK_DIR/emulator/emulator"
    if [[ ! -x "$executable" ]]; then
        echo "Android Emulator is unavailable. Run '$0 provision' first." >&2
        exit 1
    fi
    echo "$executable"
}

runtime_library_path() {
    echo "$RUNTIME_DIR/usr/lib/x86_64-linux-gnu:$SDK_DIR/emulator/lib64:$SDK_DIR/emulator/lib64/qt/lib"
}

provision_runtime_libraries() {
    local emulator
    emulator="$(emulator_bin)"
    if [[ -e "$RUNTIME_DIR/usr/lib/x86_64-linux-gnu/libX11.so.6" ]]; then
        return
    fi
    if ! ldd "$emulator" 2>/dev/null | grep -q 'libX11.so.6 => not found'; then
        return
    fi
    if ! command -v apt-get >/dev/null 2>&1 || ! command -v dpkg-deb >/dev/null 2>&1; then
        echo "Android Emulator requires libX11.so.6; install the host X11 runtime libraries." >&2
        exit 1
    fi

    local download_dir
    download_dir="$(mktemp -d "$CACHE_DIR/emulator-debs.XXXXXX")"
    (
        cd "$download_dir"
        apt-get download libx11-6 libxcb1 libxau6 libxdmcp6 libbsd0 libmd0
    )
    mkdir -p "$RUNTIME_DIR"
    local package_file
    for package_file in "$download_dir"/*.deb; do
        dpkg-deb -x "$package_file" "$RUNTIME_DIR"
    done
}

provision() {
    ensure_cli
    mkdir -p "$SDK_DIR" "$ANDROID_AVD_HOME" "$ARTIFACT_DIR"
    if [[ ! -f "$ANDROID_AVD_HOME/$AVD_NAME.ini" ]]; then
        android_cli emulator create "$AVD_NAME"
    fi
    provision_runtime_libraries
    echo "Android QA environment is ready: $AVD_NAME"
}

start_emulator() {
    provision
    local adb emulator serial accel_mode
    adb="$(adb_bin)"
    emulator="$(emulator_bin)"
    serial="$($adb devices | awk '/^emulator-[0-9]+[[:space:]]+(device|offline)/ { print $1; exit }')"
    if [[ -n "$serial" ]]; then
        echo "Android Emulator is already running: $serial"
        return
    fi

    accel_mode="auto"
    if [[ ! -r /dev/kvm ]]; then
        accel_mode="off"
        echo "KVM is unavailable; starting the slower software emulator."
    fi
    mkdir -p "$ARTIFACT_DIR" "$CACHE_DIR"
    echo "Starting Android Emulator. Keep this command session running during QA."
    exec env LD_LIBRARY_PATH="$(runtime_library_path)" \
        "$emulator" "@$AVD_NAME" \
        -no-window -no-audio -no-boot-anim -gpu software -accel "$accel_mode" \
        -no-snapshot-load -no-snapshot-save 2>&1 | tee "$EMULATOR_LOG"
}

status() {
    local adb serial boot_completed
    adb="$(adb_bin)"
    "$adb" devices -l
    serial="$($adb devices | awk '/^emulator-[0-9]+[[:space:]]+device/ { print $1; exit }')"
    if [[ -z "$serial" ]]; then
        echo "boot_completed=0"
        return
    fi
    boot_completed="$($adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    echo "boot_completed=${boot_completed:-0}"
}

product_config() {
    local product="${1:-oquturbo}"
    case "$product" in
        oquturbo|sansprint|kenkoz|baspa|wordflow) ;;
        *)
            echo "Unsupported product: $product" >&2
            exit 1
            ;;
    esac
    echo "$product com.alad1nks.$product"
}

configure_java() {
    if [[ -n "${OQUTURBO_QA_JAVA_HOME:-}" ]]; then
        export JAVA_HOME="$OQUTURBO_QA_JAVA_HOME"
    fi
    if [[ -x "${JAVA_HOME:-}/bin/java" ]]; then
        export PATH="$JAVA_HOME/bin:$PATH"
        return
    fi
    if command -v java >/dev/null 2>&1; then
        return
    fi

    local gradle_cache candidate
    gradle_cache="${GRADLE_USER_HOME:-${HOME:-}/.gradle}"
    candidate="$(find "$gradle_cache/jdks" -type f -path '*21*/bin/java' -perm -111 -print -quit 2>/dev/null || true)"
    if [[ -z "$candidate" ]]; then
        echo "JDK 21 is unavailable. Set OQUTURBO_QA_JAVA_HOME to a JDK 21 directory." >&2
        exit 1
    fi
    export JAVA_HOME="$(cd "$(dirname "$candidate")/.." && pwd)"
    export PATH="$JAVA_HOME/bin:$PATH"
}

build_apk() {
    local product package_name
    configure_java
    read -r product package_name <<<"$(product_config "${1:-oquturbo}")"
    "$ROOT_DIR/gradlew" ":app:$product:androidApp:assembleDebug"
}

debug_apk() {
    local product package_name apk
    read -r product package_name <<<"$(product_config "${1:-oquturbo}")"
    apk="$ROOT_DIR/app/$product/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
    if [[ ! -f "$apk" ]]; then
        echo "Debug APK is unavailable: $apk. Run '$0 build $product' first." >&2
        exit 1
    fi
    echo "$apk"
}

install_apk() {
    local product package_name apk serial adb installed_path process_id
    read -r product package_name <<<"$(product_config "${1:-oquturbo}")"
    apk="$(debug_apk "$product")"
    adb="$(adb_bin)"
    serial="$($adb devices | awk '/^emulator-[0-9]+[[:space:]]+device/ { print $1; exit }')"
    if [[ -z "$serial" ]]; then
        echo "No booted Android Emulator is available." >&2
        exit 1
    fi
    "$adb" -s "$serial" install -r -t "$apk"
    "$adb" -s "$serial" shell am start -W -n "$package_name/.MainActivity"
    installed_path="$($adb -s "$serial" shell pm path "$package_name" | tr -d '\r')"
    if [[ "$installed_path" != package:* ]]; then
        echo "APK post-condition failed: $package_name is not installed." >&2
        exit 1
    fi
    process_id="$($adb -s "$serial" shell pidof "$package_name" | tr -d '\r')"
    if [[ -z "$process_id" ]]; then
        echo "Launch post-condition failed: $package_name has no running process." >&2
        exit 1
    fi
    echo "Installed and launched $package_name (pid $process_id)."
}

capture() {
    local name="${1:-oquturbo}" serial output_dir
    if [[ ! "$name" =~ ^[a-zA-Z0-9._-]+$ ]]; then
        echo "Invalid artifact name: $name" >&2
        exit 1
    fi
    serial="$("$(adb_bin)" devices | awk '/^emulator-[0-9]+[[:space:]]+device/ { print $1; exit }')"
    if [[ -z "$serial" ]]; then
        echo "No booted Android Emulator is available." >&2
        exit 1
    fi
    output_dir="$ARTIFACT_DIR/$name"
    mkdir -p "$output_dir"
    rm -f "$output_dir/screen.png" "$output_dir/screen-annotated.png" "$output_dir/layout.json"
    android_cli screen capture --device="$serial" --output="$output_dir/screen.png"
    if [[ ! -s "$output_dir/screen.png" ]]; then
        echo "Screenshot post-condition failed." >&2
        exit 1
    fi
    android_cli screen capture --device="$serial" --annotate --output="$output_dir/screen-annotated.png"
    if [[ ! -s "$output_dir/screen-annotated.png" ]]; then
        echo "Annotated screenshot post-condition failed." >&2
        exit 1
    fi
    android_cli layout --device="$serial" --pretty --output="$output_dir/layout.json"
    if [[ ! -s "$output_dir/layout.json" ]]; then
        echo "Layout post-condition failed." >&2
        exit 1
    fi
    echo "Android QA artifacts: $output_dir"
}

stop_emulator() {
    local adb serial
    adb="$(adb_bin)"
    serial="$($adb devices | awk '/^emulator-[0-9]+[[:space:]]+(device|offline)/ { print $1; exit }')"
    if [[ -n "$serial" ]]; then
        "$adb" -s "$serial" emu kill
    fi
    echo "Android Emulator stopped."
}

command="${1:-}"
shift || true
case "$command" in
    provision) provision "$@" ;;
    start) start_emulator "$@" ;;
    status) status "$@" ;;
    build) build_apk "$@" ;;
    install) install_apk "$@" ;;
    capture) capture "$@" ;;
    adb) "$(adb_bin)" "$@" ;;
    logs) tail -n 200 "$EMULATOR_LOG" ;;
    stop) stop_emulator "$@" ;;
    *) usage; exit 1 ;;
esac
