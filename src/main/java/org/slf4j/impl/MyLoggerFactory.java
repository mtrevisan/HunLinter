package org.slf4j.impl;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;


public class MyLoggerFactory implements ILoggerFactory{

	private final Map<String, MyLoggerAdapter> loggerMap = new HashMap<>();


	@Override
	public Logger getLogger(String name){
		synchronized(loggerMap){
			return loggerMap.putIfAbsent(name, new MyLoggerAdapter(name));
		}
	}

}
