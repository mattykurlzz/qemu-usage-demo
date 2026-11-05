# Yocto build environment for the MangoPi MQ-Pro Allwinner D1, RISC-V 64-bit
#
# Usage:
#   docker build -t yocto-mangopi-mq-pro \
#     --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) .

FROM ubuntu:24.04

ARG USER_ID=1000
ARG GROUP_ID=1000
ARG USER_NAME=build

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    gawk \
    wget \
    git \
    diffstat \
    unzip \
    texinfo \
    gcc \
    build-essential \
    chrpath \
    socat \
    cpio \
    python3 \
    python3-pip \
    python3-pexpect \
    python3-git \
    python3-jinja2 \
    python3-subunit \
    python3-venv \
    xz-utils \
    debianutils \
    iputils-ping \
    libegl1 \
    libsdl2-dev \
    pylint \
    xterm \
    mesa-common-dev \
    zstd \
    liblz4-tool \
    file \
    bzip2 \
    gzip \
    tar \
    ca-certificates \
    curl \
    locales \
    && rm -rf /var/lib/apt/lists/*

RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && locale-gen
ENV LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8

RUN if getent passwd ${USER_ID} >/dev/null; then \
        usermod -l ${USER_NAME} -d /home/${USER_NAME} -m \
            "$(getent passwd ${USER_ID} | cut -d: -f1)"; \
    else \
        if ! getent group ${GROUP_ID} >/dev/null; then \
            groupadd -g ${GROUP_ID} ${USER_NAME}; \
        fi; \
        useradd -m -u ${USER_ID} -g ${GROUP_ID} -s /bin/bash ${USER_NAME}; \
    fi

WORKDIR /work
USER ${USER_NAME}
CMD ["/bin/bash"]