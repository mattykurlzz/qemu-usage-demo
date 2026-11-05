# Override for u-boot-allwinnerd1 to avoid a deploy conflict with the kernel.
#
# The base u-boot.inc deploys the device tree binary when UBOOT_DTB is set
# (see u-boot.inc do_deploy). The nezha-common.inc machine include sets
# UBOOT_DTB = "1", so u-boot deploys sun20i-d1-mangopi-mq-pro.dtb to the same
# shared deploy directory as the linux-mangopi kernel recipe. BitBake treats
# this as a conflict ("trying to install files into a shared area when those
# files already exist").
#
# The kernel is the authoritative source for the device tree, so we unset
# UBOOT_DTB here to stop u-boot from deploying its own copy.
UBOOT_DTB = ""