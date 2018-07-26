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
			if(!loggerMap.containsKey(name))
				loggerMap.put(name, new MyLoggerAdapter(name));

			return loggerMap.get(name);
		}
	}

}
