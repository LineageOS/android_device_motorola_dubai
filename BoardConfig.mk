#
# Copyright (C) 2022 The LineageOS Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

DEVICE_PATH := device/motorola/pstar

# Inherit from motorola sm8250-common
-include device/motorola/sm8250-common/BoardConfigCommon.mk

# Bluetooth
BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := $(DEVICE_PATH)/bluetooth

# Kernel
TARGET_KERNEL_ADDITIONAL_CONFIG := vendor/lineage_pstar_defconfig

# Partitions
BOARD_USERDATAIMAGE_PARTITION_SIZE := 226517168128
ifneq ($(WITH_GMS),true)
BOARD_PRODUCTIMAGE_PARTITION_RESERVED_SIZE := 3033788416
BOARD_SYSTEMIMAGE_PARTITION_RESERVED_SIZE := 898367488
BOARD_SYSTEM_EXTIMAGE_PARTITION_RESERVED_SIZE := 724594688
endif
BOARD_MOT_DP_GROUP_SIZE := 7109345280
BOARD_SUPER_PARTITION_SIZE := 14227079168

# Power
TARGET_TAP_TO_WAKE_NODE := "/sys/class/sensors/dt-gesture/enable"

# Properties
TARGET_VENDOR_PROP += $(DEVICE_PATH)/vendor.prop

# Recovery
TARGET_RECOVERY_UI_MARGIN_HEIGHT := 90

# inherit from the proprietary version
-include vendor/motorola/pstar/BoardConfigVendor.mk
