package com.wooduan.lightmc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.NetSession;
import com.wooduan.lightmc.impl.ApcDispatcher;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;

public class ApcDispatchHandler extends ChannelInboundHandlerAdapter 
{
	final ApcDispatcher dispather;
	final ApcStatisticRecorder statisticRecorder;
	final NetSession session;
	public ApcDispatchHandler(NetSession session, ApcDispatcher dispather, ApcStatisticRecorder statisticRecorder)
	{
		this.session = session;
		this.dispather = dispather;
		this.statisticRecorder = statisticRecorder;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception 
	{
		long start=0,end=0;
		APC apc = (APC)msg;
		try {
			statisticRecorder.beginInboundDispatching(apc);
			start = System.nanoTime();
			apc.setNetsession(session);
			dispather.call(session, apc);
			
			
		} 
		catch (Exception e)
		{
			ctx.fireExceptionCaught(e);
		}
		finally {
        	end = System.nanoTime();
        	statisticRecorder.endInboundDispatching(apc, end - start);
     
		}
	};
	
	
	
}