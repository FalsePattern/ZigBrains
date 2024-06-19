#!/bin/sh
#
# Copyright 2023-2024 FalsePattern
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

declare -a branches=("master" "241" "233" "232")

DEFAULT_BRANCH="${branches[0]}"

if [[ -z "${PRIVATE_KEY_PASSWORD}" ]]; then
  echo "PRIVATE_KEY_PASSWORD missing!"
  exit 1
fi

if [[ -z "${MAVEN_DEPLOY_USER}" ]]; then
  echo "MAVEN_DEPLOY_USER missing!"
  exit 1
fi

if [[ -z "${MAVEN_DEPLOY_PASSWORD}" ]]; then
  echo "MAVEN_DEPLOY_PASSWORD missing!"
  exit 1
fi

if [[ -z "${IJ_PUBLISH_TOKEN}" ]]; then
  echo "IJ_PUBLISH_TOKEN missing!"
  exit 1
fi

if [ ! -f secrets/chain.crt ]; then
  echo "secrets/chain.crt missing!"
  exit 1
fi

if [ ! -f secrets/private.pem ]; then
  echo "secrets/private.pem missing!"
  exit 1
fi

git checkout "$DEFAULT_BRANCH" && ./gradlew clean

RESULT=$?

if  [ $RESULT != 0 ]; then
  echo "Failed to clean!"
  exit 1
fi


for i in "${branches[@]}"
do
  echo "Building branch $i"
  git checkout "$i" && ./gradlew :verifyPluginSignature
  RESULT=$?
  if  [ $RESULT != 0 ]; then
    echo "Failed to build plugin on branch $i!"
    exit 1
  fi
done

git checkout "$DEFAULT_BRANCH"

mkdir -p build/dist

cp build/distributions/*-signed.zip build/dist/

./gradlew publish