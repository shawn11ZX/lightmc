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
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.wooduan.lightmc.impl.ApcDispatcher;
import com.wooduan.lightmc.impl.ClientNetSession;
import com.wooduan.lightmc.netty.ApcDispatchHandler;
import com.wooduan.lightmc.netty.ChannelEventHandler;
import com.wooduan.lightmc.netty.ChannelEventLogger;
import com.wooduan.lightmc.netty.TimedEventExecutorGroup;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;
import com.wooduan.lightmc.statistics.DefaultApcStatisticRecorder;
import com.wooduan.lightmc.statistics.report.EventExecutorGroupItem;
import com.wooduan.lightmc.statistics.report.EventExecutorGroupItems;

public class ApcClientFactory  {
	private final static NioEventLoopGroup defaulNioEventLoopGroup = new NioEventLoopGroup(6);
	
	public static Builder newBuilder() { return new Builder(); }
	
	public static class Builder {
		private ApcDispatcher dispatcher = new ApcDispatcher();
		private ApcSerializer serializer;
		private NioEventLoopGroup nioEventLoopGroup = defaulNioEventLoopGroup;
		private boolean reliable = true;
		private boolean treatExceptionAsInfo = false;
		private ApcStatisticRecorder statisticRecorder = DefaultApcStatisticRecorder.INSTANCE;
		private String name = "unknown";
		private int readTimeout = 0;
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
			EventExecutorGroupItems.monitorApcClientFactory(rc);
			return rc;
		}
		
		/**
		 * 设置io线程个数，io线程用于网络收发报文，报文编解码
		 * @param count io线程个数
		 * @return Builder
		 */
		public Builder setIoThreadCount(int count) {
			this.nioEventLoopGroup = new NioEventLoopGroup(count);
			return this;
		}
		
		/**
		 * 注册回调函数
		 * @param instance 实现了interfaceClazz实例
		 * @param interfaceClazz 需要被注册的方法集合
		 * @param <T> 方法集合接口类型
		 * @param <O> 实例接口类型
		 * @return Builder
		 */
		public <O extends T, T> Builder registerCallback(O instance, Class<T> interfaceClazz) {
			dispatcher.registerMethod(instance, interfaceClazz);
			return this;
		}
		
		
		/**
		 * 设置APC处理线程个数，APC处理线程用来处理APC报文并调用回调函数
		 * 该函数与setApcExecutorGroup只能有一个被调用
		 * @param count APC处理线程个数
		 * @return Builder
		 */
		public Builder setApcThreadCount(int count) {
			this.apcExecutorGroup = new TimedEventExecutorGroup(count);
			return this;
		}
		
		
		/**
		 * 设置APC处理线程池，APC处理线程用来处理APC报文并调用回调函数
		 * 该函数与setApcExecutorGroup只能有一个被调用
		 * @param group APC处理线程池
		 * @return Builder
		 */
		public Builder setApcExecutorGroup(EventExecutorGroup group) {
			this.apcExecutorGroup = group;
			return this;
		}

		/**
		 * 设置APC的序列号逻辑
		 * @param serializer  serializer
		 * @return Builder
		 */
		public Builder setApcSerializer(ApcSerializer serializer) {
			this.serializer = serializer;
			return this;
		}
		
		/**
		 * 设置时间偶讲异常打印成INFO，默认为false（即ERROR)
		 * @param treatExceptionAsInfo treatExceptionAsInfo
		 * @return Builder
		 */
		public Builder setTreatExceptionAsInfo(boolean treatExceptionAsInfo) {
			this.treatExceptionAsInfo = treatExceptionAsInfo;
			return this;
		}
		
		public Builder setStatisticRecorder(ApcStatisticRecorder statisticRecorder) {
			this.statisticRecorder = statisticRecorder;
			return this;
		}
		
		
		/**
		 * 设置是否在netty Channel writable为false时，继续写入。
		 * 当为true时，如果netty一直发送不出去，会导致内存增长
		 * @param reliable  是否在netty Channel writable为false时，继续写入
		 * @return Builder
		 */
		public Builder setReliable(boolean reliable) {
			this.reliable = reliable;
			return this;
		}

		public ApcStatisticRecorder getStatisticRecorder() {
			return statisticRecorder;
		}
		
		public Builder setName(String name) {
			this.name = name;
			return this;
		}
		
		/**
		 * 设置idle超时时间（秒），超过这个时间会断开连接。0表示不超时
		 * @param readTimeout 超时时间（秒）
		 * @return Builder
		 */
		public Builder setReadTimeout(int readTimeout) {
			this.readTimeout = readTimeout;
			return this;
		}
	
		/**
		 * 设置序列号后最大APC长度
		 * @param maxApcLength maxApcLength
		 * @return Builder
		 */
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
		
		/**
		 * 用于测试，忽略某些函数
		 * @param functionName 忽略某些函数名
		 * @return Builder
		 */
		public Builder addIgnoredFunction(String functionName) {
			this.dispatcher.ignoreFunction(functionName);
			return this;
		}  
		
		/**
		 * 设置调试状态，该状态会打印调用信息
		 * @param debug 是否打印
		 * @return
		 */
		public Builder enableDebug(boolean debug) {
			this.dispatcher.setDebug(debug);
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
	final private EventExecutorGroupItem ioEventExecutorGroupItem;
	final private EventExecutorGroupItem apcEventExecutorGroupItem;
	
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
		this.ioEventExecutorGroupItem = new EventExecutorGroupItem(nioEventLoopGroup);
		this.apcEventExecutorGroupItem = new EventExecutorGroupItem(apcExecutorGroup);
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
					
					if (readTimeout != 0)
					{
						p.addLast(new ReadTimeoutHandler(readTimeout));
					}
					
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
		
		EventExecutorGroupItems.unmonitorApcClientFactory(this);
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
	
	public EventExecutorGroupItem getApcEventExecutorGroupItem() {
		return apcEventExecutorGroupItem;
	}
	
	public EventExecutorGroupItem getIoEventExecutorGroupItem() {
		return ioEventExecutorGroupItem;
	}
}
