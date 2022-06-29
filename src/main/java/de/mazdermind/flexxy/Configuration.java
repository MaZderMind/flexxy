package de.mazdermind.flexxy;

import java.util.Map;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Configuration {
	private String sourceLocation;
	private String destinationDirectory;
	private String template;

	private Map<String, String> templateOptions;
}
