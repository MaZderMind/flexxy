package de.mazdermind.flexxy.generator.model;

public class Description {
	private final String comment;

	public Description(String description) {
		this.comment = description == null ? "" : description;
	}

	@Override
	public String toString() {
		return comment;
	}
}
