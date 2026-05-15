#!/usr/bin/env bash
# Fails if any PT-BR i18n key is missing in the EN-US counterpart bundle.
set -euo pipefail

I18N_DIR="src/main/resources/i18n"
FAILED=0

for pt_file in $(find "$I18N_DIR" -name "messages_pt_BR.properties" | sort); do
  en_file="${pt_file/_pt_BR/_en_US}"

  if [ ! -f "$en_file" ]; then
    echo "ERROR: Missing EN-US bundle: $en_file"
    FAILED=1
    continue
  fi

  while IFS='=' read -r key _rest; do
    # skip blank lines and comments
    [[ "$key" =~ ^[[:space:]]*$ ]] && continue
    [[ "$key" =~ ^[[:space:]]*# ]] && continue

    key=$(echo "$key" | tr -d ' \t')
    [[ -z "$key" ]] && continue

    if ! grep -qE "^${key}[[:space:]]*=" "$en_file" 2>/dev/null; then
      echo "MISSING KEY: '$key' in $en_file (defined in $pt_file)"
      FAILED=1
    fi
  done < "$pt_file"
done

if [ "$FAILED" -eq 0 ]; then
  echo "i18n coverage check passed."
fi

exit "$FAILED"
