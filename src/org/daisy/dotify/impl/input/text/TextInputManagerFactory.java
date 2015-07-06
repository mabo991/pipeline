package org.daisy.dotify.impl.input.text;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.daisy.dotify.api.cr.InputManager;
import org.daisy.dotify.api.cr.InputManagerFactory;
import org.daisy.dotify.api.cr.TaskGroupSpecification;

import aQute.bnd.annotation.component.Component;

@Component
public class TextInputManagerFactory implements InputManagerFactory {
	private final Set<TaskGroupSpecification> specs;

	public TextInputManagerFactory() {
		this.specs = new HashSet<TaskGroupSpecification>();
		String text = "text";
		String txt = "txt";
		String obfl = "obfl";
		String sv = "sv-SE";
		String en = "en-US";
		this.specs.add(new TaskGroupSpecification(text, obfl, sv));
		this.specs.add(new TaskGroupSpecification(text, obfl, en));
		this.specs.add(new TaskGroupSpecification(txt, obfl, sv));
		this.specs.add(new TaskGroupSpecification(txt, obfl, en));
	}

	@Override
	public boolean supportsSpecification(TaskGroupSpecification spec) {
		return specs.contains(spec);
	}

	@Override
	public InputManager newInputManager(TaskGroupSpecification spec) {
		return new TextInputManager(spec.getLocale());
	}

	@Override
	public Set<TaskGroupSpecification> listSupportedSpecifications() {
		return Collections.unmodifiableSet(specs);
	}

}
