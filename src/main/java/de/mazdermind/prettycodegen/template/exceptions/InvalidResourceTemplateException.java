package de.mazdermind.prettycodegen.template.exceptions;

public class InvalidResourceTemplateException extends GeneratorTemplateException {
	public InvalidResourceTemplateException(String template) {
		super("Template " + template + " does reference a Resource-Template but does not contain a valid Resource-Template-Name");
	}
}
