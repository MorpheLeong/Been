#!/usr/bin/env bash
set -euo pipefail

mode="${1:---all}"
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
python_bin="${PYTHON_BIN:-}"
idf_command=(idf.py)

if [[ -z "${python_bin}" ]]; then
    if command -v python3 >/dev/null 2>&1; then
        python_bin="python3"
    elif command -v python >/dev/null 2>&1; then
        python_bin="python"
    else
        echo "ERROR: Python 3 is not available; activate the project environment first." >&2
        exit 1
    fi
fi

if [[ -n "${AI_PASSPORT_IDF_PYTHON:-}" && -n "${IDF_PATH:-}" ]]; then
    idf_command=("${AI_PASSPORT_IDF_PYTHON}" "${IDF_PATH}/tools/idf.py")
fi

usage() {
    echo "Usage: $0 [--all|--static|--firmware]" >&2
}

run_static_checks() {
    local actionlint_bin
    local executable_suffix
    local test_dir

    executable_suffix=""
    case "$(uname -s)" in
        MINGW*_NT-*|MSYS_NT-*) executable_suffix=".exe" ;;
    esac

    "${python_bin}" tools/check_repo.py
    "${python_bin}" tools/check_passport_fonts.py

    actionlint_bin="${ACTIONLINT_BIN:-}"
    if [[ -z "${actionlint_bin}" ]]; then
        actionlint_bin="$(command -v actionlint || true)"
    fi
    if [[ -z "${actionlint_bin}" || ! -x "${actionlint_bin}" ]]; then
        actionlint_bin="$(./tools/install-actionlint.sh)"
    fi
    "${actionlint_bin}" -color .github/workflows/*.yml

    test_dir="$(mktemp -d /tmp/ai-passport-host-tests.XXXXXX)"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Imain \
        tests/test_ui_pixel_math.c main/ui_pixel_math.c \
        -o "${test_dir}/test_ui_pixel_math${executable_suffix}"
    "${test_dir}/test_ui_pixel_math${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Imain \
        tests/test_demo_navigation.c main/demo_navigation.c \
        -o "${test_dir}/test_demo_navigation${executable_suffix}"
    "${test_dir}/test_demo_navigation${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Imain \
        tests/test_passport_app_model.c main/passport_app_model.c \
        -o "${test_dir}/test_passport_app_model${executable_suffix}"
    "${test_dir}/test_passport_app_model${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Imain \
        tests/test_passport_display_policy.c main/passport_display_policy.c \
        -o "${test_dir}/test_passport_display_policy${executable_suffix}"
    "${test_dir}/test_passport_display_policy${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Imain \
        tests/test_passport_location_model.c main/passport_location_model.c \
        -o "${test_dir}/test_passport_location_model${executable_suffix}"
    "${test_dir}/test_passport_location_model${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Imain \
        tests/test_passport_location_protocol.c main/passport_location_protocol.c \
        -o "${test_dir}/test_passport_location_protocol${executable_suffix}"
    "${test_dir}/test_passport_location_protocol${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Imain \
        tests/test_passport_trip_model.c main/passport_trip_model.c \
        -o "${test_dir}/test_passport_trip_model${executable_suffix}"
    "${test_dir}/test_passport_trip_model${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Imain \
        tests/test_passport_avatar_protocol.c main/passport_avatar_protocol.c \
        -o "${test_dir}/test_passport_avatar_protocol${executable_suffix}"
    "${test_dir}/test_passport_avatar_protocol${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Imain \
        tests/test_passport_ble_ids.c \
        -o "${test_dir}/test_passport_ble_ids${executable_suffix}"
    "${test_dir}/test_passport_ble_ids${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Icomponents/bsp/src \
        tests/test_bsp_display_rounding.c components/bsp/src/bsp_display_rounding.c \
        -o "${test_dir}/test_bsp_display_rounding${executable_suffix}"
    "${test_dir}/test_bsp_display_rounding${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Icomponents/bsp/src \
        tests/test_bsp_es8311_sleep_check.c components/bsp/src/bsp_es8311_sleep_check.c \
        -o "${test_dir}/test_bsp_es8311_sleep_check${executable_suffix}"
    "${test_dir}/test_bsp_es8311_sleep_check${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror \
        -Itests/bsp_stubs -Icomponents/bsp/include \
        tests/test_bsp_button.c -o "${test_dir}/test_bsp_button${executable_suffix}"
    "${test_dir}/test_bsp_button${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror \
        -Itests/bsp_stubs -Icomponents/bsp/include \
        tests/test_bsp_lvgl_init.c components/bsp/src/bsp_display_rounding.c \
        -o "${test_dir}/test_bsp_lvgl_init${executable_suffix}"
    "${test_dir}/test_bsp_lvgl_init${executable_suffix}"
    "${CC:-cc}" -std=c11 -Wall -Wextra -Werror \
        -Itests/audio_stubs -Icomponents/bsp/include -Icomponents/bsp/src \
        tests/test_bsp_audio_recovery.c components/bsp/src/bsp_es8311_sleep_check.c \
        -o "${test_dir}/test_bsp_audio_recovery${executable_suffix}"
    "${test_dir}/test_bsp_audio_recovery${executable_suffix}"
    for demo in audio low_power ble wifi; do
        "${CC:-cc}" -std=c11 -Wall -Wextra -Werror \
            -ffunction-sections -fdata-sections -Itests/demo_stubs -Imain \
            "tests/test_demo_${demo}_runtime.c" -Wl,--gc-sections \
            -o "${test_dir}/test_demo_${demo}_runtime${executable_suffix}"
        "${test_dir}/test_demo_${demo}_runtime${executable_suffix}"
    done
    PYTHONDONTWRITEBYTECODE=1 "${python_bin}" tests/test_deep_sleep_contract.py
    PYTHONDONTWRITEBYTECODE=1 "${python_bin}" tests/test_passport_ble_tx_contract.py
    PYTHONDONTWRITEBYTECODE=1 "${python_bin}" tests/test_check_repo.py
    PYTHONDONTWRITEBYTECODE=1 "${python_bin}" tests/test_verify_firmware.py
    PYTHONDONTWRITEBYTECODE=1 "${python_bin}" tests/test_archive_firmware.py
    PYTHONDONTWRITEBYTECODE=1 "${python_bin}" tests/test_install_passport_skills.py
    rm -rf "${test_dir}"
    echo "Host tests: PASS"
}

run_firmware_checks() (
    local validation_build_dir

    if ! command -v idf.py >/dev/null 2>&1; then
        echo "ERROR: idf.py is not available; activate ESP-IDF 5.5.3 first." >&2
        return 1
    fi

    validation_build_dir="$(mktemp -d /tmp/ai-passport-firmware.XXXXXX)"
    trap 'case "${validation_build_dir}" in /tmp/ai-passport-firmware.*) rm -rf -- "${validation_build_dir}" ;; esac' EXIT

    SDKCONFIG_DEFAULTS="${repo_root}/sdkconfig.defaults" \
        "${idf_command[@]}" -B "${validation_build_dir}" \
        -D "SDKCONFIG=${validation_build_dir}/sdkconfig" build
    "${idf_command[@]}" -B "${validation_build_dir}" merge-bin \
        -o "${validation_build_dir}/FoloToy-AI-Passport-full.bin"
    "${python_bin}" tools/verify_firmware.py "${validation_build_dir}"
    PYTHONDONTWRITEBYTECODE=1 "${python_bin}" tools/archive_firmware.py create \
        "${validation_build_dir}" --archive-root "${repo_root}/build/firmware"
    mkdir -p "${repo_root}/build"
    install -m 0644 \
        "${validation_build_dir}/FoloToy-AI-Passport-full.bin" \
        "${repo_root}/build/FoloToy-AI-Passport-full.bin"
    echo "Firmware build: PASS"
)

cd "${repo_root}"
case "${mode}" in
    --all)
        run_static_checks
        run_firmware_checks
        ;;
    --static)
        run_static_checks
        ;;
    --firmware)
        run_firmware_checks
        ;;
    *)
        usage
        exit 2
        ;;
esac
