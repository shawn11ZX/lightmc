package com.wooduan.lightmc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import com.wooduan.lightmc.ApcClientFactory.Builder;
import com.wooduan.lightmc.impl.AbstractApcServer;
import com.wooduan.lightmc.impl.ApcDispatcher;
import com.wooduan.lightmc.impl.ServerNetSession;
import com.wooduan.lightmc.netty.ApcDispatchHandler;
import com.wooduan.lightmc.netty.ChannelEventHandler;
import com.wooduan.lightmc.netty.ChannelEventLogger;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;
import com.wooduan.lightmc.statistics.DefaultApcStatisticRecorder;
import com.wooduan.lightmc.statistics.report.NettyEventExecutorGroupMonitor;

public class ApcServer extends AbstractApcServer   {
	public static Builder newBuilder() { return new Builder(); }
	
	public static class Builder {
		
		
		private ApcDispatcher dispatcher = new ApcDispatcher();
		private int ioThreadCount = 6;
		private EventExecutorGroup apcExecutorGroup;
		private ApcSerializer serializer;
		private NetSessionEventHandler sessionEventHandler;
		
		private boolean reliable = true;
		private boolean treatExceptionAsInfo = true;
		private ApcStatisticRecorder statisticRecorder = DefaultApcStatisticRecorder.INSTANCE;
		private String name = "unknown";
		private int readTimeout = 180;
		private int maxApcLength = Integer.MAX_VALUE;
		private Map<ChannelOption<?>, Object> options = new HashMap<ChannelOption<?>, Object>();
		public ApcServer build() {
			if (serializer == null)
				throw new IllegalArgumentException("serializer not initlized");
			
			if (sessionEventHandler == null)
				throw new IllegalArgumentException("sessionEventHandler not initlized");
			
			if (apcExecutorGroup == null)
				throw new IllegalArgumentException("apc thread not initialized");
			
			return new ApcServer(
					serializer, 
					apcExecutorGroup, 
					1, 
					ioThreadCount, 
					sessionEventHandler, 
					dispatcher,
					reliable,
					treatExceptionAsInfo,
					statisticRecorder,
					name,
					readTimeout,
					maxApcLength,
					options);
		}
		
		public <O extends T, T> Builder registerCallback(O instance, Class<T> interfaceClazz) {
			dispatcher.registerMethod(instance, interfaceClazz);
			return this;
		}
		
		public Builder setIoThreadCount(int count) {
			this.ioThreadCount = count;
			return this;
		}
		
		public Builder setApcThreadCount(int count) {
			this.apcExecutorGroup = new DefaultEventExecutorGroup(count);
			return this;
		}
		
		public Builder setApcExecutorGroup(EventExecutorGroup group) {
			this.apcExecutorGroup = group;
			return this;
		}
		
		public Builder setApcSerializer(ApcSerializer serializer) {
			this.serializer = serializer;
			return this;
		}
		
		public Builder setTreatExceptionAsInfo(boolean treatExceptionAsInfo) {
			this.treatExceptionAsInfo = treatExceptionAsInfo;
			return this;
		}
		
		public Builder setSessionEventHandler(
				NetSessionEventHandler sessionEventHandler) {
			this.sessionEventHandler = sessionEventHandler;
			return this;
		}
		
		public Builder setStatisticRecorder(ApcStatisticRecorder statisticRecorder) {
			this.statisticRecorder = statisticRecorder;
			return this;
		}
		public Builder setReliable(boolean reliable) {
			this.reliable = reliable;
			return this;
		}
		
		public Builder setName(String name) {
			this.name = name;
			return this;
		}
		
		public Builder setReadTimeout(int readTimeout) {
			this.readTimeout = readTimeout;
			return this;
		}
		
		public Builder setMaxApcLength(int maxApcLength) {
			this.maxApcLength = maxApcLength;
			return this;
		}
		
		public Builder setCallbackHook(CallbackHook hook) {
			this.dispatcher.setHook(hook);
			return this;
		} 
	
		public <T>  Builder  setChildOption(ChannelOption<T> option, T value)
		{
			options.put(option, value);
			return this;
		}
	}
	
	final private ApcSerializer apcSerializer;
	final private ApcDispatcher dispatcher;
	
	final private NetSessionEventHandler sessionEventHandler;
	final private int readTimeout;
	final private EventExecutorGroup apcExecutorGroup;
	final private boolean reliable;
	final private boolean treatExceptionAsInfo;
	final private ApcStatisticRecorder statisticRecorder;
	final private String name;
	final private int maxApcLength;
	final private Map<ChannelOption<?>, Object> options;
	private ApcServer(
			ApcSerializer codec, 
			EventExecutorGroup apcExecutorGroup, 
			int nettyBossCount, 
			int nettyWorkerCount,
			NetSessionEventHandler sessionEventHandler,
			ApcDispatcher dispatcher, 
			boolean reliable,
			boolean treatExceptionAsInfo,
			ApcStatisticRecorder statisticRecorder,
			String name,
			int readTimeout,
			int maxApcLength,
			Map<ChannelOption<?>, Object> options)
	{
		super(nettyBossCount, nettyWorkerCount);
		this.options = options;
		this.reliable = reliable;
		this.sessionEventHandler = sessionEventHandler;
		this.dispatcher = dispatcher;
		this.apcSerializer = codec;
		this.treatExceptionAsInfo = treatExceptionAsInfo;
		this.statisticRecorder = statisticRecorder;
		this.name = name;
		this.readTimeout = readTimeout;
		this.maxApcLength = maxApcLength;
		this.apcExecutorGroup = apcExecutorGroup;
		setChildOptions(getServerBootstrap());
	}
	
	protected void setChildOptions(ServerBootstrap s)
	{
		for (Map.Entry<ChannelOption<?>, Object> entry : options.entrySet()) {
			s.childOption((ChannelOption<Object>)entry.getKey(), entry.getValue());
		}
	}
	
	@Override
	public void start(int port) throws SocketException {
		super.start(port);
		NettyEventExecutorGroupMonitor.monitorApcServer(this);
	}
	
	@Override
	protected void initClientPipeline(SocketChannel ch, ChannelPipeline p) {
		assert sessionEventHandler != null;
		
		NetSession session = new ServerNetSession(ch, reliable, statisticRecorder);
		p.addLast(new ReadTimeoutHandler(readTimeout));
		apcSerializer.addPreHandler(ch.eventLoop(), p);
		
        apcSerializer.addApcHandler(ch.eventLoop(), p, statisticRecorder, maxApcLength);
        /**
		 * to maximize throughput, only inbound handler can be attached to apcExecutorGroup
		 */
        p.addLast(ch.eventLoop(), new ChannelEventLogger(session, treatExceptionAsInfo));
        p.addLast(apcExecutorGroup, new ApcDispatchHandler(session, dispatcher, statisticRecorder));
        p.addLast(apcExecutorGroup, new ChannelEventHandler(session, sessionEventHandler));
	}

	public String getName() {
		return name;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		apcExecutorGroup.shutdownGracefully();
		NettyEventExecutorGroupMonitor.unmonitorApcServer(this);
	}

	
	public EventExecutorGroup getApcExecutorGroup() {
		return apcExecutorGroup;
	}
	
}
