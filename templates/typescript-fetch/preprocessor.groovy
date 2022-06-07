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
        def enums = new ArrayList<Map<String, Object>>()
        def properties = properties(schemaName, schema, imports, enums)
        def additionalProperties = additionalProperties(schema, imports, schemaName, enums)

        imports.remove(schemaName)

        log.debug("Properties for {}: {}", schemaName, properties)
        log.debug("Imports for {}: {}", schemaName, imports)
        log.debug("Enums for {}: {}", schemaName, enums)
        log.debug("AdditionalProperties for {}: {}", schemaName, additionalProperties)

        return [
                properties          : properties,
                imports             : imports,
                enums               : enums,
                additionalProperties: additionalProperties,
        ]
    }

    static List<Map<String, Object>> properties(String schemaName, Schema schema, Set<String> imports,
                                                List<Map<String, Object>> enums) {
        schema.properties.collect {
            [
                    key        : it.key,
                    field      : quoteFieldIfNecessary(it.key),
                    description: sanitizeDescription(it.value.description),
                    required   : schema.required?.contains(it.key),
                    type       : mapType(it.value, imports, schemaName, it.key, enums),
            ]
        } as List<Map<String, Object>>
    }

    static String mapType(Schema schema, Set<String> imports, String schemaName, String propertyName,
                          List<Map<String, Object>> enums) {
        if (schema.type == "string" && schema.enum) {
            def enumName = "${schemaName}${propertyName.capitalize()}Enum"
            enums.add(propertyEnum(schemaName, propertyName, enumName, schema.enum))
            return enumName
        } else if (schema.type in ["string", "float", "number", "boolean"]) {
            return schema.type
        } else if (schema.type in ["integer"]) {
            return "number"
        } else if (schema instanceof ComposedSchema) {
            if (schema.allOf) {
                return schema.allOf
                        .collect { mapType(it, imports, schemaName, propertyName, enums) }
                        .each { imports.add(it) }
                        .join(" & ")
            } else if (schema.anyOf) {
                return schema.anyOf
                        .collect { mapType(it, imports, schemaName, propertyName, enums) }
                        .each { imports.add(it) }
                        .join(" | ")
            } else if (schema.oneOf) {
                return schema.oneOf
                        .collect { mapType(it, imports, schemaName, propertyName, enums) }
                        .each { imports.add(it) }
                        .join(" | ")
            }

            return "any"
        } else if (schema instanceof ArraySchema) {
            return mapType(schema.items, imports, schemaName, propertyName, enums) + '[]'
        } else if (schema.$ref) {
            def schemaRef = stripRefPrefix(schema.$ref)
            imports.add(schemaRef)
            return schemaRef
        } else if (schema.additionalProperties && !schema.properties) {
            // Inline Dict
            if (schema.additionalProperties instanceof Schema) {
                def additionalPropertiesType = mapType(schema.additionalProperties as Schema, imports, schemaName, propertyName, enums)
                return "{ [key: string]: ${additionalPropertiesType}; }"
            } else {
                return "{ [key: string]: any; }"
            }
        }

        // FIXME additionalProperties only -> map to dict
        // FIXME additionalProperties + properties -> additional dict property

        return "any"
    }

    static TreeMap<String, Object> propertyEnum(String schemaName, String propertyName, enumName, List<String> enumItems) {
        [
                schemaName  : schemaName,
                propertyName: propertyName,
                key         : enumName,
                items       : mapEnumItems(enumItems),
        ]
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
        def startsWithNumber = fieldKey[0].isNumber()
        def containsSpaces = fieldKey.contains(" ")
        def containsDashes = fieldKey.contains("-")
        if (startsWithNumber || containsSpaces || containsDashes) {
            return "'${fieldKey}'"
        }

        return fieldKey
    }

    static Object additionalProperties(Schema schema, Set<String> imports, String schemaName, List<Map<String, Object>> enums) {
        if (schema.additionalProperties == null || schema.additionalProperties == false) {
            return false
        } else if (schema.additionalProperties instanceof Schema) {
            return mapType(schema.additionalProperties as Schema, imports, schemaName, "AdditionalProperties", enums)
        } else {
            return "any"
        }
    }

    static String sanitizeDescription(String description) {
        if (description == null) {
            return null
        }

        description
                .replace("&", "&amp;")
                .replace("*/", "&#42;/")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }
}

return new Preprocessor()
