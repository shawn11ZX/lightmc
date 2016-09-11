package com.wooduan.lightmc.sample;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;

import java.net.SocketException;
import java.util.Set;

import com.wooduan.lightmc.ApcHelper;
import com.wooduan.lightmc.ApcSerializer;
import com.wooduan.lightmc.ApcServer;
import com.wooduan.lightmc.NetSession;
import com.wooduan.lightmc.NetSessionEventHandler;

public class SampleServerStress {
	
	static class MyNetSessionEventHandler implements NetSessionEventHandler {
		volatile static boolean first = true;
		@Override
		public void channelConnected(NetSession session) {
			System.out.print("+");
		}

		@Override
		public void channelDisconnected(NetSession session) {
			System.out.print("-");
		}

		@Override
		public void channelExceptionCaught(NetSession session, Throwable cause) {
			System.out.print("x");
		}
		@Override
		public void channelConnectFailed(NetSession session) {
		}
		
	}
	
	public static class ServiceCallbackHandler implements ISampleService
	{
		ISampleServiceCallback userCallbak;
		@Override
		public void register(String name) {
			NetSession client = ApcHelper.getCurrentNetSession();
			userCallbak = client.newOutputProxy(ISampleServiceCallback.class);
			userCallbak.onRegisterSucc(name);
			
			//System.out.println("Server echoed " + name);
		}

		@Override
		public void heartBeat() {
			userCallbak.onHeartBeat();
		}
	}
	
	public static void main(String[] args) throws SocketException, InterruptedException {
		LogManager.initConfig("config/logback.xml");
		
		ApcServer server = ApcServer.newBuilder().
				setSessionEventHandler(new MyNetSessionEventHandler()).
				setApcSerializer(ApcSerializer.JObj).
				setApcThreadCount(2).
				setIoThreadCount(2).
				registerCallback(new ServiceCallbackHandler(), ISampleService.class).
				build();
		
		
		server.start(Config.getLoginPort());
		while(true)
		{
			Thread.sleep(5000);
			Set<EventExecutor> executors = server.getWorkerGroup().children();
			int tasks = 0;
			for (EventExecutor executor: executors)
			{
				if (executor instanceof SingleThreadEventExecutor) {
					final SingleThreadEventExecutor e = (SingleThreadEventExecutor) executor;
					tasks += e.pendingTasks();
					
				}
				
			}
			System.out.println("tasks: " + tasks);
		}
	}

}
