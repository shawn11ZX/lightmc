package com.wooduan.lightmc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttackFilterHandler extends ChannelInboundHandlerAdapter 
{

	private static final Logger logger = LoggerFactory.getLogger(AttackFilterHandler.class);
	private int messageCount = 0;
	private int intervalMillis;
	private int maxMessageCount;
	private long currentTimeMillis;
	private volatile boolean isClosed = false;
	
	public AttackFilterHandler(int intervalSeconds, int maxMessageCount) {
		this.intervalMillis = intervalSeconds * 1000;
		this.maxMessageCount = maxMessageCount;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception 
	{
		if(!isClosed)
		{
			messageCount++;
			
			if(messageCount > maxMessageCount)
			{
				isClosed = true;
				ctx.channel().close();
				logger.error(String.format("%d messages are received in %d seconds from %s", messageCount, intervalMillis / 1000, ctx.channel().remoteAddress().toString()));
				return;
			}
			
			if((System.currentTimeMillis() - currentTimeMillis) > intervalMillis)
			{
				messageCount = 0;
				currentTimeMillis = System.currentTimeMillis();
			}
			
			ctx.fireChannelRead(msg);
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		currentTimeMillis = System.currentTimeMillis();
		
		ctx.fireChannelActive();
	}
}
