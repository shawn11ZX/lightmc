package com.wooduan.lightmc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApcDiscardProxy {
	static Logger logger = LoggerFactory.getLogger(ApcDiscardProxy.class);
	
	public static class DummyInvocationHandler implements InvocationHandler {
		
		DummyInvocationHandler()
		{
		}
	
		@Override
		public Object invoke(Object proxy, Method method,
				Object[] args) throws Throwable {
			logger.warn("session is null or closed when calling {}", method.getName());
			return null;
		}
		
	}
	
	public static <T> T newDiscardClient(final Class<T> clazz)
	{
		@SuppressWarnings("unchecked")
		T f = (T) Proxy.newProxyInstance(clazz.getClassLoader(),
              new Class[] { clazz },
              new DummyInvocationHandler());
		return f;
	}
	
	
	
	
	
	
	
}
