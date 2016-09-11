package com.wooduan.lightmc.netty;

import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;

public class BeginApcStatisticHandler extends ChannelDuplexHandler 
{
	final ApcStatisticRecorder statisticRecord;
	public BeginApcStatisticHandler(ApcStatisticRecorder statisticRecord)
	{
		this.statisticRecord = statisticRecord;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.statisticRecord.channelActive();
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		this.statisticRecord.channelInActive();
		super.channelInactive(ctx);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		this.statisticRecord.channelException(cause.getClass().getSimpleName());
		super.exceptionCaught(ctx, cause);
	}
	@Override
	public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
			SocketAddress localAddress, ChannelPromise promise) throws Exception {
		this.statisticRecord.channelConnectRequested();
    	promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                	statisticRecord.channelConnectFail();
                }
                else {
                	statisticRecord.channelConnectSucc();
                }
            }
        });
        ctx.connect(remoteAddress, localAddress, promise);
	}
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception 
	{
		int length = 0;
		try {
			statisticRecord.beginInboundDecoding();
			ByteBuf buffer = (ByteBuf)msg;
			length = buffer.readableBytes();
			super.channelRead(ctx, msg);
		} finally {
			APC apc = ctx.attr(ChannelAttributes.CURRENT_READ_APC).get();
			statisticRecord.endInboundDecoding(apc, length);
		}
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception 
	{
		ByteBuf byteBuf = (ByteBuf)msg;
		APC apc = ctx.attr(ChannelAttributes.CURRENT_WRITE_APC).get();
		
		statisticRecord.beginOutboundWriting(apc, byteBuf.readableBytes());
		promise.addListener(new WriteChannelFutureListener(apc));
		ctx.write(msg, promise);
	}
	
	public class WriteChannelFutureListener implements ChannelFutureListener {
		APC apc;

		public WriteChannelFutureListener(APC apc) {
			this.apc = apc;
		}

		@Override
		public void operationComplete(ChannelFuture future) {
			statisticRecord.endOutboundWriting(apc);
		}
	}
}
