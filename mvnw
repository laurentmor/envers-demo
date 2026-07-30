#!/bin/sh
# SPDX-FileCopyrightText: 2026 Your Organization
# SPDX-License-Identifier: MIT
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
# ----------------------------------------------------------------------------

# If JAVA_HOME is not set we try to find JAVA from PATH
if [ -z "$JAVA_HOME" ] ; then
  JAVACMD=$(command -v java)
  if [ -z "$JAVACMD" ] ; then
    echo "Unable to find 'java' command in PATH. Please set JAVA_HOME." >&2
    exit 1
  fi
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

# Determine the script directory
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-}"
if [ -z "$MAVEN_PROJECTBASEDIR" ] ; then
  MAVEN_PROJECTBASEDIR=$(pwd)
fi

MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Read distributionUrl
if [ -r "$MAVEN_WRAPPER_PROPERTIES" ] ; then
  DISTRIBUTION_URL=$(grep "distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | sed "s/.*=//")
else
  echo "Could not read $MAVEN_WRAPPER_PROPERTIES" >&2
  exit 1
fi

# Derive cache dir: "apache-maven-3.9.9-bin" -> "apache-maven-3.9.9"
MAVEN_DIST_FILE=$(basename "$DISTRIBUTION_URL" .zip)   # e.g. apache-maven-3.9.9-bin
MAVEN_DIST_NAME="${MAVEN_DIST_FILE%-bin}"               # e.g. apache-maven-3.9.9
MAVEN_CACHE_DIR="${MAVEN_USER_HOME:-${HOME}/.m2/wrapper/dists}/$MAVEN_DIST_NAME"

# Try to find mvn executable already cached (the zip extracts to a nested
# subdir, e.g. $MAVEN_CACHE_DIR/apache-maven-3.9.9/bin/mvn, so we use find)
MAVEN_EXEC=$(find "$MAVEN_CACHE_DIR" -name "mvn" -not -name "*.cmd" -maxdepth 4 2>/dev/null | head -1)

if [ -z "$MAVEN_EXEC" ] ; then
  echo "Downloading Maven from $DISTRIBUTION_URL ..."
  mkdir -p "$MAVEN_CACHE_DIR"
  TMP_DOWNLOAD="$MAVEN_CACHE_DIR/download-$$.zip"
  if command -v curl > /dev/null 2>&1 ; then
    curl -fsSL -o "$TMP_DOWNLOAD" "$DISTRIBUTION_URL"
  elif command -v wget > /dev/null 2>&1 ; then
    wget -q -O "$TMP_DOWNLOAD" "$DISTRIBUTION_URL"
  else
    echo "Neither curl nor wget found. Cannot download Maven." >&2
    exit 1
  fi
  unzip -q "$TMP_DOWNLOAD" -d "$MAVEN_CACHE_DIR" && rm "$TMP_DOWNLOAD"
  MAVEN_EXEC=$(find "$MAVEN_CACHE_DIR" -name "mvn" -not -name "*.cmd" -maxdepth 4 2>/dev/null | head -1)
fi

if [ -z "$MAVEN_EXEC" ] ; then
  echo "Could not locate mvn under $MAVEN_CACHE_DIR" >&2
  exit 1
fi

exec "$MAVEN_EXEC" "$@"
