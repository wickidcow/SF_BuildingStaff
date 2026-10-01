#!/usr/bin/env bash
# Exact published compile-only API. This does not install a server plugin.
set -euo pipefail
VERSION=0.43.0-26.2
EXPECTED=1f5f935702647e8dca9844eeb8cb937170485962d00bfa2eff95f399e9867d71
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
curl --fail --location --silent --show-error --retry 4 --connect-timeout 15 --max-time 180 \
    "https://github.com/pylonmc/rebar/releases/download/${VERSION}/rebar-${VERSION}.jar" -o "$TMP/rebar.jar"
printf '%s  %s\n' "$EXPECTED" "$TMP/rebar.jar" | sha256sum -c -
mvn --batch-mode --no-transfer-progress install:install-file \
    -Dfile="$TMP/rebar.jar" -DgroupId=io.github.pylonmc -DartifactId=rebar \
    -Dversion="$VERSION" -Dpackaging=jar -DgeneratePom=true
