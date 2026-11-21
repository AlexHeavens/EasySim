#!/bin/bash
# Lint .github/workflows with the actionlint image pinned in pins/actionlint/Dockerfile.
set -euo pipefail
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
image=$(sed -nE 's/^FROM (rhysd\/actionlint:[0-9.]+)$/\1/p' "$REPO_DIR/pins/actionlint/Dockerfile")
[ -n "$image" ] || { echo "no actionlint image in pins/actionlint/Dockerfile" >&2; exit 1; }
docker run --rm --volume "$REPO_DIR:/repo" --workdir /repo "$image" -color
