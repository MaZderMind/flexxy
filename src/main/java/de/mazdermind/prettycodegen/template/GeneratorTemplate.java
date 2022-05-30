package de.mazdermind.prettycodegen.template;

import java.util.Map;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GeneratorTemplate {
	private Info info;

	private PreprocessorRef filenamePreprocessor;

	private String schemaTemplate;
	private PreprocessorRef schemaPreprocessor;

	private String apiTemplate;
	private PreprocessorRef apiPreprocessor;

	private Map<String, String> additionalTemplates;
}
