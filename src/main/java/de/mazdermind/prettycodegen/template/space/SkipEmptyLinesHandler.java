package de.mazdermind.prettycodegen.template.space;

import org.thymeleaf.engine.AbstractTemplateHandler;
import org.thymeleaf.model.IText;

public class SkipEmptyLinesHandler extends AbstractTemplateHandler {
	@Override
	public void handleText(IText text) {
		if (text.getText() != null && !text.getText().isEmpty()) {
			super.handleText(text);
		}
	}
}
