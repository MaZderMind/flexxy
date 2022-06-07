package de.mazdermind.prettycodegen;

import java.io.IOException;

import de.mazdermind.prettycodegen.generator.Generator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
	public static void main(String[] args) throws IOException {
		// TODO cli parsing & validation
		Configuration configuration = new Configuration()
				.setSourceLocation("/Users/pkoerner/Code/pretty-codegen/swagger-v3.v3-patched.json")
				.setTargetDirectory("/Users/pkoerner/Code/pretty-codegen/output/jira/")
				.setTemplate("/Users/pkoerner/Code/pretty-codegen/templates/typescript-fetch");

		log.info("Running with Configuration: {}", configuration);
		Generator generator = new Generator(configuration);
		generator.generate();
	}
}
