/*
 * (C) 2012-2013 Wooduan Group.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * Authors:
 * duty <huahuai009@163.com>
 */
package com.wooduan.lightmc.serializer.amf3;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *

 */
public class CompressOutputHandler extends ChannelOutboundHandlerAdapter 
{
	static public boolean flag = false;
	private static final Logger logger = LoggerFactory.getLogger(CompressOutputHandler.class);
	
	
	private int cachesize = 1024; 
	
	private Deflater deflater;
	byte[] deflatorInput;
	byte[] deflatorOutput = new byte[cachesize];
	static AtomicLong beforeCompress = new AtomicLong();
	static AtomicLong afterCompress = new AtomicLong();
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception
	{
		super.handlerAdded(ctx);
		
	}
	
	private void initDefaltor() {
		if (deflater == null)
		{
			deflater = new Deflater(Deflater.BEST_COMPRESSION);
		}
	}
	
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		
		super.handlerRemoved(ctx);
		
		if (deflater != null) {
			deflater.end();
			deflater = null;
        }
	}


	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception 
	{
		ByteBuf rawBuf = (ByteBuf) msg;
		ByteBuf zipBuf = ctx.alloc().buffer(4 * 1024);

		ByteBufOutputStream zipStream = new ByteBufOutputStream(zipBuf);
		
		try {
			if (flag == true && rawBuf.writerIndex() > 512) {
				initDefaltor();
				int len = rawBuf.writerIndex();
				beforeCompress.addAndGet(len);
				if (deflatorInput == null || deflatorInput.length < len)
				{
					deflatorInput = new byte[len];
				}
				rawBuf.readBytes(deflatorInput, 0, len);
				deflater.reset();
				deflater.setInput(deflatorInput, 0, len);
				deflater.finish();
				int got;
				zipStream.writeByte(1);
				while (!deflater.finished()) {
					got = deflater.deflate(deflatorOutput);
					zipStream.write(deflatorOutput, 0, got);
				}
				afterCompress.addAndGet(zipBuf.writerIndex());
				ctx.write(zipBuf, promise);
				zipBuf = null;
			}else{
				zipBuf.writeByte(0);
				zipBuf.writeBytes(rawBuf, rawBuf.writerIndex());
				ctx.write(zipBuf, promise);
				zipBuf = null;
			}
		} catch (Exception exc) {
			logger.error(exc.getMessage(), exc);
		} finally {
			if (zipBuf != null)
				zipBuf.release();
			zipStream.close();
			rawBuf.release();
			
		}
		
	}
	static public String getStatus(){
		StringBuilder sb =new StringBuilder();
		long defore = beforeCompress.get();
		long after = afterCompress.get();
		sb.append("Output----flag:");
		sb.append(flag);
		sb.append(",before:");
		sb.append(defore);
		sb.append(", after:");
		sb.append(after);
		if(defore!=0){
			sb.append(",ratio:");
			sb.append((double)after/defore);
		}
		return sb.toString();
	}
	
}	
