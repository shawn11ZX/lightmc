package com.wooduan.lightmc.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;

public class EndApcStatisticHandler extends ChannelDuplexHandler 
{
	final ApcStatisticRecorder statisticRecord;
	public EndApcStatisticHandler(ApcStatisticRecorder statisticRecord)
	{
		this.statisticRecord = statisticRecord;
	}
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception 
	{
		APC apc = (APC)msg;
		
		if (apc != null)
		{
			ctx.attr(ChannelAttributes.CURRENT_WRITE_APC).set(apc);
		}
		
		try {
			statisticRecord.beginOutboundEncoding(apc);
			super.write(ctx, msg, promise);
		} finally {
			statisticRecord.endOutboundEncoding(apc);
		}
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		
		APC apc = (APC)msg;
		
		if (apc != null)
		{
			ctx.attr(ChannelAttributes.CURRENT_READ_APC).set(apc);
			ctx.fireChannelRead(apc);
		}
		
	}
	
	
}
