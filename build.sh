#!/bin/bash

SCRIPT_FILE=$(realpath ${BASH_SOURCE%/*})
cd $SCRIPT_FILE

VERSION="0.0.1"
if [ -n "$1" ]; then
  VERSION=$1
fi

docker run --rm \
  -v "$PWD":/usr/src/mymaven \
  -v "$PWD/.maven":/root/.m2 \
  -w /usr/src/mymaven \
  maven:3 mvn -DpackageVersion=${VERSION} clean install
