package com.wooduan.lightmc;


public interface NetSessionEventHandler {

	public abstract void channelConnected(NetSession session);
	
	public abstract void channelConnectFailed(NetSession session);

	public abstract void channelDisconnected(NetSession session);

	public abstract void channelExceptionCaught(NetSession session, Throwable cause);

}