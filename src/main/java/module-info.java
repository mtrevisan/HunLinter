module io.github.mtrevisan.hunlinter{
	requires java.desktop;
	requires java.management;
	requires java.prefs;
	requires jdk.unsupported;
	requires json.simple;
	requires org.apache.commons.lang3;
	requires org.apache.commons.io;
	requires org.apache.commons.text;
	requires org.jfree.jfreechart;
	requires logback.classic;
	requires logback.core;
	requires slf4j.api;
	requires hppcrt;

	exports io.github.mtrevisan.hunlinter.services.log to logback.core;

	exports io.github.mtrevisan.hunlinter;
}