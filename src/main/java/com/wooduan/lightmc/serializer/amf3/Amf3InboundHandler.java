package com.wooduan.lightmc.serializer.amf3;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Input;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.APC;

public class Amf3InboundHandler extends ChannelInboundHandlerAdapter 
{
	private static final Logger Logger = LoggerFactory.getLogger(Amf3InboundHandler.class);
	
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception 
	{
		ByteBuf buffer = (ByteBuf)msg;
		Amf3Input input = new Amf3Input(new SerializationContext());
		
		try 
		{
					
			input.setInputStream(new ByteBufInputStream(buffer));
			APC apc = (APC)input.readObject();
			ctx.fireChannelRead(apc);			
		} 
		catch(Exception exc) 
		{
			Logger.error("Amf3RPCHandler - messageReceived", exc);
			ctx.fireExceptionCaught(exc);
		}
		finally
		{
			buffer.release();
			input.close();
		}
		
		
	};
}