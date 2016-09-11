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
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.ObjectInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.APC;

/**
 *

 */
public class JObjInboundHandler extends ChannelInboundHandlerAdapter 
{
	
	private static final Logger logger = LoggerFactory.getLogger(JObjInboundHandler.class);
	
	
	public JObjInboundHandler() 
	{
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception 
	{
		ByteBuf buffer = (ByteBuf)msg;
		ObjectInputStream input = null;
		
		try 
		{
			input = new HackedObjectInputStream(new ByteBufInputStream(buffer));
			APC rpc = (APC) input.readObject();
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
			if (input != null)
				input.close();
		}
	}
	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception 
	{
		ctx.fireExceptionCaught(cause);
		ctx.channel().close();
	}	
}	
