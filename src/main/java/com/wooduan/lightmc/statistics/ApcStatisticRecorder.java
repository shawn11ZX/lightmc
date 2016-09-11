package com.wooduan.lightmc.statistics;

import com.wooduan.lightmc.statistics.zipkin.TraceInfo;


public interface ApcStatisticRecorder {

	public void beginInboundDecoding();
	
	public void endInboundDecoding(TraceInfo apc, int length);
	
	public void beginInboundDispatching(TraceInfo apc);
	
	public void endInboundDispatching(TraceInfo apc, long durationNano);
	
	public void beginOutboundAsyncWriting(TraceInfo apc);
	
	public void endOutboundAsyncWriting();
	
	public void beginOutboundEncoding(TraceInfo apc);
	
	public void endOutboundEncoding(TraceInfo apc);
	
	public void beginOutboundWriting(TraceInfo apc, int length);
	
	public void endOutboundWriting(TraceInfo apc);

	public void channelActive();

	public void channelConnectRequested();


	public void channelConnectFail();


	public void channelConnectSucc();

	public int getDispatchQueueLength();

	public void channelInActive();

	public void channelException(String exceptionName);
	
}
