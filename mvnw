#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper Script for UNIX-based environments
# ----------------------------------------------------------------------------

command -v mvn >/dev/null 2>&1 && exec mvn "$@"

echo "Error: Maven is not installed or not in PATH." >&2
exit 1
