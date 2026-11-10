# Repository setup

After cloning, make sure you update your submodules

``` bash
git submodule update --init
```

# Yocto build for MangoPi MQ-Pro (Allwinner D1, RISC-V 64-bit)

This directory contains a complete, working Yocto Project setup that builds a
non-graphical Linux image (`core-image-base`) for the **MangoPi MQ-Pro** board
(Allwinner D1 SoC, RISC-V 64-bit). It is used to demonstrate the benefits QEMU
brings to embedded developers: the same image can be booted under QEMU for
development and testing without touching real hardware.

The build runs inside a Docker container so that the host does not need any
Yocto prerequisites installed. All sources, the build directory, the download
cache and the sstate cache are kept on the host and shared with the container.

## What is built

The build produces a bootable image for the MangoPi MQ-Pro:

- **Kernel**: mainline Linux 6.6 (stable), built with `allwinner_defconfig`,
  packaged as a `fitImage` with the `sun20i-d1-mangopi-mq-pro.dtb` device tree.
- **U-Boot**: `u-boot-sunxi-with-spl.bin` (from the `smaeul/u-boot` `d1-wip`
  branch) plus a boot script and `uEnv.txt`.
- **OpenSBI**: `fw_dynamic.bin` (RISC-V supervisor binary interface firmware).
- **Root filesystem**: `core-image-base` (non-graphical), produced as an
  `ext4` image and a `wic` image.

Outputs are written to `build/tmp/deploy/images/mangopi-mq-pro/`.

## Project layout

```
native-build/
├── Dockerfile              # Yocto host prerequisites + non-root build user
├── docker-compose.yml      # Container: mounts project, downloads & sstate volumes
├── run-build.sh            # Helper to run bitbake commands in the container
├── README.md               # This file
├── poky/                   # Yocto base distribution (scarthgap 5.0.x) [submodule]
├── meta-riscv/             # RISC-V BSP layer (mangopi-mq-pro machine) [submodule]
├── meta-openembedded/      # Dependency layers (meta-oe, meta-networking, meta-python) [submodule]
├── meta-mangopi/           # Custom layer with fixes for this board
│   ├── conf/layer.conf
│   ├── conf/machine/qemu-riscv64.conf   # QEMU machine config (qemu-support)
│   ├── recipes-kernel/linux/linux-mangopi_6.6.bb
│   ├── recipes-kernel/linux/linux-qemu_6.6.bb   # QEMU kernel recipe (qemu-support)
│   └── recipes-bsp/u-boot/u-boot-allwinnerd1.bbappend
├── build/                  # MangoPi build directory (generated, not committed)
│   └── conf/               # local.conf, bblayers.conf
├── build-qemu/             # QEMU build directory (generated, not committed)
│   └── conf/               # local.conf, bblayers.conf
├── downloads/              # Source download cache (named volume)
└── sstate-cache/           # Shared-state cache (named volume)
```

## Prerequisites

- Docker with the Docker Compose plugin.
- Enough disk space (a full build uses several GB in `build/`, `downloads/`
  and `sstate-cache/`).

## Building

### 1. Build the Docker image

The Docker image installs all Yocto host prerequisites and creates a non-root
build user matching your host UID/GID (Yocto refuses to build as root).

```bash
docker compose build
```

### 2. Build the image

Use the helper script, which sources the Yocto build environment and runs the
command inside the container:

```bash
./run-build.sh bitbake core-image-base
```

Or run the container directly:

```bash
docker compose run --rm yocto bash -lc \
  'source /work/poky/oe-init-build-env /work/build && bitbake core-image-base'
```

The first build downloads and compiles the toolchain, kernel, U-Boot, OpenSBI
and the root filesystem, so it takes a while. Subsequent builds are fast thanks
to the persistent sstate cache.

### 3. Find the outputs

```bash
ls build/tmp/deploy/images/mangopi-mq-pro/
```

Key artifacts:

- `fitImage` - kernel + device tree FIT image
- `sun20i-d1-mangopi-mq-pro.dtb` - device tree
- `u-boot-sunxi-with-spl.bin` - U-Boot with SPL
- `fw_dynamic.bin` - OpenSBI
- `core-image-base-mangopi-mq-pro.rootfs.ext4` - root filesystem
- `core-image-base-mangopi-mq-pro.rootfs.wic.gz` - full SD-card image

## Custom layer: meta-mangopi

The `meta-mangopi` layer contains the fixes needed to build a working image for
this board with the mainline kernel. It has layer priority 7 (higher than
`meta-riscv` at 6).

### `linux-mangopi_6.6.bb`

The `meta-riscv` layer's default kernel recipe (`linux-allwinnerd1-dev`) fetches
from the `smaeul/linux` fork, which has been removed from GitHub. This recipe
instead builds the **mainline stable kernel 6.6** with `allwinner_defconfig`
and the MangoPi MQ-Pro device tree.

It also sets `S = "${WORKDIR}/git"`. The `kernel.bbclass` default
`S = "${STAGING_KERNEL_DIR}"` equals the shared kernel source directory, which
prevents `do_symlink_kernsrc` from moving the source into place and causes
`do_configure` to fail with "No rule to make target 'olddefconfig'".

The kernel is selected in `build/conf/local.conf`:

