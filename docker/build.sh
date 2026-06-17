#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2> /dev/null || readlink -e .)"

SCALA_VERSION=3.8.3
HOMEDATA_VERSION=0.2.0
SBT_VERSION=1.12.9
JDK_VERSION=25.0.1_8

docker build \
  -f Dockerfile \
  -t colofabrix/homedata:${HOMEDATA_VERSION} \
  --build-arg JDK_VERSION=${JDK_VERSION} \
  --build-arg SBT_VERSION=${SBT_VERSION} \
  --build-arg HOMEDATA_VERSION=${HOMEDATA_VERSION} \
  --build-arg SCALA_VERSION=${SCALA_VERSION} \
  $@ \
  "$REPO_ROOT"

rm -rf lib
