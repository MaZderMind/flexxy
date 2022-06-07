package de.mazdermind.prettycodegen.preprocessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.codehaus.groovy.runtime.StringGroovyMethods;

import com.github.slugify.Slugify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.experimental.UtilityClass;

// TODO Tests
@UtilityClass
public class CodeSlugify {
	private static final Map<String, String> REPLACEMENTS = ImmutableMap.<String, String>builder()
			.put("'", "")
			.put("!=", "not equal")
			.put(">=", "bigger then or equal")
			.put("<=", "smaller then or equal")
			.put("~=", "does not contain")
			.put("~", "contains")
			.put(">", "bigger then")
			.put("<", "smaller then")
			.put("=", "equal").build();

	private static final Pattern SPLITTER = Pattern.compile("((?<=\\p{Ll})(?=\\p{Lu})|[^\\p{L}])");
	private static final Slugify SLUGIFY = new Slugify().withCustomReplacements(REPLACEMENTS);

	private static String slugifyPrefix(String s) {
		if (StringGroovyMethods.getAt(s, 0).equals("-")) {
			return "minus " + s.substring(1);
		} else if (StringGroovyMethods.getAt(s, 0).equals("+")) {
			return "plus " + s.substring(1);
		} else if (StringGroovyMethods.getAt(s, 0).equals("_")) {
			return "underscore " + s.substring(1);
		}


		return s;
	}

	private static List<String> slugifyToWords(String s) {
		return Arrays.stream(SPLITTER.split(s))
				.flatMap(word -> Arrays.stream(
						SLUGIFY.slugify(word).split("-")
				))
				.collect(Collectors.toList());
	}

	// fooBarMoo
	public static String camelCase(String s) {
		List<String> words = slugifyToWords(slugifyPrefix(s));
		String firstWord = words.get(0);
		List<String> remainingWords = words.subList(1, words.size());
		List<String> casedWords = ImmutableList.<String>builder()
				.add(
						firstWord.substring(0, 1).toUpperCase() + firstWord.substring(1)
				)
				.addAll(
						remainingWords.stream()
								.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
								.collect(Collectors.toList())
				)
				.build();

		return String.join("", casedWords);
	}

	// FooBarMoo
	public static String pascalCase(String s) {
		List<String> words = slugifyToWords(slugifyPrefix(s));
		List<String> casedWords = words.stream()
				.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
				.collect(Collectors.toList());

		return String.join("", casedWords);
	}

	// FOO_BAR_MOO
	public static String constCase(String s) {
		List<String> words = slugifyToWords(slugifyPrefix(s));
		List<String> casedWords = words.stream()
				.map(String::toUpperCase)
				.collect(Collectors.toList());

		return String.join("_", casedWords);
	}

	// foo/bar/moo
	public static String pathCase(String s) {
		List<String> words = slugifyToWords(slugifyPrefix(s));
		List<String> casedWords = words.stream()
				.map(String::toLowerCase)
				.collect(Collectors.toList());

		return String.join("/", casedWords);
	}

	// foo_bar_moo
	public static String snakeCase(String s) {
		List<String> words = slugifyToWords(slugifyPrefix(s));
		List<String> casedWords = words.stream()
				.map(String::toLowerCase)
				.collect(Collectors.toList());

		return String.join("_", casedWords);
	}
}
