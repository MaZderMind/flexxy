package de.mazdermind.prettycodegen.template.space;

import java.util.HashSet;
import java.util.Set;

import org.thymeleaf.dialect.IPostProcessorDialect;
import org.thymeleaf.postprocessor.IPostProcessor;
import org.thymeleaf.postprocessor.PostProcessor;
import org.thymeleaf.templatemode.TemplateMode;

public class SourceCodeFileDialect implements IPostProcessorDialect {
	@Override
	public String getName() {
		return "CustomTemplatePostProcessor";
	}

	@Override
	public Set<IPostProcessor> getPostProcessors() {
		Set<IPostProcessor> processors = new HashSet<>();
		processors.add(new PostProcessor(TemplateMode.TEXT, SkipEmptyLinesHandler.class, Integer.MAX_VALUE));
		return processors;
	}

	@Override
	public int getDialectPostProcessorPrecedence() {
		return 0;
	}
}
