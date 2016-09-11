package com.wooduan.lightmc.impl;

import com.wooduan.lightmc.statistics.ApcStatisticRecorder;

import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

public class ServerNetSession extends AbstractNetSession {
	private volatile Channel channel;
	
	public ServerNetSession(SocketChannel ch, boolean reliable, ApcStatisticRecorder statisticRecorder) {
		super(reliable, statisticRecorder);
		this.channel = ch;
	}

	public ServerNetSession(SocketChannel ch, boolean reliable) {
		super(reliable);
		this.channel = ch;
	}
	
	@Override
	public Channel getChannel() {
		return channel;
	}
}
