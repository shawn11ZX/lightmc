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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.StringUtil;

import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Inflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *

 */
public class DecompressInputHandler extends ChannelInboundHandlerAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(DecompressInputHandler.class);

	private int cachesize = 1024;
	private static int MAX_DEFLATE_COUNT = 50;
	private static int printed = 0;
	private Inflater inflater;
	byte[] inflaterInput;
	byte[] inflaterOutput = new byte[cachesize];
	static AtomicLong beforeCompress = new AtomicLong();
	static AtomicLong afterCompress = new AtomicLong();
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception
	{
		super.handlerAdded(ctx);
		
	}
	
	private void initInfaltor() {
		if (inflater == null)
		{
			inflater = new Inflater();
		}
	}
	
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		
		super.handlerRemoved(ctx);
		
		if (inflater != null) {
			inflater.end();
			inflater = null;
        }
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		ByteBuf zippedByteBuf = (ByteBuf) msg;
		ByteBuf rawByteBuf = null;
		ByteBufOutputStream outputStream = null;
		boolean flag = false;
		try {
			int rIndex = zippedByteBuf.readerIndex();
			byte t = zippedByteBuf.readByte();
			int len = zippedByteBuf.writerIndex();
			
			flag = t == 1 ? true : false;
			if (flag) {
				initInfaltor();
				afterCompress.addAndGet(len);
				if (inflaterInput == null || inflaterInput.length < len -1)
					inflaterInput = new byte[len-1];
				rawByteBuf = ctx.alloc().buffer(4 * 1024);

				outputStream = new ByteBufOutputStream(rawByteBuf);
				zippedByteBuf.readBytes(inflaterInput, 0, len- 1);
				inflater.reset();
				inflater.setInput(inflaterInput, 0, len - 1);

				int got;
				int count = 0;
				while (!inflater.finished() && count++ < MAX_DEFLATE_COUNT) {
					got = inflater.inflate(inflaterOutput);
					if (got == 0)
						break;
					outputStream.write(inflaterOutput, 0, got);
					beforeCompress.addAndGet(got);
				}
				if (count < MAX_DEFLATE_COUNT)
				{
					ctx.fireChannelRead(rawByteBuf);
					rawByteBuf = null;
				}
				else 
				{
					if (printed++ < 5)
					{
						zippedByteBuf.readerIndex(rIndex);
						logger.error("unexpected inflator: {}", formatByteBuf(ctx, "LOG", zippedByteBuf));
					}
				}
			}else{
				ctx.fireChannelRead(zippedByteBuf);
				zippedByteBuf = null;
			}
			
		} catch (Exception exc) {
			
			logger.error(exc.getMessage(), exc);
		} finally {
			if (rawByteBuf != null)
				rawByteBuf.release();
			if(outputStream != null)
				outputStream.close();
			if(zippedByteBuf != null)
				zippedByteBuf.release();
		}
	}
	
	 private static String formatByteBuf(ChannelHandlerContext ctx, String eventName, ByteBuf msg) {
	        String chStr = ctx.channel().toString();
	        int length = msg.readableBytes();
	        if (length == 0) {
	            StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 4);
	            buf.append(chStr).append(' ').append(eventName).append(": 0B");
	            return buf.toString();
	        } else {
	            int rows = length / 16 + (length % 15 == 0? 0 : 1) + 4;
	            StringBuilder buf = new StringBuilder(chStr.length() + 1 + eventName.length() + 2 + 10 + 1 + 2 + rows * 80);

	            buf.append(chStr).append(' ').append(eventName).append(": ").append(length).append('B').append(StringUtil.NEWLINE);
	            ByteBufUtil.appendPrettyHexDump(buf, msg);

	            return buf.toString();
	        }
	    }
	static public String getStatus(){
		StringBuilder sb =new StringBuilder();
		long defore = beforeCompress.get();
		long after = afterCompress.get();
		sb.append("Input----before:");
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
