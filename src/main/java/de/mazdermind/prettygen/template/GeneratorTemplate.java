package de.mazdermind.prettygen.template;

import java.util.Map;

import org.apache.velocity.Template;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GeneratorTemplate {
	private Info info;

	private IPreprocessor preprocessor;

	private Template schemaTemplate;
	private Template apiTemplate;
	private Map<String, Template> additionalTemplates;
}
