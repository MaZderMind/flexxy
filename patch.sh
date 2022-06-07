#!/bin/bash
set -euo pipefail

echo "Format swagger-json"
jq < swagger-v3.v3.json >swagger-v3.v3-formated.json

echo "Patch up swagger-json"
jq '
  # Patch up StringList with actual string
  (.paths[][].parameters[].schema.items | select(."$ref" == "#/components/schemas/StringList")) |= {type: "string"}
  |
  # Patch FieldConfigurationDetails to add missing properties
  (.components.schemas.FieldConfigurationDetails.properties) +=
  {
    id: {
      type: "integer",
      description: "The ID of the referenced item.",
      format: "int64"
    },
    isDefault: {
      type: "boolean",
      "description": "Whether this is the default field configuration."
    }
  }
' <swagger-v3.v3.json >swagger-v3.v3-patched.json
