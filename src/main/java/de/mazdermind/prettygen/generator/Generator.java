package de.mazdermind.prettygen.generator;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.common.collect.ImmutableMap;

import de.mazdermind.prettygen.Configuration;
import de.mazdermind.prettygen.generator.exceptions.SwaggerParseException;
import de.mazdermind.prettygen.template.GeneratorTemplate;
import de.mazdermind.prettygen.template.TemplateLoader;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Generator {
	private final Configuration configuration;

	public Generator(Configuration configuration) {

		this.configuration = configuration;
		Velocity.init();
	}

	public void generate() {
		log.info("Loading OpenApi Specification: {}", configuration.getSourceLocation());
		OpenAPI openApi = parseOpenApi(configuration.getSourceLocation());

		Path templatePath = Paths.get(configuration.getTemplate());
		log.info("Loading Template: {}", templatePath);
		GeneratorTemplate generatorTemplate = TemplateLoader.loadTemplate(templatePath);

		log.info("Initialize Preprocessor");
		generatorTemplate.getPreprocessor().initialize(configuration);

		log.info("Passing all {} Schemas to Template-Preprocessor for preprocessSchemas", openApi.getComponents().getSchemas().size());
		generatorTemplate.getPreprocessor().preprocessSchemas(openApi.getComponents().getSchemas());

		log.info("Generating Output-Files for Schemas");
		openApi.getComponents().getSchemas().forEach((name, schema) ->
				generateSchema(
						Paths.get(configuration.getDestinationDirectory()),
						name, schema, openApi, generatorTemplate
				));
	}

	@SneakyThrows
	private void generateSchema(Path destinationDirectory, String schemaName, Schema schema, OpenAPI openApi, GeneratorTemplate generatorTemplate) {
		log.debug("Checking Preprocessor if a Schema-File should be generated for Schema {}", schemaName);
		if (!generatorTemplate.getPreprocessor().shouldGenerateSchema(schemaName, schema)) {
			log.info("Not generating a Schema-File for {} because the preprocessor did deny it", schemaName);
			return;
		}

		log.debug("Calling Preprocessor for additional Template-Args for Schema {}", schemaName);
		Map<String, Object> additionalTemplateArgs = generatorTemplate.getPreprocessor().additionalSchemaTemplateArgs(schemaName, schema);
		log.debug("Preprocessor provided additional Template-Args: {}", additionalTemplateArgs);

		log.debug("Rendering Template for Schema {}", schemaName);
		ImmutableMap<String, Object> context = ImmutableMap.<String, Object>builder()
				.put("info", openApi.getInfo())
				.put("schema", schema)
				.put("schemaName", schemaName)
				.putAll(additionalTemplateArgs)
				.build();

		String output = renderTemplate(generatorTemplate.getSchemaTemplate(), context);

		String schemaFilename = generatorTemplate.getPreprocessor().generateSchemaFilename(schemaName, schema);
		Path schemaPath = destinationDirectory.resolve(schemaFilename);

		Path parentPath = schemaPath.getParent();
		if (!parentPath.toFile().exists()) {
			log.info("Creating Dir {}", parentPath);
			Files.createDirectories(parentPath);
		}

		log.info("Generating Output-File {}", schemaPath);
		Files.write(schemaPath, output.getBytes(StandardCharsets.UTF_8));
	}

	private String renderTemplate(Template template, Map<String, Object> context) {
		HashMap<String, Object> writableContextCopy = new HashMap<>(context);
		VelocityContext velocityContext = new VelocityContext(writableContextCopy);

		StringWriter writer = new StringWriter();
		template.merge(velocityContext, writer);

		return writer.getBuffer().toString();
	}

	private OpenAPI parseOpenApi(String sourceLocation) {
		SwaggerParseResult result = new OpenAPIParser().readLocation(sourceLocation, null, null);
		if (SwaggerParseException.hasParseErrors(result)) {
			throw new SwaggerParseException(result);
		}

		return result.getOpenAPI();
	}
}
