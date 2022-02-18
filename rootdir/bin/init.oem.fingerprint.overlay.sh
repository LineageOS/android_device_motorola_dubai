#!/vendor/bin/sh
#
# Identify fingerprint sensor model
#
# Copyright (c) 2019 Lenovo
# All rights reserved.
#
# Changed Log:
# ---------------------------------
# April 15, 2019  chengql2@lenovo.com  Initial version
# April 28, 2019  chengql2  Add fps_id creating step
# December 2, 2019  chengql2  Store fps_id into persist fs, and identify sensor
#                             again when secure unit boots as factory mode.

script_name=${0##*/}
script_name=${script_name%.*}
function notice {
    log -t "$scriptname" -p i "$*"
}

persist_fps_id=/mnt/vendor/persist/fps/vendor_id

FPS_VENDOR_GOODIX=goodix
FPS_VENDOR_FPC=fpc
FPS_VENDOR_NONE=none

PROP_FPS_IDENT=vendor.hw.fps.ident
MAX_TIMES=20

function ident_fps {
    log "- install FPC driver"
    insmod /vendor/lib/modules/fpc1020_mmi.ko
    sleep 1
    notice "- identify FPC sensor"
    setprop $PROP_FPS_IDENT ""
    start fpc_ident
    for i in $(seq 1 $MAX_TIMES)
    do
        sleep 0.1
        ident_status=$(getprop $PROP_FPS_IDENT)
        notice "-result : $ident_status"
        if [ $ident_status == $FPS_VENDOR_FPC ]; then
            log "ok"
            echo $FPS_VENDOR_FPC > $persist_fps_id
            return 0
        elif [ $ident_status == $FPS_VENDOR_NONE ]; then
            notice "fail"
            notice "- unload FPC driver"
            rmmod fpc1020_mmi
            break
        fi
    done

    notice "- install Goodix driver"
    insmod /vendor/lib/modules/goodix_fod_mmi.ko
    echo $FPS_VENDOR_GOODIX > $persist_fps_id
    return 0
}

if [ ! -f $persist_fps_id ]; then
    ident_fps
    return $?
fi

fps_vendor=$(cat $persist_fps_id)
if [ -z $fps_vendor ]; then
    fps_vendor=$FPS_VENDOR_NONE
fi
notice "FPS vendor: $fps_vendor"

if [ $fps_vendor == $FPS_VENDOR_GOODIX ]; then
    notice "- install Goodix driver"
    insmod /vendor/lib/modules/goodix_fod_mmi.ko
    return $?
fi

if [ $fps_vendor == $FPS_VENDOR_FPC ]; then
    notice "- install FPC driver"
    insmod /vendor/lib/modules/fpc1020_mmi.ko
    return $?
fi

ident_fps
return $?
