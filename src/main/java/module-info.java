module io.github.mtrevisan.hunlinter{
	requires java.desktop;
	requires java.management;
	requires java.prefs;
	requires jdk.unsupported;
	requires org.apache.commons.lang3;
	requires org.apache.commons.io;
	requires org.apache.commons.text;
	requires org.jfree.jfreechart;
	requires hppcrt;
	requires org.slf4j;
	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;

	exports io.github.mtrevisan.hunlinter.services.log to ch.qos.logback.core;

	exports io.github.mtrevisan.hunlinter;
}