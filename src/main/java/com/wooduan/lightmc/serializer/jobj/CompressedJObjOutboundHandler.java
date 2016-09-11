package com.wooduan.lightmc.serializer.jobj;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.io.ObjectOutputStream;

import com.wooduan.lightmc.APC;

public class CompressedJObjOutboundHandler extends ChannelOutboundHandlerAdapter 
{
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception 
	{
		ByteBuf byteBuf = ctx.alloc().buffer(4 * 1024);
		try {
			
			ByteBufOutputStream bout = new ByteBufOutputStream(byteBuf);
			bout.write(new byte[4]); // reserved for the length
	        ObjectOutputStream oout = new HackedObjectOutputStream(bout);
	        APC apc = (APC)msg;
	        oout.writeObject(apc);
	        oout.flush();
	        oout.close();
	        
	        ByteBuf encoded = bout.buffer();
			int sysFlag = 0;
	        if (JObjCompressor.tryToCompressMessage(encoded, 4)) {
	            sysFlag |= MessageSysFlag.CompressedFlag;
	        }
			encoded.setInt(0, sysFlag);
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
