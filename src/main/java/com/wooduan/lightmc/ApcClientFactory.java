package com.wooduan.lightmc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.wooduan.lightmc.impl.ApcDispatcher;
import com.wooduan.lightmc.impl.ClientNetSession;
import com.wooduan.lightmc.netty.ApcDispatchHandler;
import com.wooduan.lightmc.netty.ChannelEventHandler;
import com.wooduan.lightmc.netty.ChannelEventLogger;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;
import com.wooduan.lightmc.statistics.DefaultApcStatisticRecorder;
import com.wooduan.lightmc.statistics.report.NettyEventExecutorGroupMonitor;

public class ApcClientFactory  {
	private final static NioEventLoopGroup defaulNioEventLoopGroup = new NioEventLoopGroup(6);
	
	public static Builder newBuilder() { return new Builder(); }
	
	public static class Builder {
		private ApcDispatcher dispatcher = new ApcDispatcher();
		private ApcSerializer serializer;
		private NioEventLoopGroup nioEventLoopGroup = defaulNioEventLoopGroup;
		private boolean reliable = true;
		private boolean treatExceptionAsInfo = true;
		private ApcStatisticRecorder statisticRecorder = DefaultApcStatisticRecorder.INSTANCE;
		private String name = "unknown";
		private int readTimeout = 60*60;
		private int maxApcLength = Integer.MAX_VALUE;
		private Map<ChannelOption<?>, Object> options = new HashMap<ChannelOption<?>, Object>();
		private EventExecutorGroup apcExecutorGroup;
		public ApcClientFactory build() {
			if (serializer == null)
				throw new IllegalArgumentException("serializer not initialized");
			
			if (apcExecutorGroup == null)
				throw new IllegalArgumentException("apc thread not initialized");
			ApcClientFactory rc = new ApcClientFactory(
					serializer, 
					apcExecutorGroup, 
					dispatcher,
					reliable,
					nioEventLoopGroup,
					treatExceptionAsInfo,
					statisticRecorder,
					name,
					readTimeout,
					maxApcLength,
					options);
			NettyEventExecutorGroupMonitor.monitorApcClientFactory(rc);
			return rc;
		}
		
		public void setIoThreadCount(int count) {
			this.nioEventLoopGroup = new NioEventLoopGroup(count);;
		}
		
		public <O extends T, T> Builder registerCallback(O instance, Class<T> interfaceClazz) {
			dispatcher.registerMethod(instance, interfaceClazz);
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
		
		public Builder setStatisticRecorder(ApcStatisticRecorder statisticRecorder) {
			this.statisticRecorder = statisticRecorder;
			return this;
		}
		
		public ApcStatisticRecorder getStatisticRecorder() {
			return statisticRecorder;
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
	
		public <T>  Builder  setOption(ChannelOption<T> option, T value)
		{
			options.put(option, value);
			return this;
		}
		
		public Builder addIgnoredFunction(String functionName) {
			this.dispatcher.ignoreFunction(functionName);
			return this;
		}  
	}
	
	static NetSessionEventHandler defaultHandler = new NetSessionEventHandler() {
		@Override
		public void channelExceptionCaught(NetSession session, Throwable cause) {
		}
		
		@Override
		public void channelDisconnected(NetSession session) {
		}
		
		@Override
		public void channelConnected(NetSession session) {
		}
		
		@Override
		public void channelConnectFailed(NetSession session) {
		}
	};
	
	final private ApcSerializer apcSerializer;
	final private ApcDispatcher dispatcher;
	
	final private int readTimeout; // 1h
	final private EventExecutorGroup apcExecutorGroup;
	final private boolean reliable;
	final private NioEventLoopGroup nioEventLoopGroup;
	final private boolean treatExceptionAsInfo;
	final private ApcStatisticRecorder statisticRecorder;
	final private String name;
	final private int maxApcLength;
	final private Map<ChannelOption<?>, Object> options;
	private ApcClientFactory(
			ApcSerializer codec, 
			EventExecutorGroup apcExecutorGroup, 
			ApcDispatcher dispatcher, 
			boolean reliable,
			NioEventLoopGroup nioEventLoopGroup,
			boolean treatExceptionAsInfo,
			ApcStatisticRecorder statisticRecorder,
			String name,
			int readTimeout,
			int maxApcLength,
			Map<ChannelOption<?>, Object> options)
	{
		this.readTimeout = readTimeout;
		this.reliable = reliable;
		this.dispatcher = dispatcher;
		this.apcSerializer = codec;
		this.nioEventLoopGroup = nioEventLoopGroup;
		this.treatExceptionAsInfo = treatExceptionAsInfo;
		this.statisticRecorder  = statisticRecorder;
		this.name = name;
		this.maxApcLength = maxApcLength;
		this.options = options;
		this.apcExecutorGroup = apcExecutorGroup;
	}

	public ClientNetSession connect(String ip, int port) {
		return connect(ip, port, defaultHandler);
	}
	
	public ClientNetSession connect(String ip, int port, NetSessionEventHandler sessionEventHandler) {
		return connect("unknown", ip, port, sessionEventHandler);
	}
	
	public ApcStatisticRecorder getStatisticRecorder() {
		return statisticRecorder;
	}
	
	
	public ClientNetSession connect(ClientNetSession session, String ip, int port, 
			NetSessionEventHandler sessionEventHandler) 
	{
		Bootstrap clientBootstrap = new Bootstrap();
		clientBootstrap.group(nioEventLoopGroup)
		               .channel(NioSocketChannel.class)
		               //.resolver(resolverGroup)
		               .handler(getChannelInitializer(session, ip, port, sessionEventHandler));
		
		for (Map.Entry<ChannelOption<?>, Object> entry : options.entrySet()) {
			clientBootstrap.option((ChannelOption<Object>)entry.getKey(), entry.getValue());
		}
		
		ChannelFuture future = clientBootstrap.connect(new InetSocketAddress(ip, port));
        Channel ch = future.channel();
        session.setChannel(ch);
        
        return session; 
    }
	
	
	public ClientNetSession connect(String name, String ip, int port, 
			NetSessionEventHandler sessionEventHandler) 
	{
		ClientNetSession session = new ClientNetSession(name, this, reliable, ip, port, sessionEventHandler, statisticRecorder);
		
        return connect( session, ip, port, sessionEventHandler); 
    }
	
	protected ChannelInitializer<SocketChannel> getChannelInitializer(
			final ClientNetSession session,
			final String ip, 
			final int port, 
			final NetSessionEventHandler sessionEventHandler) {
		return new ChannelInitializer<SocketChannel>() 
		   {
				@Override
				protected void initChannel(SocketChannel ch) throws Exception 
				{
					ChannelPipeline p = ch.pipeline();
					
					session.setChannel(ch);
					
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
		   };
	}
	
	public void shutdown() {
		apcExecutorGroup.shutdownGracefully();
		if (nioEventLoopGroup != defaulNioEventLoopGroup)
		{
			nioEventLoopGroup.shutdownGracefully();
		}
		
		NettyEventExecutorGroupMonitor.unmonitorApcClientFactory(this);
	}
	
	public EventExecutorGroup getApcExecutorGroup() {
		return apcExecutorGroup;
	}
	
	public EventExecutorGroup getIoExecutorGroup() {
		return nioEventLoopGroup;
	}
	
	public String getName() {
		return name;
	}
}
