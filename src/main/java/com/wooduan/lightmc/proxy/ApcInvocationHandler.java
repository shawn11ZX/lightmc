package com.wooduan.lightmc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.NetSession;

public class ApcInvocationHandler<T> implements InvocationHandler {
	
	final private NetSession session;
	
	public ApcInvocationHandler(final Class<T> clazz,  final NetSession session)
	{
		this.session = session;
		
	}

	@Override
	public Object invoke(Object proxy, Method method,
			Object[] args) throws Throwable {
		
		try {
			if (session == null)
			{
				ApcDiscardProxy.logger.warn("session is null {}", method.getName());
				
			} 
			else if (!session.getChannel().isActive()) {
				ApcDiscardProxy.logger.warn("session is closed {}", method.getName());
			} 
			else
			{
				synchronized(session) {
					session.call(new APC(method.getName(), args));
				}
			}
		}
		catch (Exception exception)
		{
			ApcDiscardProxy.logger.error("error while SyncInvocationHandler", exception);
		}
		return null;
	}

	
}