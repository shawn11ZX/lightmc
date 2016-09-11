/*
 * (C) 2012-2013 Wooduan Group.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * Authors:
 * duty <huahuai009@163.com>
 */
package com.wooduan.lightmc.serializer.jobj;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.APC;

/**
 *

 */
public class CompressedJObjInboundHandler extends ChannelInboundHandlerAdapter 
{
	private static final Logger logger = LoggerFactory.getLogger(CompressedJObjInboundHandler.class);
	
	
	public CompressedJObjInboundHandler() {
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception 
	{
		ByteBuf buffer = (ByteBuf)msg;
		
		
		try 
		{
			APC rpc = JObjCompressor.internalDecode(buffer);
			ctx.fireChannelRead(rpc);
		} 
		catch(Exception exc) 
		{
			logger.error(exc.getMessage(), exc);
			ctx.fireExceptionCaught(exc);
		}
		finally
		{
			buffer.release();
		}
    }
	

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception 
	{
		ctx.channel().close();	
		ctx.fireExceptionCaught(cause);
	}
}
