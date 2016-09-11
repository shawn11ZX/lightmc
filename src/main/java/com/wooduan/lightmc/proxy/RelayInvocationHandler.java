package com.wooduan.lightmc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.NetSession;
import com.wooduan.lightmc.RelayService;

public class RelayInvocationHandler<T> implements InvocationHandler, ApcRelayProxy {
	private Logger logger = null;
	final private NetSession session;
	final private ThreadLocal<Object> tag = new ThreadLocal<Object>();
	final RelayService service;
	
	public RelayInvocationHandler(final Class<T> clazz, NetSession netSession)
	{
		service = netSession.newOutputProxy(RelayService.class);
		logger = LoggerFactory.getLogger(clazz);
		session = netSession;
	}
	
	
	@Override
	public Object invoke(Object proxy, Method method,
			Object[] args) throws Throwable {
		
		if (method.getDeclaringClass().equals(ApcRelayProxy.class))
		{
			this.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(this, args);
		}
		else 
		{
			if (session != null && session.getChannel().isActive() && tag.get() != null)
			{
				service.onRelay(tag.get(), new APC(method.getName(), args));
			}
			else
			{
				logger.error("session is null or closed when callsing {}, userId{}", method.getName(), tag);
			}
		}
		return null;
	}



	@Override
	public void setExtraTag(Object tag) {
		this.tag.set(tag);
	}


}