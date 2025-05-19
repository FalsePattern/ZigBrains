#!/bin/sh
#
# This file is part of ZigBrains.
#
# Copyright (C) 2023-2025 FalsePattern
# All Rights Reserved
#
# The above copyright notice and this permission notice shall be included
# in all copies or substantial portions of the Software.
#
# ZigBrains is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, only version 3 of the License.
#
# ZigBrains is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with ZigBrains. If not, see <https://www.gnu.org/licenses/>.
#

set -e

declare -a branches=("master" "251" "243" "242" "241")

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
  git checkout "$i" && ./gradlew verifyPluginSignature
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