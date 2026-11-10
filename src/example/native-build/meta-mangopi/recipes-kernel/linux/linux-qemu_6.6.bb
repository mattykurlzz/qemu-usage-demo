# Mainline kernel recipe for the QEMU RISC-V 64-bit 'virt' machine.
#
# This is a barebone adaptation of the linux-mangopi recipe for QEMU. It builds
# the same mainline stable kernel 6.6 with the same RISC-V 64-bit toolchain, but
# configured for the generic QEMU 'virt' machine instead of the Allwinner D1.
#
# The QEMU 'virt' machine is a generic RISC-V platform, so we use the generic
# RISC-V defconfig and boot the kernel directly (Image). QEMU generates the
# device tree for the 'virt' machine at runtime, so no device tree is bundled.

SUMMARY = "Mainline Linux kernel for the QEMU RISC-V 64-bit virt machine"
DESCRIPTION = "Mainline stable Linux kernel configured for the QEMU RISC-V 64-bit virt machine"
SECTION = "kernel"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

inherit kernel

S = "${WORKDIR}/git"

# Same mainline stable kernel branch as the MangoPi build.
LINUX_VERSION = "6.6"
BRANCH = "linux-6.6.y"
SRCREV = "${AUTOREV}"

SRC_URI = " \
    git://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git;protocol=https;branch=${BRANCH} \
"

PV = "${LINUX_VERSION}+git${SRCPV}"

# The generic RISC-V defconfig enables support for the QEMU 'virt' machine.
KBUILD_DEFCONFIG = "defconfig"

# Direct kernel boot under QEMU: produce a plain Image, not a FIT image.
KERNEL_IMAGETYPE = "Image"

COMPATIBLE_MACHINE = "qemu-riscv64"

# The kernel is built with the RISC-V 64-bit toolchain (same as mangopi-mq-pro).
COMPATIBLE_HOST = "riscv64.*-linux"