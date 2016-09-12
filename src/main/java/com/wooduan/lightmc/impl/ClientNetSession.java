package com.wooduan.lightmc.impl;

import io.netty.channel.Channel;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;

import com.wooduan.lightmc.ApcClientFactory;
import com.wooduan.lightmc.HeartBeatHandler;
import com.wooduan.lightmc.NetSessionEventHandler;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;


public class ClientNetSession extends AbstractNetSession {
	/**
	 * 
	 */
	final String ip;
	final int port;
	final ApcClientFactory parent;
	
	private volatile ScheduledFuture autoReconnectFuture;
	private volatile Channel channel;
	final private NetSessionEventHandler netSessionEventHandler;
	
	public ClientNetSession(String name, ApcClientFactory parent,
			boolean reliable,
			String ip, 
			int port, 
			NetSessionEventHandler netSessionEventHandler,
			ApcStatisticRecorder statisticRecorder) {
		super(reliable, statisticRecorder);
		setName(name);
		this.parent = parent;
		this.ip = ip;
		this.port = port;
		this.netSessionEventHandler = netSessionEventHandler;
	}
	
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	
	

	public void connect() 
	{
		parent.connect(this, ip, port, netSessionEventHandler);
    }
	
	public void resetConnect() {
		if(!isConnected()) {
			if(getChannel() != null) {
				getChannel().close();
			}
			connect();
		}
	}
	
	/**
	 * This method may cause memory leak if used uncarefully.
	 * One should call disableAutoReconnect before abandon this NetSession
	 * @param checkIntervalSec 自动重练间隔
	 * @param calback 自动重连回调（通常用来发送心跳）
	 */
	public void enableAutoReconnect(int checkIntervalSec, HeartBeatHandler calback)
	{
		disableAutoReconnect();
		
		autoReconnectFuture = getChannel().eventLoop().scheduleAtFixedRate(
				new HeartBeatTask(this, calback), 
				checkIntervalSec, 
				checkIntervalSec, 
				TimeUnit.SECONDS);
		
	}
	
	@Override
	public void closeChannel() {
		disableAutoReconnect();
		super.closeChannel();
	}
	public void disableAutoReconnect()
	{
		
		if (autoReconnectFuture != null)
		{
			autoReconnectFuture.cancel(false);
			try {
				autoReconnectFuture.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			autoReconnectFuture = null;
		}
		
	}

	@Override
	public Channel getChannel() {
		return channel;
	}
	
	public String getRemoteIp() {
		return ip;
	}
	
	public int getRemotePort() {
		return port;
	}
	

	@Override
	public String toString() {
		
		return '[' + getName() + ']' + channel.toString();
	}

}