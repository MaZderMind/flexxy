package de.mazdermind.prettycodegen;

import java.io.IOException;

import de.mazdermind.prettycodegen.generator.Generator;

public class Main {
	public static void main(String[] args) throws IOException {
		// TODO cli parsing & validation
		Configuration configuration = new Configuration()
				.setSourceLocation("/Users/pkoerner/Code/pretty-codegen/min.json")
				.setTargetDirectory("/Users/pkoerner/Code/pretty-codegen/output/jira/")
				.setTemplate("/Users/pkoerner/Code/pretty-codegen/templates/typescript-fetch");

		Generator generator = new Generator(configuration);
		generator.generate();
	}
}
