package de.mazdermind.prettycodegen.template;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Info {
	private List<String> reservedWords;

	private String filenamePreprocessor;

	private String schemaTemplate;
	private String schemaPreprocessor;

	private String apiTemplate;
	private String apiPreprocessor;

	private Map<String, String> additionalTemplates;
}
