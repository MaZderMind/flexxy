package de.mazdermind.prettycodegen.template;

import java.util.Collections;
import java.util.Map;

import de.mazdermind.prettycodegen.Configuration;
import io.swagger.v3.oas.models.media.Schema;

public interface IPreprocessor {

	default boolean shouldGenerateSchema(String schemaName, Schema schema) {
		return true;
	}

	default String generateSchemaFilename(String schemaName, Schema schema) {
		return schemaName;
	}

	default void preprocessSchemas(Map<String, Schema> schemas) {
	}

	default Map<String, Object> additionalSchemaTemplateArgs(String schemaName, Schema schema) {
		return Collections.emptyMap();
	}

	default void initialize(Configuration configuration) {
	}
}
