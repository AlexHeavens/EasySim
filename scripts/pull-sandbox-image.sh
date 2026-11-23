#!/bin/bash
# Pulls the image that the child JVM runs in (ADR 0003), as pinned in pins/sandbox/Dockerfile.
set -euo pipefail
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
image=$(sed -nE 's/^FROM (.+)$/\1/p' "$REPO_DIR/pins/sandbox/Dockerfile")
[ -n "$image" ] || { echo "no image in pins/sandbox/Dockerfile" >&2; exit 1; }
docker pull --quiet "$image"
