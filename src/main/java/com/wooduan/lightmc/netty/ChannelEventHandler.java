package com.wooduan.lightmc.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import com.wooduan.lightmc.ApcHelper;
import com.wooduan.lightmc.NetSession;
import com.wooduan.lightmc.NetSessionEventHandler;




public class ChannelEventHandler extends ChannelInboundHandlerAdapter {

	private NetSessionEventHandler handler; 

	private NetSession session;
	public ChannelEventHandler(NetSession session, NetSessionEventHandler handler) {
		this.handler = handler;
		this.session = session;
		
		
	}
	
    
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception 
	{
		assert session.getChannel() == ctx.channel();
		ctx.fireChannelActive();
		
		if (handler != null)
		{
			ApcHelper.setCurrentNetSession(session);
    		handler.channelConnected(session);
    		ApcHelper.setCurrentNetSession(null);
		}
	};
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception 
	{
		assert session.getChannel() == ctx.channel();
		ctx.fireChannelInactive();
		
		if (handler != null)
		{
			ApcHelper.setCurrentNetSession(session);
    		handler.channelDisconnected(session);
    		ApcHelper.setCurrentNetSession(null);
		}
	};
    
	
	

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		
		assert session.getChannel() == ctx.channel();
		if (handler != null)
		{
			ApcHelper.setCurrentNetSession(session);
			handler.channelExceptionCaught(session, cause);
			ApcHelper.setCurrentNetSession(null);
		}
    		
		
		
	}
	
}
