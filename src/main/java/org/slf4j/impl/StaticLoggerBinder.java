package org.slf4j.impl;

import lombok.Getter;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;


public class StaticLoggerBinder implements LoggerFactoryBinder{

	private static class SingletonHelper{
		private static final StaticLoggerBinder INSTANCE = new StaticLoggerBinder();
	}

	public static final StaticLoggerBinder getSingleton(){
		return SingletonHelper.INSTANCE;
	}

	/**
	 * Declare the version of the SLF4J API this implementation is
	 * compiled against. The value of this field is usually modified
	 * with each release.
	 */
	// To avoid constant folding by the compiler, this field must *not* be final
	public static String REQUESTED_API_VERSION = "1.6";

	private static final String LOGGER_FACTORY_CLASS_STR = MyLoggerFactory.class.getName();


	/** The ILoggerFactory instance returned by the {@link #getLoggerFactory} method should always be the same object. */
	@Getter
	private final ILoggerFactory loggerFactory = new MyLoggerFactory();


	@Override
	public String getLoggerFactoryClassStr(){
		return LOGGER_FACTORY_CLASS_STR;
	}

}
