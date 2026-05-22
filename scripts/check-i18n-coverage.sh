#!/usr/bin/env bash
# Fails if any PT-BR i18n key is missing in the EN-US counterpart bundle,
# or if an EN-US-only key drifts away from the source bundle.
set -euo pipefail

I18N_DIR="src/main/resources/i18n"
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

if [ "$FAILED" -eq 0 ]; then
  echo "i18n coverage check passed."
fi

exit "$FAILED"
