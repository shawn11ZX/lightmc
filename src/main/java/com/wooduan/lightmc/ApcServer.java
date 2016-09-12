package com.wooduan.lightmc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import com.wooduan.lightmc.impl.AbstractApcServer;
import com.wooduan.lightmc.impl.ApcDispatcher;
import com.wooduan.lightmc.impl.ServerNetSession;
import com.wooduan.lightmc.netty.ApcDispatchHandler;
import com.wooduan.lightmc.netty.ChannelEventHandler;
import com.wooduan.lightmc.netty.ChannelEventLogger;
import com.wooduan.lightmc.netty.TimedEventExecutorGroup;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;
import com.wooduan.lightmc.statistics.DefaultApcStatisticRecorder;
import com.wooduan.lightmc.statistics.report.EventExecutorGroupItem;
import com.wooduan.lightmc.statistics.report.EventExecutorGroupItems;

public class ApcServer extends AbstractApcServer   {
	public static Builder newBuilder() { return new Builder(); }
	
	public static class Builder {
		
		
		private ApcDispatcher dispatcher = new ApcDispatcher();
		private int ioThreadCount = 6;
		private EventExecutorGroup apcExecutorGroup;
		private ApcSerializer serializer;
		private NetSessionEventHandler sessionEventHandler;
		
		private boolean reliable = true;
		private boolean treatExceptionAsInfo = false;
		private ApcStatisticRecorder statisticRecorder = DefaultApcStatisticRecorder.INSTANCE;
		private String name = "unknown";
		private int readTimeout = 0;
		private int maxApcLength = Integer.MAX_VALUE;
		private Map<ChannelOption<?>, Object> options = new HashMap<ChannelOption<?>, Object>();
		
		/**
		 * 构造并返回ApcServer
		 * @return ApcServer
		 */
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
		 * 设置io线程个数，io线程用于网络收发报文，报文编解码
		 * @param count io线程个数
		 * @return Builder
		 */
		public Builder setIoThreadCount(int count) {
			this.ioThreadCount = count;
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
		
		/**
		 * 设置连接状态处理回调对象
		 * @param sessionEventHandler 连接状态处理回调对象
		 * @return Builder
		 */
		public Builder setSessionEventHandler(
				NetSessionEventHandler sessionEventHandler) {
			this.sessionEventHandler = sessionEventHandler;
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
		
		/**
		 * 设置在处理每个APC时的Hook
		 * @param hook  CallbackHook
		 * @return Builder
		 */
		public Builder setCallbackHook(CallbackHook hook) {
			this.dispatcher.setHook(hook);
			return this;
		} 
	
		public <T>  Builder  setChildOption(ChannelOption<T> option, T value)
		{
			options.put(option, value);
			return this;
		}
		
		/**
		 * 设置调试状态，该状态会打印调用信息
		 * @param debug 是否打印
		 * @return Builder Builder
		 */
		public Builder enableDebug(boolean debug) {
			this.dispatcher.setDebug(debug);
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
	final private EventExecutorGroupItem listenEventExecutorGroupItem;
	final private EventExecutorGroupItem ioEventExecutorGroupItem;
	final private EventExecutorGroupItem apcEventExecutorGroupItem;
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
		this.listenEventExecutorGroupItem = new EventExecutorGroupItem(getBossGroup());
		this.ioEventExecutorGroupItem = new EventExecutorGroupItem(getWorkerGroup());
		this.apcEventExecutorGroupItem = new EventExecutorGroupItem(apcExecutorGroup);
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
		EventExecutorGroupItems.monitorApcServer(this);
	}
	
	@Override
	protected void initClientPipeline(SocketChannel ch, ChannelPipeline p) {
		assert sessionEventHandler != null;
		
		NetSession session = new ServerNetSession(ch, reliable, statisticRecorder);
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

	public String getName() {
		return name;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		apcExecutorGroup.shutdownGracefully();
		EventExecutorGroupItems.unmonitorApcServer(this);
	}

	
	public EventExecutorGroup getApcExecutorGroup() {
		return apcExecutorGroup;
	}
	
	public EventExecutorGroupItem getIoEventExecutorGroupItem() {
		return ioEventExecutorGroupItem;
	}
	
	public EventExecutorGroupItem getApcEventExecutorGroupItem() {
		return apcEventExecutorGroupItem;
	}
	
	public EventExecutorGroupItem getListenEventExecutorGroupItem() {
		return listenEventExecutorGroupItem;
	}
}
