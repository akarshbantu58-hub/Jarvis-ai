#!/usr/bin/env bash
set -euo pipefail

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is not installed or not on PATH. Install Gradle or use a project wrapper." >&2
  exit 1
fi

exec gradle "$@"
