package de.mazdermind.prettygen.template;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Info {
	private List<String> reservedWords;

	private String preprocessor;

	private String schemaTemplate;
	private String apiTemplate;

	private Map<String, String> additionalTemplates;
}
