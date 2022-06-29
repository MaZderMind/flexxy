package de.mazdermind.flexxy.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.mazdermind.flexxy.template.exceptions.GeneratorPreprocessorException;
import de.mazdermind.flexxy.template.exceptions.GeneratorTemplateException;
import groovy.lang.GroovyShell;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class TemplateLoader {

	private static final String TEMPLATE_INFO_FILE = "info.json";

	public static GeneratorTemplate loadTemplate(Path templatePath) {
		// TODO validation
		Info info = loadInfo(templatePath);

		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty("resource.loader.file.path", templatePath.toString());

		return new GeneratorTemplate()
				.setInfo(info)
				.setApiTemplate(loadTemplateFile(velocityEngine, templatePath, info.getApiTemplate()))
				.setSchemaTemplate(loadTemplateFile(velocityEngine, templatePath, info.getSchemaTemplate()))
				.setAdditionalTemplates(loadAdditionalTemplates(velocityEngine, templatePath, info.getAdditionalTemplates()))
				.setPreprocessor(loadPreprocessor(templatePath, info.getPreprocessor()));
	}

	private static Map<String, Template> loadAdditionalTemplates(VelocityEngine velocityEngine, Path templatePath, Map<String, String> additionalTemplates) {
		return additionalTemplates.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> loadTemplateFile(velocityEngine, templatePath, e.getValue())
				));
	}

	private static Template loadTemplateFile(VelocityEngine velocityEngine, Path templatePath, String templateFilename) {
		Path templateFile = templatePath.resolve(templateFilename);
		log.debug("Loading Template-Tile: {}", templateFile);

		if (!templateFilename.endsWith(".vm")) {
			throw new GeneratorTemplateException("Template must be a .vm File, was " + templateFilename);
		}

		return velocityEngine.getTemplate(templateFilename, StandardCharsets.UTF_8.name());
	}

	@Nonnull
	public static IPreprocessor loadPreprocessor(Path templatePath, @Nullable String preprocessorFilename) {
		if (preprocessorFilename == null) {
			log.info("Not Loading a Preprocessor, because the Template did not specify one");

			return new IPreprocessor() {
			};
		}

		Path preprocessorFile = templatePath.resolve(preprocessorFilename);
		log.info("Loading Preprocessor {}", preprocessorFile);

		GroovyShell shell = new GroovyShell();
		Object preprocessor;
		try {
			preprocessor = shell.evaluate(preprocessorFile.toFile());
		} catch (IOException e) {
			throw new GeneratorPreprocessorException(String.format(
					"Error evaluating specified Preprocessor File: %s", preprocessorFilename), e);
		}

		if (preprocessor == null) {
			throw new GeneratorPreprocessorException(String.format(
					"Specified Preprocessor File did not return a Value %s", preprocessorFilename));
		}
		if (!(preprocessor instanceof IPreprocessor)) {
			throw new GeneratorPreprocessorException(String.format(
					"Specified Preprocessor File return a Value that does not implement IPreprocessor, was %s", preprocessor.getClass()));
		}

		log.debug("Loaded Preprocessor");
		return (IPreprocessor) preprocessor;
	}

	@SneakyThrows
	private static Info loadInfo(Path templatePath) {
		Path infoFilePath = templatePath.resolve(TEMPLATE_INFO_FILE);
		InputStream stream = Files.newInputStream(infoFilePath);
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readValue(stream, Info.class);
	}
}
