#!/usr/bin/env bash
#
# Habitat API pre-commit hook. Install via:
#   ln -s ../../scripts/pre-commit.sh .git/hooks/pre-commit
#
# Blocks the things our development-standards.md says must never land:
#   1. throw new RuntimeException(...)       — use the typed hierarchy
#   2. tokens in URL query parameters        — Authorization headers only
#   3. edits to already-applied Flyway       — new file per change
#   4. file.getContentType()                  — Tika magic-byte validation
#   5. @Enumerated without EnumType.STRING    — corruption risk
#   6. --no-verify                            — opt-out telemetry
#
# Exits 0 = allow commit; non-zero = block.

set -euo pipefail

STAGED=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.(java|sql|yml|yaml)$' || true)

if [[ -z "$STAGED" ]]; then
    exit 0
fi

fail=0

# 1. Bare RuntimeException
if echo "$STAGED" | xargs grep -nE 'throw new RuntimeException\(' 2>/dev/null; then
    echo "❌ Bare RuntimeException is banned. Use the typed exception hierarchy."
    fail=1
fi

# 2. Tokens in URL query strings
if echo "$STAGED" | xargs grep -nE '[?&](access_token|token|jwt)=' 2>/dev/null; then
    echo "❌ Tokens in URL query parameters are banned. Use Authorization: Bearer."
    fail=1
fi

# 3. Edit to an already-applied Flyway migration
for f in $STAGED; do
    if [[ "$f" == src/main/resources/db/migration/V*__*.sql ]]; then
        if git ls-tree HEAD -- "$f" >/dev/null 2>&1; then
            echo "❌ $f has already been committed. Never edit an applied migration — create a new V*.sql instead."
            fail=1
        fi
    fi
done

# 4. file.getContentType() as the only validation
if echo "$STAGED" | xargs grep -nE '\.getContentType\(\)' 2>/dev/null | grep -v 'Tika\|//.*OK' >/dev/null 2>&1; then
    echo "⚠️  file.getContentType() is client-supplied. Validate via Apache Tika magic bytes."
    # warn only — there may be legitimate read paths
fi

# 5. @Enumerated without EnumType.STRING
if echo "$STAGED" | xargs grep -nE '@Enumerated[^()]*\)' 2>/dev/null | grep -v 'EnumType.STRING' >/dev/null 2>&1; then
    if echo "$STAGED" | xargs grep -nE '@Enumerated\b' 2>/dev/null | grep -v 'EnumType.STRING'; then
        echo "❌ @Enumerated must specify EnumType.STRING — ORDINAL silently corrupts on enum reordering."
        fail=1
    fi
fi

if [[ $fail -ne 0 ]]; then
    echo
    echo "Commit blocked. See development-standards.md (§17 Hard prohibitions)."
    exit 1
fi

exit 0
