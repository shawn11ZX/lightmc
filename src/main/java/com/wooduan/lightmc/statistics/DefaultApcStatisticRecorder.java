package com.wooduan.lightmc.statistics;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.statistics.zipkin.TraceInfo;



public class DefaultApcStatisticRecorder implements ApcStatisticRecorder {
	private static final Logger Logger = LoggerFactory.getLogger(DefaultApcStatisticRecorder.class);
	public static DefaultApcStatisticRecorder INSTANCE = new DefaultApcStatisticRecorder();
	private ThreadLocal<Long> beginDecodingTsMicro = new ThreadLocal<Long>();
	private AtomicInteger dispatchWaitingQueueSize = new AtomicInteger();
	private AtomicInteger dispatchWorkingListSize = new AtomicInteger();
	private AtomicInteger outputWaitingQueueSize = new AtomicInteger();
	private AtomicInteger outputWorkingListSize = new AtomicInteger();
	
	
	@Override
	public void beginInboundDecoding()
	{
		beginDecodingTsMicro.set(MicroTime.currentTimeMicros());
	}
	
	@Override
	public void endInboundDecoding(TraceInfo apc, int length) {
		dispatchWaitingQueueSize.incrementAndGet();
		//Logger.info("decoding {}: {}", apc.getName(), dispatchWaitingQueueSize.get());
		
		Long ts = beginDecodingTsMicro.get();
		apc.beginSpan(ts);
		apc.endSpan("decode");
		
		
	}

	@Override
	public void beginInboundDispatching(TraceInfo apc) {
		dispatchWaitingQueueSize.decrementAndGet();
		dispatchWorkingListSize.incrementAndGet();
		
		//Logger.info("dispatching {} {}", apc.getName(), dispatchWaitingQueueSize.get());
		
		apc.beginSpan();
		apc.saveThreadState();
		
		
	}

	@Override
	public void endInboundDispatching(TraceInfo apc, long nanoS) {
		dispatchWorkingListSize.decrementAndGet();
		
		apc.endSpan("dispatch");
		
	}

	@Override
	public void beginOutboundAsyncWriting(TraceInfo apc) {
		outputWaitingQueueSize.incrementAndGet();
		apc.beginSpan();
		
	}
	
	@Override
	public void endOutboundAsyncWriting() {
		outputWaitingQueueSize.decrementAndGet();
	}

	@Override
	public void beginOutboundEncoding(TraceInfo apc) {
		
		outputWorkingListSize.incrementAndGet();
		apc.endSpan("call");
		
		apc.beginSpan();
	}

	@Override
	public void endOutboundEncoding(TraceInfo apc) {
		outputWorkingListSize.decrementAndGet();
		apc.endSpan("encode");
	}

	@Override
	public void beginOutboundWriting(TraceInfo apc, int length) {
		apc.beginSpan();
	}
	
	@Override
	public void endOutboundWriting(TraceInfo apc) {
		apc.endSpan("send");
		
	}


	public int getDispatchQueueLength()
	{
		return dispatchWaitingQueueSize.get();
	}

	@Override
	public void channelActive() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void channelConnectRequested() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void channelConnectFail() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void channelConnectSucc() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void channelInActive() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void channelException(String exceptionName) {
		// TODO Auto-generated method stub
		
	}


}
