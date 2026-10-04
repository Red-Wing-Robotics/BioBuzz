#!/usr/bin/env bash
# Claude Code PostToolUse hook (Edit|Write): after a Java file under TeamCode/ is
# changed, compile TeamCode (main + unit-test sources) and report errors back to
# Claude via exit code 2 + stderr. Edits to Route View, or to the TeamCode sources it
# compiles (auto/, auto/route/, automaps/ — tools/routeview/build.gradle), also compile
# tools/routeview. Other edits exit immediately.
set -u

f=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty' 2>/dev/null)
teamcode=false
routeview=false
case "$f" in
  */TeamCode/*.java) teamcode=true ;;
esac
case "$f" in
  */tools/routeview/*.java|*/tools/routeview/*.gradle|*/teamcode/auto/*.java|*/teamcode/auto/route/*.java|*/teamcode/automaps/*.java)
    routeview=true ;;
esac
if ! $teamcode && ! $routeview; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/.." && pwd)}" || exit 0
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

failed() {
  {
    echo "$1 compile FAILED after editing ${f#"$PWD"/}:"
    echo "$2" | grep -vE 'warning: \[options\]|^WARNING: |^Note: |^[0-9]+ warnings?$' | head -60
  } >&2
  exit 2
}

if $teamcode; then
  out=$(./gradlew :TeamCode:compileDevDebugJavaWithJavac :TeamCode:compileCompDebugJavaWithJavac \
          :TeamCode:compileDevDebugUnitTestJavaWithJavac -q --console=plain 2>&1) || failed TeamCode "$out"
fi
if $routeview; then
  out=$(./gradlew -p tools/routeview compileJava compileTestJava -q --console=plain 2>&1) || failed "Route View" "$out"
fi
exit 0
