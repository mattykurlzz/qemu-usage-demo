#!/usr/bin/env bash
#
# Helper to run BitBake commands inside the Yocto Docker container.
#
# Usage:
#   ./run-build.sh bitbake core-image-base
#   ./run-build.sh bitbake-layers show-recipes
#   ./run-build.sh bash          # open an interactive shell
#
# The script sources the Yocto build environment (oe-init-build-env) and then
# runs the given command inside the container. The project directory is
# bind-mounted at /work, so the build directory and sources are shared with
# the host.

set -euo pipefail

# Directory of this script (the project root).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Build the image first if it does not exist yet.
if ! docker image inspect yocto-mangopi-mq-pro >/dev/null 2>&1; then
    echo "Building the Yocto Docker image (first run, this may take a while)..."
    docker compose -f "${SCRIPT_DIR}/docker-compose.yml" build
fi

# Run the command inside the container. The environment is initialized by
# sourcing oe-init-build-env, which sets up the build directory and PATH.
docker compose -f "${SCRIPT_DIR}/docker-compose.yml" run --rm \
    -w /work \
    yocto \
    bash -lc 'source /work/poky/oe-init-build-env /work/build && "$@"' _ "$@"