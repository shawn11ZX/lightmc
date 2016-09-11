package com.wooduan.lightmc.serializer.amf3;

import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.Amf3Output;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import com.wooduan.lightmc.APC;

public class Amf3OutboundHandler extends ChannelOutboundHandlerAdapter 
{
	
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception 
	{
		ByteBuf byteBuf = ctx.alloc().buffer(4 * 1024);
		try {
			ByteBufOutputStream bout = new ByteBufOutputStream(byteBuf);
			SerializationContext serializationContext = new SerializationContext();
			serializationContext.legacyCollection = true;
			Amf3Output out = new Amf3Output(serializationContext);
			
			out.setOutputStream(bout);
			APC apc = (APC)msg;

			out.writeObject(apc);
			out.flush();
			out.close();
			
			ByteBuf encoded = bout.buffer();
	        ctx.writeAndFlush(encoded, promise);
	        byteBuf = null;
	        
	        
		} 
		catch (Exception e)
		{
			ctx.fireExceptionCaught(e);
		}
		finally {
			if (byteBuf != null)
			{
				byteBuf.release();
			}
		}
		
        
	}
}