```
PREFERRED_PROVIDER_virtual/kernel = "linux-mangopi"
```

### `u-boot-allwinnerd1.bbappend`

The `nezha-common.inc` machine include sets `UBOOT_DTB = "1"`, which makes
U-Boot deploy its own copy of `sun20i-d1-mangopi-mq-pro.dtb` to the shared
deploy directory. This collides with the device tree deployed by the kernel
recipe ("trying to install files into a shared area when those files already
exist"). The kernel is the authoritative source for the device tree, so this
bbappend unsets `UBOOT_DTB` to stop U-Boot from deploying its own copy.

### Machine config note

The `meta-riscv` machine config for `mangopi-mq-pro` also pulls in the
`rtl8723ds` WiFi driver via `MACHINE_EXTRA_RRECOMMENDS`. That out-of-tree
driver does not compile against the mainline 6.6 kernel (the `ieee80211_ptr`
member was removed from `struct net_device` in kernel 6.4+). Because this is a
non-graphical base image that does not need WiFi, the `rtl8723ds` module and
the `wifi`/`bluetooth` machine features were removed from the machine config in
`meta-riscv/conf/machine/mangopi-mq-pro.conf`.

## Yocto build for QEMU (RISC-V 64-bit 'virt' machine)

The same RISC-V 64-bit toolchain and root filesystem can be built for the QEMU
`virt` machine, so the image can be booted and tested under emulation without
touching real hardware. This is the QEMU-specific part of the project: it lives
on the `qemu-support` branch and is layered on top of the MangoPi build.

### What is different for QEMU

The MangoPi build targets the Allwinner D1 SoC. The QEMU build targets the
generic RISC-V 64-bit `virt` machine, which is a much simpler platform:

- **Kernel**: the same mainline Linux 6.6, but built with the generic RISC-V
  `defconfig` instead of `allwinner_defconfig`, and booted directly as an
  `Image` (no FIT image, no bundled device tree: QEMU generates the DTB for the
  `virt` machine at runtime).
- **U-Boot / OpenSBI**: not needed for direct kernel boot. QEMU loads OpenSBI
  (`fw_jump`) which then loads the kernel `Image`.
- **Root filesystem**: the same `core-image-base`, produced as an `ext4` image
  and a `wic.qcow2` image for convenience.

### QEMU-specific files

Two files in the `meta-mangopi` layer are QEMU-specific:

- `meta-mangopi/conf/machine/qemu-riscv64.conf` - the machine config for the
  QEMU `virt` machine. It reuses `riscv-common.inc` (same toolchain as the
  MangoPi build), selects `linux-qemu` as the kernel provider, sets
  `KERNEL_IMAGETYPE = "Image"`, adjusts `IMAGE_FSTYPES` and `SERIAL_CONSOLES`,
  and enables the `qemuboot` class with the QEMU command-line options used by
  `runqemu`.
- `meta-mangopi/recipes-kernel/linux/linux-qemu_6.6.bb` - the kernel recipe for
  QEMU. It builds the same mainline stable kernel 6.6 with the generic RISC-V
  `defconfig` and produces a plain `Image`.

### Building for QEMU

Create a separate build directory (so the MangoPi and QEMU builds do not share
`TMPDIR`), source the Yocto environment, and set the machine:

```bash
# Inside the container
source /work/poky/oe-init-build-env /work/build-qemu
```

Then edit `build-qemu/conf/local.conf` and set the machine to the QEMU machine:

```
MACHINE ??= "qemu-riscv64"
```

The `PREFERRED_PROVIDER_virtual/kernel` is already set to `linux-qemu` inside
`qemu-riscv64.conf`, so no extra provider line is needed in `local.conf`.

The `build-qemu/conf/bblayers.conf` must list the same layers as the MangoPi
build (poky, meta-openembedded, meta-riscv, meta-mangopi).

Then build the image:

```bash
./run-build.sh bitbake core-image-base
```

Outputs are written to `build-qemu/tmp/deploy/images/qemu-riscv64/`.

### Booting under QEMU

The `qemuboot` class generates a `qemuboot.conf` file that `runqemu` uses to
boot the image with the correct QEMU options:

```bash
./run-build.sh runqemu qemu-riscv64
```

This boots the image under QEMU with the `virt` machine, 4 CPUs, a virtio
network device and a virtio block device for the root filesystem. The console
is available on the serial port (`ttyS0` / `hvc0`).

## Useful commands

```bash
# Open an interactive shell in the container
./run-build.sh bash

# List available recipes
./run-build.sh bitbake-layers show-recipes

# Show which layers are active and their priorities
./run-build.sh bitbake-layers show-layers

# Show the appends applied to a recipe
./run-build.sh bitbake-layers show-appends u-boot-allwinnerd1

# Clean a recipe and rebuild it
./run-build.sh bitbake -c cleanall linux-mangopi
./run-build.sh bitbake core-image-base
```

## Notes

- The `poky`, `meta-riscv` and `meta-openembedded` directories are git
  submodules. The `meta-riscv` machine config and the `meta-mangopi` layer
  contain local modifications described above.
- `build/`, `build-qemu/`, `downloads/` and `sstate-cache/` are generated and
  should not be committed. `downloads/` and `sstate-cache/` are Docker named
  volumes so they persist across container runs.
