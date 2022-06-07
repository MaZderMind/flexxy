import de.mazdermind.prettycodegen.preprocessor.CodeSlugify
import de.mazdermind.prettycodegen.template.IPreprocessor
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory

//def schemaPreprocessor = evaluate(new File("./preprocessor/SchemaPreprocessor.groovy"))

// TODO Reusable enums
class Preprocessor implements IPreprocessor {
    def log = LoggerFactory.getLogger("de.mazdermind.prettycodegen.template.typescriptFetch.Preprocessor")
    def schemaAliases = [:]

    static String stripRefPrefix(String $ref) {
        def prefix = "#/components/schemas/"
        if ($ref.startsWith(prefix)) {
            return $ref.substring(prefix.length())
        }

        return null
    }

    @Override
    void preprocessSchemas(Map<String, Schema> schemas) {
        log.info("Generating Schema-Aliases Map")
        schemas.each {
            def schema = it.value

            if (schema.type == 'string' && !schema.enum) {
                // A String-Alias Type
                schemaAliases[it.key] = 'string'
            }
        }
        log.debug("Schema-Aliases: {}", schemaAliases)
    }

    @Override
    boolean shouldGenerateSchema(String schemaName, Schema schema) {
        // do not create Schema-Files for pure Aliases
        return !schemaAliases.containsKey(schemaName)
    }

    @Override
    String generateSchemaFilename(String schemaName, Schema schema) {
        return "${schemaName}.ts"
    }

    @Override
    Map<String, Object> additionalSchemaTemplateArgs(String schemaName, Schema schema) {
        def imports = [] as Set<String>
        def properties = properties(schemaName, schema, imports)
        def enums = propertyEnums(schemaName, schema)
        log.debug("Properties for {}: {}", schemaName, properties)
        log.debug("Imports for {}: {}", schemaName, imports)
        log.debug("Enums for {}: {}", schemaName, enums)

        return [
                properties: properties,
                imports   : imports,
                enums     : enums,
        ]
    }

    static List<Map<String, Object>> properties(String schemaName, Schema schema, Set<String> imports) {
        schema.properties.collect {
            [
                    key        : it.key,
                    field      : quoteFieldIfNecessary(it.key),
                    description: it.value.description,
                    required   : schema.required?.contains(it.key),
                    type       : mapType(it.value, imports, schemaName, it.key),
            ]
        } as List<Map<String, Object>>
    }

    static String mapType(Schema schema, Set<String> imports, String schemaName, String propertyName) {
        if (schema.type == "string" && schema.enum) {
            return "${schemaName}${propertyName.capitalize()}Enum"
        } else if (schema.type in ["string", "float", "number", "boolean"]) {
            return schema.type
        } else if (schema.type in ["integer"]) {
            return "number"
        } else if (schema instanceof ComposedSchema) {
            if (schema.allOf) {
                return schema.allOf
                        .collect { mapType(it, imports, schemaName, propertyName) }
                        .each { imports.add(it) }
                        .join(" & ")
            } else if (schema.anyOf) {
                return schema.anyOf
                        .collect { mapType(it, imports, schemaName, propertyName) }
                        .each { imports.add(it) }
                        .join(" | ")
            } else if (schema.oneOf) {
                return schema.oneOf
                        .collect { mapType(it, imports, schemaName, propertyName) }
                        .each { imports.add(it) }
                        .join(" | ")
            }

            return "any"
        } else if (schema instanceof ArraySchema) {
            return mapType(schema.items, imports, schemaName, propertyName) + '[]'
        } else if (schema.$ref) {
            def schemaRef = stripRefPrefix(schema.$ref)
            imports.add(schemaRef)
            return schemaRef
        } else if (schema.additionalProperties) {
            // Inline Dict
            if (schema.additionalProperties instanceof Schema) {
                def additionalPropertiesType = mapType(schema.additionalProperties as Schema, imports, schemaName, propertyName)
                return "{ [key: string]: ${additionalPropertiesType}; }"
            } else {
                return "{ [key: string]: any; }"
            }
        }

        // FIXME additionalProperties only -> map to dict
        // FIXME additionalProperties + properties -> additional dict property

        return "any"
    }

    static List<Map<String, Object>> propertyEnums(String schemaName, Schema schema) {
        schema.properties
                .findAll { it.value.type == "string" && it.value.enum }
                .collect {
                    [
                            schemaName  : schemaName,
                            propertyName: it.key,
                            key         : "${schemaName}${it.key.capitalize()}Enum",
                            items       : mapEnumItems(it.value.enum),
                    ]
                }
    }

    static List<Map<String, String>> mapEnumItems(List<String> enumItems) {
        enumItems.collect({
            [
                    key  : CodeSlugify.constCase(it),
                    value: it,
            ]
        })
    }

    static String quoteFieldIfNecessary(String fieldKey) {
        if (fieldKey[0].isNumber()) {
            return "'${fieldKey}'"
        }

        return fieldKey
    }
}

return new Preprocessor()
