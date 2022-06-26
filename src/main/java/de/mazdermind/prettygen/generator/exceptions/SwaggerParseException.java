package de.mazdermind.prettygen.generator.exceptions;

import java.util.List;

import io.swagger.v3.parser.core.models.SwaggerParseResult;

public class SwaggerParseException extends RuntimeException {
	public SwaggerParseException(SwaggerParseResult result) {
		this(result.getMessages());
	}

	public SwaggerParseException(List<String> messages) {
		super(String.join("\n", messages));
	}

	public static boolean hasParseErrors(SwaggerParseResult result) {
		return result.getMessages() != null && !result.getMessages().isEmpty();
	}
}
