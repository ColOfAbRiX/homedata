#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2> /dev/null || readlink -e .)"

JDK_VERSION=25.0.1_8

SCALA_VERSION="$(sed -nre "s/val scala3Version = \"(.*)\"/\1/p" "$REPO_ROOT/build.sbt")"
if [[ -z "$SCALA_VERSION" ]]; then
  echo "SCALA_VERSION not set"
  exit 1
fi

HOMEDATA_VERSION="$(sed -nre "s/^\s+version\s+\:= \"(.*)\",/\1/p" "$REPO_ROOT/build.sbt")"
if [[ -z "$HOMEDATA_VERSION" ]]; then
  echo "HOMEDATA_VERSION not set"
  exit 1
fi

SBT_VERSION="$(sed -nre "s/sbt.version=(.*)/\1/p" "$REPO_ROOT/project/build.properties")"
if [[ -z "$SBT_VERSION" ]]; then
  echo "SBT_VERSION not set"
  exit 1
fi

echo docker build \
  -f Dockerfile \
  -t colofabrix/homedata:${HOMEDATA_VERSION} \
  --build-arg JDK_VERSION=${JDK_VERSION} \
  --build-arg SBT_VERSION=${SBT_VERSION} \
  --build-arg HOMEDATA_VERSION=${HOMEDATA_VERSION} \
  --build-arg SCALA_VERSION=${SCALA_VERSION} \
  $@ \
  "$REPO_ROOT"
