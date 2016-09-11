package com.wooduan.lightmc.impl;

import io.netty.util.internal.ConcurrentSet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.ApcHelper;
import com.wooduan.lightmc.CallbackHook;
import com.wooduan.lightmc.NetSession;


public class ApcDispatcher {
	private final Logger Logger = LoggerFactory.getLogger(ApcDispatcher.class);
	static class Entry {
		Entry(Object instance, Method method) {
			this.instance = instance;
			this.method = method;
		}
		Object instance;
		Method method;
	}
	private ConcurrentHashMap<String, Entry> methodMap = new ConcurrentHashMap<String, Entry>();
	private CallbackHook hook;
    private ConcurrentSet<String> ignoredMethods = new ConcurrentSet<String>();
    
    public ApcDispatcher()
    {
    	
    }
	
    public void setHook(CallbackHook hook)
    {
    	this.hook = hook;
    }
	
	public <O extends T, T> void registerMethod(O instance, Class<T> interfaceClazz) {
		assert interfaceClazz != null;
		assert instance != null;
		assert (instance.getClass().getModifiers() | Modifier.PUBLIC) != 0;
		
		for (Method method : interfaceClazz.getMethods()) {
			try {
				if (method.getDeclaringClass() != Object.class)
				{
					Method oMethond = instance.getClass().getMethod(method.getName(), method.getParameterTypes());
					
					if (oMethond != null)
					{
						if (methodMap.containsKey(oMethond.getName()))
						{
							Logger.error("method re-register {} in {}", method.getName(), instance.getClass());	
						}
						methodMap.put(oMethond.getName(), new Entry(instance, oMethond));
					}
					else 
					{
						
						Logger.error("method {} not found in {}", method.getName(), instance.getClass());
						throw new IllegalArgumentException("method " + method.getName()+" not found in {}" + instance.getClass());
					}
				}
			} catch (Exception e) {
				Logger.error("error while register method", e );
				throw new IllegalArgumentException(e);
			}
		}
			
	}
	
	
	
	public void ignoreFunction(String functionName)
	{
		ignoredMethods.add(functionName);
	}
	public void call(NetSession netSession, APC request) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		String functionName = request.getFunctionName();
		if(!ignoredMethods.contains(functionName) && methodMap.containsKey(functionName)) {
			
			Entry entry = methodMap.get(functionName);
			
			try {
				ApcHelper.setCurrentNetSession(netSession);
				ApcHelper.setCurrentAPC(request);
				if (hook != null)
					hook.preCall(request);
				
				entry.method.invoke(entry.instance, request.getParameters());
				
				if (hook != null)
					hook.postCall(request);
			}
			catch (Exception e)
			{
				Logger.error("invoking method error {}", request, e);
				throw new InvocationTargetException(e);
			}
			finally {
				ApcHelper.setCurrentAPC(null);
				ApcHelper.setCurrentNetSession(null);
			}
		} else {
			Logger.error("the method [" + functionName +"] does not exist, or not registerd with @Procedure {}", functionName);
		}
	}

	
	
}
