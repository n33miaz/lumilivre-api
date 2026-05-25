#!/usr/bin/env bash
# 1) Fails if any PT-BR i18n key is missing in the EN-US counterpart bundle,
#    or if an EN-US-only key drifts away from the source bundle.
# 2) Fails if Java services contain hardcoded PT-BR strings
#    (string literals with Latin accents). Internal log messages, logger
#    facades and source comments are intentionally ignored.
set -euo pipefail

I18N_DIR="src/main/resources/i18n"
SERVICE_DIR="src/main/java/br/com/lumilivre/api/service"
FAILED=0

check_keys() {
  local source_file="$1"
  local target_file="$2"

  while IFS='=' read -r key _rest; do
    [[ "$key" =~ ^[[:space:]]*$ ]] && continue
    [[ "$key" =~ ^[[:space:]]*# ]] && continue

    key=$(echo "$key" | tr -d ' \t')
    [[ -z "$key" ]] && continue

    if ! grep -qF "${key}=" "$target_file" 2>/dev/null; then
      echo "MISSING KEY: '$key' in $target_file (defined in $source_file)"
      FAILED=1
    fi
  done < "$source_file"
}

for pt_file in $(find "$I18N_DIR" -name "*_pt_BR.properties" | sort); do
  en_file="${pt_file/_pt_BR/_en_US}"

  if [ ! -f "$en_file" ]; then
    echo "ERROR: Missing EN-US bundle: $en_file"
    FAILED=1
    continue
  fi

  check_keys "$pt_file" "$en_file"
  check_keys "$en_file" "$pt_file"
done

# --- Services PT-BR hardcoded check ----------------------------------------
# Allow internal logs (log.* / System.err / // comments / /* ... */ blocks).
if [ -d "$SERVICE_DIR" ]; then
  TMP=$(mktemp)
  grep -RInE '"[^"]*[À-ÿ]+[^"]*"' "$SERVICE_DIR" \
    | grep -vE 'log\.(trace|debug|info|warn|error)' \
    | grep -vE 'System\.err\.println|System\.out\.println' \
    | grep -vE '^[^:]+:[0-9]+:[[:space:]]*//' \
    | grep -vE '^[^:]+:[0-9]+:[[:space:]]*\*' \
    > "$TMP" || true

  if [ -s "$TMP" ]; then
    echo "Hardcoded PT-BR strings found in services (must use MessageResolver):"
    cat "$TMP"
    FAILED=1
  fi
  rm -f "$TMP"
fi

if [ "$FAILED" -eq 0 ]; then
  echo "i18n coverage check passed."
fi

exit "$FAILED"
