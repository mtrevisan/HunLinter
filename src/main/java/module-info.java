module HunLinter{
	requires java.desktop;
	requires java.management;
	requires java.prefs;
	requires jdk.unsupported;
	/*
	jdeps --ignore-missing-deps --generate-module-info jars .\json-simple-1.1.1.jar
	javac --patch-module json.simple=json-simple-1.1.1.jar .\jars\json.simple\module-info.java
	jar uf .\json-simple-1.1.1.jar -C .\jars\json.simple module-info.class
	*/
	requires json.simple;
	/*
	jdeps --ignore-missing-deps --generate-module-info jars .\commons-lang3-3.11.jar
	javac --patch-module org.apache.commons.lang3=commons-lang3-3.11.jar .\jars\org.apache.commons.lang3\module-info.java
	jar uf .\commons-lang3-3.11.jar -C .\jars\org.apache.commons.lang3 module-info.class
	*/
	requires org.apache.commons.lang3;
	requires org.apache.commons.io;
	requires org.apache.commons.text;
	requires org.jfree.jfreechart;
	/*
	jdeps --ignore-missing-deps --generate-module-info jars .\logback-classic-1.2.3.jar
	javac --patch-module logback.classic=logback-classic-1.2.3.jar .\jars\logback.classic\module-info.java
	jar uf .\logback-classic-1.2.3.jar -C .\jars\logback.classic module-info.class
	*/
	requires logback.classic;
	/*
	jdeps --ignore-missing-deps --generate-module-info jars .\logback-core-1.2.3.jar
	javac --patch-module logback.core=logback-core-1.2.3.jar .\jars\logback.core\module-info.java
	jar uf .\logback-core-1.2.3.jar -C .\jars\logback.core module-info.class
	*/
	requires logback.core;
	/*
	jdeps --ignore-missing-deps --generate-module-info jars .\slf4j-api-1.7.25.jar
	javac --patch-module slf4j.api=slf4j-api-1.7.25.jar .\jars\slf4j.api\module-info.java
	jar uf .\slf4j-api-1.7.25.jar -C .\jars\slf4j.api module-info.class
	*/
	requires slf4j.api;
	/*
	jdeps --ignore-missing-deps --generate-module-info jars .\hppcrt-0.7.5.jar
	javac --patch-module hppcrt=hppcrt-0.7.5.jar .\jars\hppcrt\module-info.java
	jar uf .\hppcrt-0.7.5.jar -C .\jars\hppcrt module-info.class
	*/
	requires hppcrt;

	exports io.github.mtrevisan.hunlinter.services.log to logback.core;

	exports io.github.mtrevisan.hunlinter;
}