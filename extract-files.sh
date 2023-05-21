#!/bin/bash
#
# Copyright (C) 2016 The CyanogenMod Project
# Copyright (C) 2017-2022 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# If we're being sourced by the common script that we called,
# stop right here. No need to go down the rabbit hole.
if [ "${BASH_SOURCE[0]}" != "${0}" ]; then
    return
fi

set -e

export DEVICE=dubai
export DEVICE_COMMON=sm7325-common
export VENDOR=motorola

function blob_fixup() {
    case "${1}" in
        vendor/lib/libmot_chi_desktop_helper.so)
            grep -q "libgui_shim.so" "${2}" || ${PATCHELF} --add-needed "libgui_shim.so" "${2}"
            ;;
    esac
}

"./../../${VENDOR}/${DEVICE_COMMON}/extract-files.sh" "$@"
