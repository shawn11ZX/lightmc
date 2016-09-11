package com.wooduan.lightmc.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractApcServer
{
	private static final Logger Logger = LoggerFactory.getLogger(AbstractApcServer.class);
	
	private boolean ePoolDisabled = true;
	private int port;
	private Channel serverChannel;
	private ServerBootstrap server;
	protected EventLoopGroup bossGroup;
	protected EventLoopGroup workerGroup;
	
	public AbstractApcServer() 
	{
		/**
		 * since one channel can only bind to one ventLoopGroup, there is no use of have more than 1 boss threads
		 * @see http://stackoverflow.com/questions/34275138/why-do-we-really-need-multiple-netty-boss-threads
		 */
		this(1, 6);
	}
	public AbstractApcServer(int bossCount, int workerCount) 
	{
		boolean epollSucc = false;
		if (Epoll.isAvailable() && !ePoolDisabled)
		{
			try {
				bossGroup = new EpollEventLoopGroup(bossCount);
		        workerGroup = new EpollEventLoopGroup(workerCount);
				this.server = new ServerBootstrap();
		        this.server.group(bossGroup, workerGroup).channel(EpollServerSocketChannel.class);
				epollSucc = true;
				Logger.info("using epoll succ");
			} 
			catch (Exception e)
			{
				Logger.error("error ", e);
				ePoolDisabled = true;
			} 
			catch (Error e)
			{
				Logger.error("error ", e);
				ePoolDisabled = true;
			}
		}
		if (!epollSucc)
		{
			Logger.info("using nio...");
			bossGroup = new NioEventLoopGroup(bossCount);
	        workerGroup = new NioEventLoopGroup(workerCount);
			this.server = new ServerBootstrap();
			this.server.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
		}
        
		
		setOptions(this.server);
		
		
	}
	
	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}
	
	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}
	
	public ServerBootstrap getServerBootstrap() {
		return server;
	}
	protected void setOptions(ServerBootstrap s) 
	{
        s.childOption(ChannelOption.TCP_NODELAY, true);
        s.childOption(ChannelOption.SO_KEEPALIVE, true);
	}
	
		
	protected abstract void initClientPipeline(SocketChannel ch, ChannelPipeline p);
	
	
	
	protected void clientChannelPipelineFactory(ServerBootstrap server) 
	{
		server.childHandler(new ChannelInitializer<SocketChannel>(){
               @Override
               public void initChannel(SocketChannel ch) throws Exception 
               {
            	   ChannelPipeline p = ch.pipeline();
            	   initClientPipeline(ch, p);
               }
           });
	}
	
	
	
	public int getPort()
	{
		return port;
	}
	
	public void start(int port) throws SocketException 
	{
		clientChannelPipelineFactory(this.server);
				
		Logger.info("{} trying to bind to port {}", this.getClass().getName(), port);
		this.port = port;
		ChannelFuture f = this.server.bind(new InetSocketAddress(this.port));
		f.syncUninterruptibly();
		this.serverChannel = f.channel();
		
	}
	
	public void waitTermination() throws InterruptedException {
		this.serverChannel.closeFuture().sync();
	}
	
	public void shutdown() 
	{
		
		
		if (this.serverChannel != null) 
		{
			this.serverChannel.close().awaitUninterruptibly();
		}
		
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		
	}
	
	public Channel getServerChannel() {
		return this.serverChannel;
	}
}
