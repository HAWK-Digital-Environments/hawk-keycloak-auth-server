#!/bin/bash

SCRIPT_FILE=$(realpath ${BASH_SOURCE%/*})
cd $SCRIPT_FILE
docker run --rm \
  -v "$PWD":/usr/src/mymaven \
  -v "$PWD/.maven":/root/.m2 \
  -w /usr/src/mymaven \
  maven:3 mvn clean install
