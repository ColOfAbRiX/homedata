#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2> /dev/null || readlink -e .)"

JDK_VERSION=25.0.3_9

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
echo "HOMEDATA_VERSION=${HOMEDATA_VERSION}" > "${REPO_ROOT}/docker/.env"

SBT_VERSION="$(sed -nre "s/sbt.version=(.*)/\1/p" "$REPO_ROOT/project/build.properties")"
if [[ -z "$SBT_VERSION" ]]; then
  echo "SBT_VERSION not set"
  exit 1
fi

# Parse known flags, pass the rest through to docker build
LOCAL_CACHE=false
POSITIONAL_ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --local-cache)
      LOCAL_CACHE=true
      shift
      ;;
    *)
      POSITIONAL_ARGS+=("$1")
      shift
      ;;
  esac
done
set -- "${POSITIONAL_ARGS[@]}"

# When --local-cache is set, copy local ivy2 artifacts into the build context
# so they are available inside the Docker container during sbt update.
IVY2_LOCAL_DIR="${REPO_ROOT}/.ivy2-local"
rm -rf "$IVY2_LOCAL_DIR"

if [[ "$LOCAL_CACHE" == "true" ]]; then
  IVY2_SOURCE="${IVY2_LOCAL:-$HOME/.ivy2/local}"
  if [[ ! -d "$IVY2_SOURCE" ]]; then
    echo "Local ivy2 cache not found at: $IVY2_SOURCE"
    echo "Set IVY2_LOCAL env var to override the path."
    exit 1
  fi
  echo "Copying local ivy2 cache from $IVY2_SOURCE ..."
  mkdir -p "$IVY2_LOCAL_DIR"
  cp -r "$IVY2_SOURCE/." "$IVY2_LOCAL_DIR/"
fi

# Always ensure the directory exists for Docker COPY (even if empty)
mkdir -p "$IVY2_LOCAL_DIR"

docker build \
  -f Dockerfile \
  -t colofabrix/homedata:${HOMEDATA_VERSION} \
  --build-arg JDK_VERSION=${JDK_VERSION} \
  --build-arg SBT_VERSION=${SBT_VERSION} \
  --build-arg HOMEDATA_VERSION=${HOMEDATA_VERSION} \
  --build-arg SCALA_VERSION=${SCALA_VERSION} \
  "$@" \
  "$REPO_ROOT"

# Clean up staging directory after build
rm -rf "$IVY2_LOCAL_DIR"
