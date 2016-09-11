package com.wooduan.lightmc.sample;

import java.net.SocketException;

import com.wooduan.lightmc.ApcHelper;
import com.wooduan.lightmc.ApcSerializer;
import com.wooduan.lightmc.ApcServer;
import com.wooduan.lightmc.NetSession;
import com.wooduan.lightmc.NetSessionEventHandler;

public class SampleServer {
	
	
	public static class ServiceCallbackHandler implements ISampleService, NetSessionEventHandler
	{
		ISampleServiceCallback userCallbak;
		@Override
		public void register(String name) {
			NetSession client = ApcHelper.getCurrentNetSession();
			userCallbak = client.newOutputProxy(ISampleServiceCallback.class);
			userCallbak.onRegisterSucc(name);
			System.out.println("received register from client, reply onRegisterSucc");
		}

		@Override
		public void heartBeat() {
			System.out.println("received heartBeat from client, reply onHeartBeat");
			userCallbak.onHeartBeat();
		}

		@Override
		public void channelConnected(NetSession session) {
			System.out.println("connected to client");
		}

		@Override
		public void channelConnectFailed(NetSession session) {
			
		}

		@Override
		public void channelDisconnected(NetSession session) {
			
		}

		@Override
		public void channelExceptionCaught(NetSession session, Throwable cause) {
			
		}
	}
	
	public static void main(String[] args) throws SocketException, InterruptedException {
		LogManager.initConfig("config/logback.xml");
		
		ServiceCallbackHandler service = new ServiceCallbackHandler();
		ApcServer server = ApcServer.newBuilder()
				.setSessionEventHandler(service)
				.setApcSerializer(ApcSerializer.JObj)
				.setApcThreadCount(2)
				.setIoThreadCount(2)
				.registerCallback(service, ISampleService.class)
				.build();
		
		
		server.start(Config.getLoginPort());
		server.waitTermination();
	}

}
