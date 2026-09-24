#!/usr/bin/env bash
# Claude Code PostToolUse hook (Edit|Write): after a Java file under TeamCode/ is
# changed, compile TeamCode (main + unit-test sources) and report errors back to
# Claude via exit code 2 + stderr. Non-Java edits exit immediately.
set -u

f=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty' 2>/dev/null)
case "$f" in
  */TeamCode/*.java) ;;
  *) exit 0 ;;
esac

cd "${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/.." && pwd)}" || exit 0
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

out=$(./gradlew :TeamCode:compileDevDebugJavaWithJavac :TeamCode:compileCompDebugJavaWithJavac \
        :TeamCode:compileDevDebugUnitTestJavaWithJavac -q --console=plain 2>&1)
status=$?

if [ "$status" -ne 0 ]; then
  {
    echo "TeamCode compile FAILED after editing ${f#"$PWD"/}:"
    echo "$out" | grep -vE 'warning: \[options\]|^WARNING: |^Note: |^[0-9]+ warnings?$' | head -60
  } >&2
  exit 2
fi
exit 0
