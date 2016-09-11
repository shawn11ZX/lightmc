package com.wooduan.lightmc.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.NetSession;




public class ChannelEventLogger extends ChannelDuplexHandler {

	private static final Logger logger = LoggerFactory.getLogger(ChannelEventLogger.class);
	final private NetSession session;
	final private boolean treatExceptionAsInfo;
	
	private SocketAddress remoteAddr;
	private boolean activeClose = false;
	public ChannelEventLogger(NetSession session) {
		this(session, false);
	}
	
	
	public ChannelEventLogger(NetSession session, boolean treatExceptionAsInfo) {
		this.session = session;
		this.treatExceptionAsInfo = treatExceptionAsInfo;
	}
	
	private String getInfo()
	{
		return "[" + session.getName() + "]";
	}
	
	
	
	private void logException(Channel channel, Throwable cause)
	{
		if (channel.isActive())
		{
			String msg = "exception happened in connected channel: info=" + getInfo()
					+ ", address=" + channel.toString() 
					+ "(" + channel.id() + ")"  
					+ ", message=" + cause.getMessage();
			
			if (treatExceptionAsInfo)
			{
				logger.info(msg, cause);
			}
			else
			{
				logger.error(msg, cause);
			}
		}
		else
		{
			String msg = "exception happened in closed channel: info=" + getInfo()
					+ ", attempt=" + remoteAddr
					+ "(" + channel.id() + ")"
					+ ", message=" + cause.getMessage();
			
			if (treatExceptionAsInfo)
			{
				logger.info(msg, cause);
			}
			else
			{
				logger.error(msg, cause);
			}
		}
		
	}
	
	

    
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception 
	{
		this.remoteAddr = ctx.channel().remoteAddress();
		
		logger.info("connected to {}, info={}, address={}({}), recvBufSize={}, sendBuf={}", 
				remoteAddr
				, getInfo()
				, ctx.channel().toString()
				, ctx.channel().id()
				, ctx.channel().config().getOption(ChannelOption.SO_RCVBUF)
				, ctx.channel().config().getOption(ChannelOption.SO_SNDBUF));
		
		ctx.fireChannelActive();
		
	};
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception 
	{
		if (this.treatExceptionAsInfo || activeClose)
			logger.info("channel disconnected: info={}, address={}({})", getInfo(), ctx.channel().toString(), ctx.channel().id());
		else
			logger.info("channel disconnected: info={}, address={}({})", getInfo(), ctx.channel().toString(), ctx.channel().id());
		
		ctx.fireChannelInactive();
		
	};
    
	
	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise future)
			throws Exception {
		
		super.close(ctx, future);
		
		activeClose = true;
		logger.info("channel active close: info={}, address={}({})", getInfo(), ctx.channel().toString(), ctx.channel().id());
	}

	@Override
    public void write(final ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                	Throwable cause = future.cause();
                	logException(future.channel(), cause);
                	
                	// fire up
                	ctx.fireExceptionCaught(cause);
                }
            }
        });
		
		ctx.write(msg, promise);
    }

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		logException(ctx.channel(), cause);
		ctx.fireExceptionCaught(cause);
	}
	
	
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
    	promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                	Throwable cause = future.cause();
                	logException(future.channel(), cause);
                }
            }
        });
        ctx.connect(remoteAddress, localAddress, promise);
    }

	
}
