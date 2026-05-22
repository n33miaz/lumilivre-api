#!/usr/bin/env bash
set -euo pipefail

CONTROLLER_DIR="src/main/java/br/com/lumilivre/api/controller"
SWAGGER_I18N_DIR="src/main/resources/i18n/swagger"

mapping_count=$(grep -RhoE '@(Get|Post|Put|Delete|Patch)Mapping' "$CONTROLLER_DIR" | wc -l | tr -d ' ')
operation_count=$(grep -Rho '@Operation(' "$CONTROLLER_DIR" | wc -l | tr -d ' ')
tag_count=$(grep -Rho '@Tag(' "$CONTROLLER_DIR" | wc -l | tr -d ' ')
controller_count=$(find "$CONTROLLER_DIR" -name '*Controller.java' | wc -l | tr -d ' ')

if [ "$mapping_count" != "$operation_count" ]; then
  echo "ERROR: OpenAPI operation coverage mismatch: mappings=$mapping_count operations=$operation_count"
  exit 1
fi

if [ "$tag_count" != "$controller_count" ]; then
  echo "ERROR: OpenAPI tag coverage mismatch: controllers=$controller_count tags=$tag_count"
  exit 1
fi

missing_operation_ids=$(grep -Rho '@Operation([^)]*)' "$CONTROLLER_DIR" | grep -vc 'operationId = ' || true)
if [ "$missing_operation_ids" != "0" ]; then
  echo "ERROR: Found @Operation annotations without operationId."
  exit 1
fi

while IFS= read -r operation_id; do
  for locale in pt_BR en_US; do
    if ! grep -RqhF "swagger.operation.${operation_id}.summary=" "$SWAGGER_I18N_DIR" --include="*_${locale}.properties"; then
      echo "ERROR: Missing summary key for operation '$operation_id' in $locale swagger bundles."
      exit 1
    fi
    if ! grep -RqhF "swagger.operation.${operation_id}.description=" "$SWAGGER_I18N_DIR" --include="*_${locale}.properties"; then
      echo "ERROR: Missing description key for operation '$operation_id' in $locale swagger bundles."
      exit 1
    fi
  done
done < <(grep -RhoE '@Operation\(operationId = "[^"]+"' "$CONTROLLER_DIR" | sed -E 's/.*operationId = "([^"]+)".*/\1/' | sort -u)

echo "OpenAPI annotation coverage check passed."
