FROM gitpod/workspace-full

# Install custom tools, runtimes, etc.
#
# More information: https://www.gitpod.io/docs/config-docker/

RUN apt update && \
    apt install tldr --assume-yes && \
    rm -rf /var/lib/apt/lists/*