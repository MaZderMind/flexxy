package de.mazdermind.prettycodegen;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Configuration {
	private String packagePrefix;

	private String sourceLocation;
	private String targetDirectory;
	private String template;
}
