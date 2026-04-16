#!/bin/sh
# Maven Wrapper script
# Adapted from https://github.com/apache/maven-wrapper

set -euf

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

MAVEN_HOME="${MAVEN_USER_HOME:-${HOME}/.m2/wrapper}"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    MAVEN_URL=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2- | tr -d '\r')
fi

MAVEN_URL="${MAVEN_URL:-https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip}"
MAVEN_DIST_NAME=$(basename "$MAVEN_URL" .zip | sed 's/-bin$//')
MAVEN_DIST_DIR="$MAVEN_HOME/dists/$MAVEN_DIST_NAME"

if [ ! -d "$MAVEN_DIST_DIR" ]; then
    echo "Downloading Maven from $MAVEN_URL..."
    mkdir -p "$MAVEN_HOME/dists"
    TMPFILE=$(mktemp /tmp/maven-wrapper-XXXXXX.zip)
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL "$MAVEN_URL" -o "$TMPFILE"
    elif command -v wget > /dev/null 2>&1; then
        wget -q "$MAVEN_URL" -O "$TMPFILE"
    else
        echo "Error: curl or wget required to download Maven" >&2
        exit 1
    fi
    unzip -q "$TMPFILE" -d "$MAVEN_HOME/dists"
    rm -f "$TMPFILE"
fi

exec "$MAVEN_DIST_DIR/bin/mvn" "$@"
