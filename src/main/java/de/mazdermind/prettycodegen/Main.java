package de.mazdermind.prettycodegen;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import de.mazdermind.prettycodegen.generator.Generator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
	public static final int STATUS_CODE_INVALID_COMMAND_LINE = 1;

	public static void main(String[] args) {
		CommandLine commandLine = parseOptions(args);
		Configuration configuration = commandLineToConfiguration(commandLine);

		// TODO Validate Configuration

		log.info("Running with Configuration: {}", configuration);
		Generator generator = new Generator(configuration);
		generator.generate();
	}

	// TODO Test
	private static CommandLine parseOptions(String[] args) {
		Options options = new Options();

		options.addOption("s", "source", true, "Path to Source JSON-File\n" +
				"Example: -s /path/to/foo.json");
		options.addOption("d", "destination", true, "Path to Destination-Folder\n" +
				"Example -d /path/to/destination/");
		options.addOption("t", "template", true, "Path to Template\n" +
				"Example: -t /path/to/template/");
		options.addOption("o", "option", true, "Template-Specific Option\n" +
				"Example: -opackage=de.mazdermind.foo");

		CommandLineParser parser = new PosixParser();
		try {
			return parser.parse(options, args);
		} catch (ParseException parseException) {
			System.err.println("Failed parsing CommandLine. Reason: " + parseException.getMessage());
			System.exit(STATUS_CODE_INVALID_COMMAND_LINE);
			return null;
		}
	}

	// TODO Test
	private static Configuration commandLineToConfiguration(CommandLine commandLine) {
		return new Configuration()
				.setSourceLocation(commandLine.getOptionValue("s"))
				.setDestinationDirectory(commandLine.getOptionValue("d"))
				.setTemplate(commandLine.getOptionValue("t"))
				.setTemplateOptions(getLineOptionValues(commandLine));
	}

	// TODO Test
	private static Map<String, String> getLineOptionValues(CommandLine commandLine) {
		String[] options = commandLine.getOptionValues("o");
		if (options == null) {
			return Collections.emptyMap();
		}

		return Arrays.stream(options)
				.map(option -> Arrays.asList(option.split("=", 1)))
				.collect(Collectors.toMap(parts -> parts.get(0), parts -> parts.get(1)));
	}
}
