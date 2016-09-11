package com.wooduan.lightmc.sample;

import com.wooduan.lightmc.ApcClientFactory;
import com.wooduan.lightmc.ApcSerializer;
import com.wooduan.lightmc.NetSession;
import com.wooduan.lightmc.NetSessionEventHandler;
import com.wooduan.lightmc.impl.ClientNetSession;

public class SampleClientStress {
	
	public static class ServiceCallbackHandler implements ISampleServiceCallback, NetSessionEventHandler
	{
		ISampleService helloService;
		@Override
		public void onRegisterSucc(String name) {
			System.out.print("y");
			//System.out.println("Server echoed " + name);
		}

		@Override
		public void channelConnected(NetSession session) {
			System.out.print("+");
			
			helloService = session.newOutputProxy(ISampleService.class);
			helloService.register("bob");
		}

		@Override
		public void channelConnectFailed(NetSession session) {
			System.out.print("x");
		}

		@Override
		public void channelDisconnected(NetSession session) {
			System.out.print("-");
		}

		@Override
		public void channelExceptionCaught(NetSession session, Throwable cause) {
			
		}

		@Override
		public void onHeartBeat() {
			System.out.print("h");
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		LogManager.initConfig("config/logback.xml");
		
		ApcClientFactory pool = ApcClientFactory.newBuilder().
			registerCallback(new ServiceCallbackHandler(), ISampleServiceCallback.class).
			setApcSerializer(ApcSerializer.JObj).
			setApcThreadCount(2).
			build();
		
		
		while(true)
		{
			ClientNetSession[] clients = new ClientNetSession[1000];
			
			for (int j = 0; j < clients.length; j++)
			{
				ServiceCallbackHandler handler = new ServiceCallbackHandler();
				clients[j] = pool.connect(Config.getLoginIp(), Config.getLoginPort(), handler);
						
			}
			
			Thread.sleep(1000);
			
			for (int j = 0; j < clients.length; j++)
			{
				clients[j].closeChannel();
			}
			
		}
			
			
		
		
	}

}
