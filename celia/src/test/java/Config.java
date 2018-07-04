import static org.daisy.pipeline.pax.exam.Options.thisPlatform;

public class Config {
	static String[] testDependencies() {
		return new String[] {
			"org.daisy.pipeline.modules.braille:common-utils:?",
			"org.daisy.pipeline.modules.braille:css-utils:?",
			"org.daisy.pipeline.modules.braille:liblouis-utils:?",
			"org.daisy.pipeline.modules.braille:liblouis-tables:?",
			"org.daisy.pipeline.modules.braille:liblouis-native:jar:" + thisPlatform() + ":?",
			"org.daisy.pipeline.modules.braille:libhyphen-core:?",
			"org.daisy.pipeline.modules.braille:libhyphen-native:jar:" + thisPlatform() + ":?",
			"org.daisy.pipeline.modules.braille:texhyph-core:?",
			"org.daisy.pipeline.modules.braille:pef-utils:?",
			"org.daisy.pipeline.modules.braille:dotify-utils:?",
			"org.daisy.pipeline.modules.braille:dotify-formatter:?",
			"org.daisy.pipeline.modules.braille:dtbook-to-pef:?",
			// FIXME: Dotify needs older version of jing
			"org.daisy.libs:jing:20120724.0.0",
		};
	}
}
