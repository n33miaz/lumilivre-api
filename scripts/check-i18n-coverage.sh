#!/usr/bin/env bash
# 1) Fails if the message bundles drift between locales. PT-BR is the source of
#    truth: every business bundle must expose the exact same key set in the five
#    published languages, and every Swagger bundle in the two documentation
#    languages. A key that exists in only some locales silently falls back and
#    the user reads the wrong language mid-sentence.
# 2) Fails if the positional placeholders of a key ({0}, {1}, ...) differ between
#    locales. Word order changes between languages — Hindi and Mandarin do not
#    follow Portuguese order — but the argument set must not: a dropped {0}
#    means a sentence with a hole in it.
# 3) Fails if Java services contain hardcoded PT-BR strings
#    (string literals with Latin accents). Internal log messages, logger
#    facades and source comments are intentionally ignored.
set -euo pipefail

I18N_DIR="src/main/resources/i18n"
SWAGGER_DIR="$I18N_DIR/swagger"
SERVICE_DIR="src/main/java/br/com/lumilivre/api/service"
FAILED=0

# Business messages reach the end user, so they ship in five languages.
# The Swagger bundles stay in two on purpose: they document the contract for
# developers and feed only the api-pt-br / api-en-us groups, so a third spec
# would be maintenance with no reader.
MESSAGE_LOCALES="en_US es zh hi"
SWAGGER_LOCALES="en_US"

# Accented Portuguese letters only. A broad [À-ÿ] range also matches punctuation
# such as the "·" separator, which is not translatable copy.
ACCENTED='áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ'

if [ ! -d "$I18N_DIR" ]; then
  echo "ERROR: $I18N_DIR not found. Run this script from the lumilivre-api root."
  exit 1
fi

keys_of() {
  sed -nE 's/^[[:space:]]*([A-Za-z0-9_.-]+)[[:space:]]*=.*/\1/p' "$1" | LC_ALL=C sort -u
}

# Set comparison in both directions: a key missing in the target and a key the
# target invented on its own are both drift.
compare_keys() {
  local source_file="$1"
  local target_file="$2"
  local missing extra

  missing=$(LC_ALL=C comm -23 <(keys_of "$source_file") <(keys_of "$target_file"))
  extra=$(LC_ALL=C comm -13 <(keys_of "$source_file") <(keys_of "$target_file"))

  if [ -n "$missing" ]; then
    echo "MISSING KEYS in $target_file (defined in $source_file):"
    echo "$missing" | sed 's/^/  - /'
    FAILED=1
  fi
  if [ -n "$extra" ]; then
    echo "UNKNOWN KEYS in $target_file (absent from $source_file):"
    echo "$extra" | sed 's/^/  - /'
    FAILED=1
  fi
}

# Only {0}-style indices are compared. {min}/{max}/{value} come from Bean
# Validation, are resolved by Hibernate and are not positional arguments.
compare_placeholders() {
  local source_file="$1"
  local target_file="$2"
  local report

  report=$(awk -v src="$source_file" '
    function slots(value,   toks, n, i, j, t, out, seen) {
      n = 0
      while (match(value, /\{[0-9]+\}/)) {
        t = substr(value, RSTART, RLENGTH)
        if (!(t in seen)) { seen[t] = 1; toks[++n] = t }
        value = substr(value, RSTART + RLENGTH)
      }
      for (i = 1; i < n; i++)
        for (j = i + 1; j <= n; j++)
          if (toks[j] < toks[i]) { t = toks[i]; toks[i] = toks[j]; toks[j] = t }
      out = ""
      for (i = 1; i <= n; i++) out = out toks[i]
      return out
    }
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    !index($0, "=") { next }
    {
      key = substr($0, 1, index($0, "=") - 1)
      gsub(/[[:space:]]/, "", key)
      if (key == "") next
      value = substr($0, index($0, "=") + 1)
      if (FILENAME == src) { source_slots[key] = slots(value) }
      else { target_slots[key] = slots(value) }
    }
    END {
      for (key in source_slots)
        if (key in target_slots && source_slots[key] != target_slots[key])
          printf "  - %s: source=[%s] target=[%s]\n", key, source_slots[key], target_slots[key]
    }
  ' "$source_file" "$target_file" | LC_ALL=C sort)

  if [ -n "$report" ]; then
    echo "PLACEHOLDER MISMATCH in $target_file (vs $source_file):"
    echo "$report"
    FAILED=1
  fi
}

check_bundle() {
  local pt_file="$1"
  local locales="$2"

  for locale in $locales; do
    local target_file="${pt_file/_pt_BR/_$locale}"

    if [ ! -f "$target_file" ]; then
      echo "ERROR: missing bundle for locale $locale: $target_file"
      FAILED=1
      continue
    fi

    compare_keys "$pt_file" "$target_file"
    compare_placeholders "$pt_file" "$target_file"
  done
}

for pt_file in $(find "$I18N_DIR" -name "*_pt_BR.properties" | LC_ALL=C sort); do
  case "$pt_file" in
    "$SWAGGER_DIR"/*) check_bundle "$pt_file" "$SWAGGER_LOCALES" ;;
    *)                check_bundle "$pt_file" "$MESSAGE_LOCALES" ;;
  esac
done

# --- Services PT-BR hardcoded check ----------------------------------------
# Allow internal logs (log.* / System.err / // comments / /* ... */ blocks).
if [ -d "$SERVICE_DIR" ]; then
  TMP=$(mktemp)
  grep -RInE "\"[^\"]*[$ACCENTED]+[^\"]*\"" "$SERVICE_DIR" \
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
  echo "i18n coverage check passed (messages: pt_BR $MESSAGE_LOCALES | swagger: pt_BR $SWAGGER_LOCALES)."
fi

exit "$FAILED"
