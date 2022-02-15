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

# Inherit from motorola sm8250-common
$(call inherit-product, device/motorola/sm8250-common/BoardConfigCommon.mk)

# Partitions
BOARD_USERDATAIMAGE_PARTITION_SIZE := 226517168128
ifneq ($(WITH_GMS),true)
BOARD_PRODUCTIMAGE_PARTITION_RESERVED_SIZE := 3548788416
BOARD_SYSTEMIMAGE_PARTITION_RESERVED_SIZE := 988367488
BOARD_SYSTEM_EXTIMAGE_PARTITION_RESERVED_SIZE := 1554594688
BOARD_VENDORIMAGE_PARTITION_RESERVED_SIZE := 993096064
endif
BOARD_MOT_DP_SIZE := 7109345280
BOARD_SUPER_PARTITION_SIZE := 14227079168


# inherit from the proprietary version
$(call inherit-product,  vendor/motorola/pstar/BoardConfigVendor.mk)
