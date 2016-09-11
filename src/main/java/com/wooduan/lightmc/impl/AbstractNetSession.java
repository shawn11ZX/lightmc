package com.wooduan.lightmc.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.NetSession;
import com.wooduan.lightmc.proxy.ApcInvocationHandler;
import com.wooduan.lightmc.proxy.ApcRelayProxy;
import com.wooduan.lightmc.proxy.RelayInvocationHandler;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;

public abstract class AbstractNetSession implements NetSession, ChannelFutureListener{

	private static final Logger logger = LoggerFactory.getLogger(NetSession.class);
	
	final private long uid;
	final private ApcStatisticRecorder statisticRecorder;
	volatile private boolean reliable;
	volatile private String name = "unknown";
	
	
	private static AtomicLong gid = new AtomicLong(0);

	protected AbstractNetSession(boolean reliable, ApcStatisticRecorder statisticRecorder) {
		this.uid = gid.incrementAndGet();
		this.reliable = reliable;
		this.statisticRecorder = statisticRecorder;
	}
	protected AbstractNetSession(boolean reliable) {
		this(reliable, null);
	}
	
	
	@Override
	public void setReliable(boolean reliable) {
		this.reliable = reliable;
	}
	
	
	
	public boolean call(APC apc) {
		
		
		Channel channel = getChannel();
		try {
			
			if (!channel.isActive())
			{
				Exception exception = new Exception();
				if(reliable) {
					logger.error("channel " + channel.toString() +" is not connected while calling {}, stack={}", apc.getFunctionName(), exception.getStackTrace());
				}
				else {
					logger.info("channel " + channel.toString() +" is not connected while calling {}, stack={}", apc.getFunctionName(), exception.getStackTrace());
				}
				return false;
			}
			
			if(reliable) {
				return forceWrite(apc, channel);
			}
			else
			{
				return tryWrite(apc, channel);
			}
		} 
		catch (Exception e)
		{
			if(reliable) {
				logger.error("channel " + channel.toString() +" write error calling  {}, stack={}", apc.getFunctionName(), e.getStackTrace());
			}
			else {
				logger.info("channel " + channel.toString() +" write error calling {}, stack={}", apc.getFunctionName(), e.getStackTrace());
			}
			return false;
		}
	}


	private boolean tryWrite(APC apc, Channel channel) {
		String lastFunc = "";
		
		if (channel.isWritable()) {
			lastFunc = apc.getFunctionName();
			if (statisticRecorder != null)
			{
				statisticRecorder.beginOutboundAsyncWriting(apc);
			}
			ChannelFuture f = channel.writeAndFlush(apc);
			f.addListener(this);
			
			return true;
		} else {
			logger.warn("channel is not writable" + "[funcName:" + apc.getFunctionName() +"],lastFunc:" + lastFunc + ",isConnected:" + channel.isActive() + ",isOpen:" + channel.isOpen());
			return false;
		}
	}
	
	private boolean forceWrite(APC apc, Channel channel) {
		if (statisticRecorder != null)
		{
			statisticRecorder.beginOutboundAsyncWriting(apc);
		}
		ChannelFuture f = channel.writeAndFlush(apc);
		f.addListener(this);
		return true;
	}
	

	@Override
	public void operationComplete(ChannelFuture future) {
		if (statisticRecorder != null)
		{
			statisticRecorder.endOutboundAsyncWriting();
		}
	}
	
	@Override
	public long getUid() {
		return uid;
	}
	
	abstract public Channel getChannel();
	


	public boolean isConnected() {
		if(getChannel() == null) {
			return false;
		}
		if(getChannel().isActive() && getChannel().isOpen()) {
			return true;
		}
		return false;
	}
	/**
	 * 重置链接
	 */

	
	public void closeChannel() {
		if(getChannel() != null) {
			getChannel().close().awaitUninterruptibly();
		}
	}
	
	
	@Override
	public <T> T newOutputProxy(final Class<T> clazz)
	{
		
		@SuppressWarnings("unchecked")
		T f = (T) Proxy.newProxyInstance(clazz.getClassLoader(),
              new Class[] { clazz},
              new ApcInvocationHandler<T>(clazz, this));
		return f;
		
	}
	
	/* (non-Javadoc)
	 * @see com.wooduan.lightmc.NetSession#newRelayProxy(java.lang.Class)
	 */
	@Override
	public <T> T newRelayProxy(final Class<T> clazz)
	{
		
		try {
			RelayInvocationHandler<T> handler = new RelayInvocationHandler<T>(clazz, this);
			@SuppressWarnings("unchecked")
			T f = (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                  new Class[] { clazz, ApcRelayProxy.class },
                  handler);
			return f;
		} 
		catch(Exception e)
		{
			logger.error("error", e);
		}
		return null;
	}
	
	@Override
	public String getRemoteAddr() {
		Channel clientChannel = getChannel();
		if(clientChannel != null) {
			SocketAddress address = clientChannel.remoteAddress();
			if (address == null) {
	            return null;
	        }
	        if (address instanceof InetSocketAddress) {
	            final InetSocketAddress socketAddr = (InetSocketAddress) address;
	            final InetAddress inetAddr = socketAddr.getAddress();
	            return (inetAddr != null ? inetAddr.getHostAddress() : socketAddr.getHostName());
	        }
		}
		return null;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
