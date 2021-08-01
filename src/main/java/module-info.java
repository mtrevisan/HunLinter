module HunLinter{
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
	/*
	jdeps --ignore-missing-deps --generate-module-info jars .\hppc-0.7.2.jar
	javac --patch-module hppc=hppc-0.7.2.jar .\jars\hppc\module-info.java
	jar uf .\hppc-0.7.2.jar -C .\jars\hppc module-info.class
	*/
//	requires hppc;
	requires hppcrt;

	exports io.github.mtrevisan.hunlinter.services.log to logback.core;

	exports io.github.mtrevisan.hunlinter;
}