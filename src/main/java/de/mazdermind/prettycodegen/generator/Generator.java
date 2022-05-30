package de.mazdermind.prettycodegen.generator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import de.mazdermind.prettycodegen.Configuration;
import de.mazdermind.prettycodegen.generator.exceptions.SwaggerParseException;
import de.mazdermind.prettycodegen.template.GeneratorTemplate;
import de.mazdermind.prettycodegen.template.TemplateLoader;
import de.mazdermind.prettycodegen.template.space.SourceCodeFileDialect;
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
	}

	public void generate() {
		OpenAPI openApi = parseOpenApi(configuration.getSourceLocation());

		Path templatePath = Paths.get(configuration.getTemplate());
		GeneratorTemplate generatorTemplate = TemplateLoader.loadTemplate(templatePath);

		FileTemplateResolver templateResolver = new FileTemplateResolver();
		templateResolver.setPrefix(templatePath + File.separator);
		templateResolver.setTemplateMode(TemplateMode.TEXT);

		TemplateEngine templateEngine = new TemplateEngine();
		templateEngine.setTemplateResolver(templateResolver);
		templateEngine.addDialect(new SourceCodeFileDialect());

		openApi.getComponents().getSchemas().forEach((name, schema) ->
				generateSchema(
						Paths.get(configuration.getTargetDirectory()),
						name, schema, openApi, templateEngine, generatorTemplate
				));
	}

	@SneakyThrows
	private void generateSchema(Path targetDirectory, String schemaName, Schema schema, OpenAPI openApi, TemplateEngine templateEngine, GeneratorTemplate generatorTemplate) {
		Context context = new Context();
		context.setVariable("info", openApi.getInfo());
		context.setVariable("schema", schema);
		context.setVariable("schemaName", schemaName);
		String output = templateEngine.process(generatorTemplate.getSchemaTemplate(), context);

		String schemaFilename = (String) generatorTemplate.getFilenamePreprocessor()
				.call("formatSchemaFilename", schemaName)
				.orElse(schemaName);

		Path schemaPath = targetDirectory.resolve(schemaFilename);
		Path parentPath = schemaPath.getParent();
		log.info("Creating Dir {}", parentPath);
		Files.createDirectories(parentPath);

		log.info("Creating Schema-File {}", schemaPath);
		Files.write(schemaPath, output.getBytes(StandardCharsets.UTF_8));
	}

	private OpenAPI parseOpenApi(String sourceLocation) {
		SwaggerParseResult result = new OpenAPIParser().readLocation(sourceLocation, null, null);
		if (SwaggerParseException.hasParseErrors(result)) {
			throw new SwaggerParseException(result);
		}

		return result.getOpenAPI();
	}
}
