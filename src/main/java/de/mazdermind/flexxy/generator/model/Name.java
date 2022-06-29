package de.mazdermind.flexxy.generator.model;

public class Name {
	private final String name;

	public Name(String name) {
		this.name = name == null ? "" : name;
	}

	public String getLowercase() {
		return name.toLowerCase();
	}

	public String getUppercase() {
		return name.toUpperCase();
	}

	@Override
	public String toString() {
		return name;
	}
}
