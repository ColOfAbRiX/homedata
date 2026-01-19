#!/usr/bin/env bash
set -euo pipefail

# To account for the fact that I haven't yet published some libraries
rm -rf lib
mkdir -p lib
cp -r ~/.ivy2/local/com.colofabrix.scala/ lib/

docker build \
  -f Dockerfile \
  -t colofabrix/homedata:0.1.0 \
  ..

rm -rf lib
