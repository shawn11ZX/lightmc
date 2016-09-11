package com.wooduan.lightmc;

import io.netty.channel.Channel;

public interface NetSession {

	public void setReliable(boolean reliable);
	
	public abstract long getUid();

	public abstract Channel getChannel();

	public abstract <T> T newOutputProxy(Class<T> clazz);

	public abstract <T> T newRelayProxy(Class<T> clazz);

	public abstract boolean call(APC apc);

	public String getRemoteAddr();

	public String getName();
	
	public void setName(String name);
    
}