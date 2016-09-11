package com.wooduan.lightmc.sample;

import com.wooduan.lightmc.ApcClientFactory;
import com.wooduan.lightmc.ApcSerializer;
import com.wooduan.lightmc.HeartBeatHandler;
import com.wooduan.lightmc.NetSession;
import com.wooduan.lightmc.NetSessionEventHandler;
import com.wooduan.lightmc.impl.ClientNetSession;

public class SampleClient {
	
	public static class ServiceCallbackHandler implements ISampleServiceCallback, NetSessionEventHandler, HeartBeatHandler
	{
		ISampleService helloService;
		@Override
		public void onRegisterSucc(String name) {
			System.out.println("server reply: onRegisterSucc " + name);
		}

		@Override
		public void channelConnected(NetSession session) {
			System.out.println("connected to server send register");
			helloService = session.newOutputProxy(ISampleService.class);
			helloService.register("bob");
		}

		@Override
		public void channelConnectFailed(NetSession session) {
		}

		@Override
		public void channelDisconnected(NetSession session) {
			System.out.println("disconnected to server");
		}

		@Override
		public void channelExceptionCaught(NetSession session, Throwable cause) {
			
		}

		@Override
		public void onHeartBeat() {
			System.out.println("server reply: onHeartBeat");
		}

		@Override
		public void onSendHeartBeat() {
			helloService.heartBeat();
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		LogManager.initConfig("config/logback.xml");
		
		ServiceCallbackHandler handler = new ServiceCallbackHandler();
		
		ApcClientFactory pool = ApcClientFactory.newBuilder()
				.registerCallback(handler, ISampleServiceCallback.class)
				.setApcSerializer(ApcSerializer.JObj)
				.setApcThreadCount(2)
				.build();
		
		
		ClientNetSession client = pool.connect(Config.getLoginIp(), Config.getLoginPort(), handler);
		client.enableAutoReconnect(10, handler);
		
		Thread.sleep(100000);
		
		client.disableAutoReconnect();
		client.closeChannel();
		
		
			
		
		
	}

}
