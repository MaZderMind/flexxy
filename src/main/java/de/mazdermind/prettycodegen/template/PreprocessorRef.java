package de.mazdermind.prettycodegen.template;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.script.Invocable;
import javax.script.ScriptException;

public class PreprocessorRef {
	@Nullable
	private final Invocable invocable;

	PreprocessorRef() {
		this(null);
	}

	PreprocessorRef(@Nullable Invocable invocable) {
		this.invocable = invocable;
	}

	public boolean isPresent() {
		return invocable != null;
	}

	public Optional<Object> call(String name, Object... args) {
		if (invocable == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(
					invocable.invokeFunction(name, args)
			);
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			return Optional.empty();
		}
	}
}
