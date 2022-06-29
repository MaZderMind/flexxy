package de.mazdermind.flexxy.template.exceptions;

public class GeneratorPreprocessorException extends RuntimeException {
	public GeneratorPreprocessorException(String message) {
		super(message);
	}

	public GeneratorPreprocessorException(String message, Throwable cause) {
		super(message, cause);
	}
}
