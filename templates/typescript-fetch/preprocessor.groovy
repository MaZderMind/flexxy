import com.google.common.collect.ImmutableMap
import de.mazdermind.flexxy.preprocessor.CodeSlugify
import de.mazdermind.flexxy.template.IPreprocessor
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory

//def schemaPreprocessor = evaluate(new File("./preprocessor/SchemaPreprocessor.groovy"))

class Preprocessor implements IPreprocessor {
    static log = LoggerFactory.getLogger("de.mazdermind.flexxy.template.typescript-fetch.Preprocessor")

    static String stripRefPrefix(String $ref) {
        def prefix = "#/components/schemas/"
        if ($ref.startsWith(prefix)) {
            return $ref.substring(prefix.length())
        }

        return null
    }

    @Override
    String generateSchemaFilename(String schemaName, Schema schema) {
        return "${schemaName}.ts"
    }

    @Override
    Map<String, Object> additionalSchemaTemplateArgs(String schemaName, Schema schema) {
        def imports = [] as Set<String>
        def enums = new ArrayList<Map<String, Object>>()
        def innerSchemas = new LinkedHashMap<String, Map<String, Object>>()

        def properties = properties(schemaName, schema, imports, enums, innerSchemas)
        def additionalProperties = additionalProperties(schema, imports, schemaName, enums, innerSchemas)

        imports.remove(schemaName)


        def baseInfo = [
                properties          : properties,
                imports             : imports,
                enums               : enums,
                innerSchemas        : innerSchemas,
                additionalProperties: additionalProperties,
        ]

        if (schema instanceof ComposedSchema) {
            return [
                    schemaType    : 'compound',
                    compoundSchema: compoundSchema(schema, imports, schemaName, "", enums, innerSchemas),
                    *             : baseInfo,
            ]
        } else if (schema.type == "string" && schema.enum) {
            return [
                    schemaType: 'enum',
                    enum      : propertyEnum(schemaName, "", schemaName, schema.enum),
                    *         : baseInfo,
            ]
        } else if (schema.type == "object" && schema.properties) {
            return [
                    schemaType: 'object',
                    *         : baseInfo,
            ]
        } else {
            return [
                    schemaType: 'alias',
                    alias     : mapType(schema, imports, schemaName, "", enums, innerSchemas),
                    *         : baseInfo,
            ]
        }
    }

    static List<Map<String, Object>> properties(String schemaName, Schema schema, Set<String> imports,
                                                List<Map<String, Object>> enums, HashMap<String, Map<String, Object>> innerSchemas) {
        schema.properties.collect {
            [
                    key        : it.key,
                    field      : quoteFieldIfNecessary(it.key),
                    description: sanitizeDescription(it.value.description),
                    required   : schema.required?.contains(it.key),
                    type       : mapType(it.value, imports, schemaName, it.key, enums, innerSchemas),
            ]
        } as List<Map<String, Object>>
    }

    static String mapType(Schema schema, Set<String> imports, String schemaName, String propertyName,
                          List<Map<String, Object>> enums, HashMap<String, Map<String, Object>> innerSchemas) {
        if (schema.type == "string" && schema.enum) {
            def enumName = "${schemaName}${propertyName.capitalize()}Enum"
            enums.add(propertyEnum(schemaName, propertyName, enumName, schema.enum))
            return enumName
        } else if (schema.type in ["string", "float", "number", "boolean"]) {
            return schema.type
        } else if (schema.type in ["integer"]) {
            return "number"
        } else if (schema instanceof ComposedSchema) {
            return compoundSchema(schema, imports, schemaName, propertyName, enums, innerSchemas)
        } else if (schema instanceof ArraySchema) {
            return mapType(schema.items, imports, schemaName, propertyName, enums, innerSchemas) + '[]'
        } else if (schema.$ref) {
            def schemaRef = stripRefPrefix(schema.$ref)
            imports.add(schemaRef)
            return schemaRef
        } else if (schema.additionalProperties && !schema.properties) {
            // Inline Dict
            if (schema.additionalProperties instanceof Schema) {
                def additionalPropertiesType = mapType(schema.additionalProperties as Schema, imports, schemaName, propertyName, enums, innerSchemas)
                return "{ [key: string]: ${additionalPropertiesType}; }"
            } else {
                return "{ [key: string]: any; }"
            }
        } else if (schema.type == "object" && schema.properties) {
            def innerClassPrefix = "${schemaName}${propertyName.capitalize()}"
            def innerClassName = "${innerClassPrefix}Type"

            def properties = properties(innerClassPrefix, schema, imports, enums, innerSchemas)
            def additionalProperties = additionalProperties(schema, imports, innerClassPrefix, enums, innerSchemas)

            innerSchemas.put(innerClassName.toString(), ImmutableMap.of(
                    "schema", schema,
                    "properties", properties,
                    "additionalProperties", additionalProperties,
            ))

            return innerClassName
        }

        return "any"
    }

    private static String compoundSchema(ComposedSchema schema, Set<String> imports, String schemaName, String propertyName, List<Map<String, Object>> enums, HashMap<String, Map<String, Object>> innerSchemas) {
        String combiner;
        List<Schema> items;
        if (schema.allOf) {
            combiner = " & "
            items = schema.allOf
        } else if (schema.anyOf) {
            combiner = " | "
            items = schema.anyOf
        } else if (schema.oneOf) {
            combiner = " | "
            items = schema.oneOf
        } else {
            return "any"
        }

        def single = items.size() == 1

        return items
                .withIndex()
                .collect { it, index ->
                    def proprtyName = single ? propertyName : "${propertyName}Part${index + 1}"
                    mapType(it, imports, schemaName, proprtyName, enums, innerSchemas)
                }
                .join(combiner)
    }

    static Map<String, Object> propertyEnum(String schemaName, String propertyName, enumName, List<String> enumItems) {
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
                    key  : it.isEmpty() ? "EMPTY" : CodeSlugify.constCase(it),
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

    static Object additionalProperties(Schema schema, Set<String> imports, String schemaName, List<Map<String, Object>> enums, HashMap<String, Map<String, Object>> innerSchemas) {
        if (schema.additionalProperties == null || schema.additionalProperties == false) {
            return false
        } else if (schema.additionalProperties instanceof Schema) {
            return mapType(schema.additionalProperties as Schema, imports, schemaName, "AdditionalProperties", enums, innerSchemas)
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
