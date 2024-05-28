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

declare -a branches=("master" "241" "233" "232" "231")

DEFAULT_BRANCH="${branches[0]}"

if [[ -z "${PRIVATE_KEY_PASSWORD}" ]]; then
  echo "Private key password does not exist!"
  exit 1
fi

if [ ! -f secrets/chain.crt ]; then
  echo "Certificate chain does not exist!"
  exit 1
fi

if [ ! -f secrets/private.pem ]; then
  echo "Plugin signing key does not exist!"
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
  git checkout "$i" && ./gradlew :plugin:verifyPluginSignature
  RESULT=$?
  if  [ $RESULT != 0 ]; then
    echo "Failed to build plugin on branch $i!"
    exit 1
  fi
done

git checkout "$DEFAULT_BRANCH"

mkdir -p build/dist

cp plugin/build/distributions/*-signed.zip build/dist/