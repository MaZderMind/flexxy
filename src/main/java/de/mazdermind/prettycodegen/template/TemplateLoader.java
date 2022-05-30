package de.mazdermind.prettycodegen.template;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TemplateLoader {

	private static final String TEMPLATE_INFO_FILE = "info.json";

	public static GeneratorTemplate loadTemplate(Path templatePath) {
		// TODO validate
		Info info = loadInfo(templatePath);

		return new GeneratorTemplate()
				.setInfo(info)
				.setFilenamePreprocessor(loadPreprocessor(templatePath, info.getFilenamePreprocessor()))
				.setApiTemplate(info.getApiTemplate())
				.setApiPreprocessor(loadPreprocessor(templatePath, info.getApiPreprocessor()))
				.setSchemaTemplate(info.getSchemaTemplate())
				.setSchemaPreprocessor(loadPreprocessor(templatePath, info.getSchemaPreprocessor()))
				.setAdditionalTemplates(info.getAdditionalTemplates());
	}

	@SneakyThrows
	public static PreprocessorRef loadPreprocessor(Path templatePath, @Nullable String preprocessorFilename) {
		if (preprocessorFilename == null) {
			return new PreprocessorRef();
		}

		Path preprocessorFile = templatePath.resolve(preprocessorFilename);
		String javascriptString = Files.readString(preprocessorFile, StandardCharsets.UTF_8);

		ScriptEngine engine = new ScriptEngineManager().getEngineByMimeType("application/javascript");
		engine.eval(javascriptString);

		return new PreprocessorRef((Invocable) engine);
	}

	@SneakyThrows
	private static Info loadInfo(Path templatePath) {
		Path infoFilePath = templatePath.resolve(TEMPLATE_INFO_FILE);
		InputStream stream = Files.newInputStream(infoFilePath);
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readValue(stream, Info.class);
	}
}
