# Custom mainline kernel recipe for the MangoPi MQ Pro (Allwinner D1, RISC-V).
#
# The Allwinner D1 (sun20i) support is present in the mainline Linux kernel
# since v6.2. The meta-riscv layer's default kernel recipe
# (linux-allwinnerd1-dev) points to the now-removed smaeul/linux fork, so we
# provide our own recipe that builds the mainline stable kernel instead.
#
# The machine uses KERNEL_IMAGETYPE = "fitImage" (see riscv-common.inc), so the
# kernel is packaged as a FIT image together with the device tree.

SUMMARY = "Mainline Linux kernel for the MangoPi MQ Pro (Allwinner D1)"
DESCRIPTION = "Mainline stable Linux kernel configured for the MangoPi MQ Pro board"
SECTION = "kernel"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

inherit kernel

S = "${WORKDIR}/git"

# Mainline stable kernel branch. Allwinner D1 support is present since 6.2;
# 6.6 is an LTS release with mature D1 support.
LINUX_VERSION = "6.6"
BRANCH = "linux-6.6.y"
SRCREV = "${AUTOREV}"

SRC_URI = " \
    git://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git;protocol=https;branch=${BRANCH} \
"

PV = "${LINUX_VERSION}+git${SRCPV}"

# The allwinner_defconfig enables support for the Allwinner D1 (sun20i) SoC
# and produces the sun20i-d1-mangopi-mq-pro device tree.
KBUILD_DEFCONFIG = "allwinner_defconfig"

# Build the device tree for the MangoPi MQ Pro board.
KERNEL_DEVICETREE = "allwinner/sun20i-d1-mangopi-mq-pro.dtb"

# Ensure the FIT image is produced.
KERNEL_IMAGETYPE = "fitImage"

COMPATIBLE_MACHINE = "mangopi-mq-pro"

# The kernel is built with the RISC-V 64-bit toolchain.
COMPATIBLE_HOST = "riscv64.*-linux"