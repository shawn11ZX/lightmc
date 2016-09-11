package com.wooduan.lightmc.test;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.Timeout;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.ApcClientFactory;
import com.wooduan.lightmc.ApcHelper;
import com.wooduan.lightmc.ApcSerializer;
import com.wooduan.lightmc.ApcServer;
import com.wooduan.lightmc.HeartBeatHandler;
import com.wooduan.lightmc.NetSession;
import com.wooduan.lightmc.NetSessionEventHandler;
import com.wooduan.lightmc.RelayService;
import com.wooduan.lightmc.impl.ClientNetSession;
import com.wooduan.lightmc.proxy.ApcRelayProxy;
import com.wooduan.lightmc.sample.LogManager;
import com.wooduan.lightmc.serializer.amf3.CompressOutputHandler;
import com.wooduan.lightmc.serializer.jobj.JObjCompressor;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;

@RunWith(Parameterized.class)
public class ApcTest {
	
	@Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                 
                 {ApcSerializer.Amf3Client, ApcSerializer.Amf3Server, ApcSerializer.JObj},
                 {ApcSerializer.CompressedAmf3Client, ApcSerializer.CompressedAmf3Server, ApcSerializer.JObj},
                 {ApcSerializer.JObj, ApcSerializer.JObj, ApcSerializer.Amf3Client}, 
                 {ApcSerializer.JObjCompress, ApcSerializer.JObjCompress, ApcSerializer.Amf3Client},
           });
    }
    
//    public ApcTest() {
//        this.clientserializer = ApcSerializer.JObj;
//        this.clientserializerError = ApcSerializer.Amf3Client;
//        this.serverserializer = ApcSerializer.JObj;
//    }
    
	public ApcTest(ApcSerializer clientserializer, ApcSerializer serverserializer,  ApcSerializer clientserializerError) {
        this.clientserializer = clientserializer;
        this.clientserializerError = clientserializerError;
        this.serverserializer = serverserializer;
    }
	  
	String ip = "127.0.0.1";
	int port = 3234;
	int TIMEOUT_MS = 5000;
	
	NetSessionEventHandler serverSessionHandler;
	ITestService serverCallbackObj ;
	RelayService serverRelayCallbackObj ;
	
	NetSessionEventHandler clientSessionHandler ;
	ITestServiceCallback clientCallbackObj ;
	
	ApcServer server;
	ApcClientFactory clientPool;
	ApcClientFactory clientPoolError;
	
	ApcSerializer clientserializer = ApcSerializer.JObj;
	ApcSerializer clientserializerError = ApcSerializer.Amf3Client;
	ApcSerializer serverserializer = ApcSerializer.JObj;
	
	ApcStatisticRecorder serverStatisticRecorder;
	ApcStatisticRecorder clientStatisticRecorder;
	@Before
	public  void setUp() throws InterruptedException, SocketException {
		JObjCompressor.setCompressionThreshold(1);
		
		LogManager.initConfig("config/logback.xml");
		
		serverSessionHandler = Mockito.spy(NetSessionEventHandler.class);
		serverCallbackObj = Mockito.spy(ITestService.class);
		serverRelayCallbackObj = Mockito.spy(RelayService.class);
		
		clientSessionHandler = Mockito.spy(NetSessionEventHandler.class);
		clientCallbackObj = Mockito.spy(ITestServiceCallback.class);
		
		serverStatisticRecorder = Mockito.spy(ApcStatisticRecorder.class);
		clientStatisticRecorder = Mockito.spy(ApcStatisticRecorder.class);
		server = buildServer();
		
		
		
		clientPool = buildClient();
		
		clientPoolError = ApcClientFactory.newBuilder().
				registerCallback(clientCallbackObj, ITestServiceCallback.class).
				setApcSerializer(clientserializerError).
				setStatisticRecorder(clientStatisticRecorder).
				setApcThreadCount(2).
				build();
	}

	private ApcClientFactory buildClient() {
		return buildClient(180, 100000);
	}
	private ApcClientFactory buildClient(int readTimeout, int maxApcLength) {
		return ApcClientFactory.newBuilder().
				registerCallback(clientCallbackObj, ITestServiceCallback.class).
				setApcSerializer(clientserializer).
				setStatisticRecorder(clientStatisticRecorder).
				setApcThreadCount(2).
				setReadTimeout(readTimeout).setMaxApcLength(maxApcLength).
				build();
	}

	private ApcServer buildServer() {
		return buildServer(180, 1000000);
	}
	private ApcServer buildServer(int readTimeout, int maxApcLength) {
		return ApcServer.newBuilder().
				setSessionEventHandler(serverSessionHandler).
				setApcSerializer(serverserializer).
				setTreatExceptionAsInfo(false).
				setApcThreadCount(2).
				setIoThreadCount(2).
				setStatisticRecorder(serverStatisticRecorder).
				setReadTimeout(readTimeout).setMaxApcLength(maxApcLength).
				registerCallback(serverCallbackObj, ITestService.class).
				registerCallback(serverRelayCallbackObj, RelayService.class).
				build();
	}
	
	@After
	public void tearDown()
	{
		server.shutdown();
		clientPool.shutdown();
	}
	
	
	@Test
	public void testSimpleConnection() throws SocketException, InterruptedException
	{

		server.start(port);
		ClientNetSession clientSession = clientPool.connect(ip, port, clientSessionHandler);
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
		Mockito.reset(serverSessionHandler);
		Mockito.reset(clientSessionHandler);
		clientSession.closeChannel();
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));
		Mockito.reset(serverSessionHandler);
		Mockito.reset(clientSessionHandler);
		
		clientSession.resetConnect();
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
	
	}
	
	private Timeout newTimeout(int count) {
		return new Timeout(TIMEOUT_MS, VerificationModeFactory.times(count));
	}
	
	
	@Test
	public void testStressConnection() throws SocketException, InterruptedException
	{
		
		server.start(port);
		
		ClientNetSession[] clients = new ClientNetSession[1000];
		for (int i = 0; i < clients.length; i++)
		{
			clients[i] = clientPool.connect(ip, port, clientSessionHandler);
		}
		
		Mockito.verify(serverSessionHandler, newTimeout(clients.length)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, newTimeout(clients.length)).channelConnected(Mockito.any(NetSession.class));
			
		Mockito.reset(serverSessionHandler);
		Mockito.reset(clientSessionHandler);
		
		for (int i = 0; i < clients.length; i++)
		{
			clients[i].closeChannel();
		}
		
			
		Mockito.verify(serverSessionHandler, newTimeout(clients.length)).channelDisconnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, newTimeout(clients.length)).channelDisconnected(Mockito.any(NetSession.class));
		Mockito.reset(serverSessionHandler);
		Mockito.reset(clientSessionHandler);
			
		for (int i = 0; i < clients.length; i++)
		{
			clients[i].resetConnect();
		}
			
		Mockito.verify(serverSessionHandler, newTimeout(clients.length)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, newTimeout(clients.length)).channelConnected(Mockito.any(NetSession.class));
		
	}
	
	
	@Test
	public void testStressRpc() throws SocketException, InterruptedException
	{
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				NetSession session = invocation.getArgumentAt(0, NetSession.class);
				ITestService service = session.newOutputProxy(ITestService.class);
				service.registerClient("me");
				return null;
			}
			
		}).when(clientSessionHandler).channelConnected(Mockito.any(NetSession.class));
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				String name = invocation.getArgumentAt(0, String.class);
				NetSession session = ApcHelper.getCurrentNetSession();
				ITestServiceCallback service = session.newOutputProxy(ITestServiceCallback.class);
				service.onHello(name);
				return null;
			}
			
		}).when(serverCallbackObj).registerClient(Mockito.any(String.class));
		
		server.start(port);		
		ClientNetSession[] clients = new ClientNetSession[1000];
		for (int i = 0; i < clients.length; i++)
		{
			clients[i] = clientPool.connect(ip, port, clientSessionHandler);
		}
		
		
		
		Mockito.verify(serverSessionHandler, newTimeout(clients.length)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, newTimeout(clients.length)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(serverCallbackObj, newTimeout(clients.length)).registerClient("me");
		Mockito.verify(clientCallbackObj, newTimeout(clients.length)).onHello("me");
		
		ArgumentCaptor<APC> argument = ArgumentCaptor.forClass(APC.class);
		Mockito.verify(serverStatisticRecorder, newTimeout(clients.length)).endInboundDispatching(argument.capture(), Mockito.anyInt());
		org.junit.Assert.assertEquals("registerClient", argument.getValue().getName());
		
		argument = ArgumentCaptor.forClass(APC.class);
		Mockito.verify(serverStatisticRecorder, newTimeout(clients.length)).beginOutboundWriting(argument.capture(), Mockito.anyInt());
		org.junit.Assert.assertEquals("onHello", argument.getValue().getName());
		
		argument = ArgumentCaptor.forClass(APC.class);
		Mockito.verify(clientStatisticRecorder, newTimeout(clients.length)).endInboundDispatching(argument.capture(), Mockito.anyInt());
		org.junit.Assert.assertEquals("onHello", argument.getValue().getName());
		
		argument = ArgumentCaptor.forClass(APC.class);
		Mockito.verify(clientStatisticRecorder, newTimeout(clients.length)).beginOutboundWriting(argument.capture(), Mockito.anyInt());
		org.junit.Assert.assertEquals("registerClient", argument.getValue().getName());
		
		Mockito.reset(serverSessionHandler);
		Mockito.reset(clientSessionHandler);
	}
	
	@Test
	public void testSendMap() throws SocketException, InterruptedException
	{
		CompressOutputHandler.flag = false;
		server.start(port);		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
		ITestService serverService = client.newOutputProxy(ITestService.class);
		
		Map<String, String> m = new HashMap();
		for (int i = 0; i < 1000; i++)
		{
			m.put("sssssss" + i, "vvvvvvvvvv" + i);
		}
		serverService.sendMap(m);
		
		Mockito.verify(serverCallbackObj, Mockito.timeout(TIMEOUT_MS)).sendMap(Mockito.any(Map.class));
		

		Thread.sleep(1000);
		org.junit.Assert.assertTrue(client.isConnected());
	}
	
	@Test
	public void testSendMap2() throws SocketException, InterruptedException
	{
		CompressOutputHandler.flag = true;
		server.start(port);		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
		ITestService serverService = client.newOutputProxy(ITestService.class);
		
		Map<String, String> m = new HashMap();
		for (int i = 0; i < 1000; i++)
		{
			m.put("sssssss" + i, "vvvvvvvvvv" + i);
		}
		serverService.sendMap(m);
		
		Mockito.verify(serverCallbackObj, Mockito.timeout(TIMEOUT_MS)).sendMap(Mockito.any(Map.class));
		

		Thread.sleep(1000);
		org.junit.Assert.assertTrue(client.isConnected());
	}
	
	
	@Test
	public void testReadSerializeException() throws SocketException, InterruptedException
	{
		
		server.start(port);		
		ClientNetSession client = clientPoolError.connect(ip, port, clientSessionHandler);
		
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
		ITestService serverService = client.newOutputProxy(ITestService.class);
		serverService.registerClient("me");
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelExceptionCaught(Mockito.any(NetSession.class), Mockito.any(Throwable.class));
		

		Thread.sleep(1000);
		org.junit.Assert.assertTrue(client.isConnected());
	}
	
	@Test
	public void testWriteSerializeException() throws SocketException, InterruptedException
	{
		
		server.start(port);		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
		ITestService serverService = client.newOutputProxy(ITestService.class);
		serverService.unserializableArgument(new UnSerializableArg());
		
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelExceptionCaught(Mockito.any(NetSession.class), Mockito.any(Throwable.class));

		Thread.sleep(1000);
		org.junit.Assert.assertTrue(client.isConnected());
	}
	
	
	@Test
	public void testDispatcherException() throws SocketException, InterruptedException
	{
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				throw new IllegalArgumentException("ss");
			}
			
		}).when(serverCallbackObj).registerClient(Mockito.any(String.class));
		
		server.start(port);	
		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
		ITestService serverService = client.newOutputProxy(ITestService.class);
		serverService.registerClient("me");
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelExceptionCaught(Mockito.any(NetSession.class), Mockito.any(Throwable.class));
		

		Thread.sleep(1000);
		org.junit.Assert.assertTrue(client.isConnected());
		
	}
	
	@Test
	public void testAutoHeartBeat() throws SocketException, InterruptedException
	{
		
		server.start(port);	
		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		final ITestService serverService = client.newOutputProxy(ITestService.class);
		
		client.enableAutoReconnect(1, new HeartBeatHandler() {
			
			@Override
			public void onSendHeartBeat() {
				serverService.registerClient("me");
			}
		});
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(serverCallbackObj, Mockito.timeout(TIMEOUT_MS)).registerClient(Mockito.any(String.class));	
		
		Mockito.reset(serverSessionHandler);
		Mockito.reset(clientSessionHandler);
		Mockito.reset(serverCallbackObj);
		
		server.shutdown();
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));

		Mockito.reset(serverSessionHandler);
		Mockito.reset(clientSessionHandler);
		Mockito.reset(serverCallbackObj);
		
		server = buildServer();
		server.start(port);
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(serverCallbackObj, Mockito.timeout(TIMEOUT_MS)).registerClient(Mockito.any(String.class));
		
		client.disableAutoReconnect();
	}
	
	@Test
	public void testClientTimeout() throws SocketException, InterruptedException
	{
		clientPool.shutdown();
		clientPool = buildClient(2, 60000);
		
		
		server.start(port);	
		
		
		
		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		final ITestService serverService = client.newOutputProxy(ITestService.class);
		
		client.enableAutoReconnect(5, new HeartBeatHandler() {
			
			@Override
			public void onSendHeartBeat() {
				serverService.registerClient("me");
			}
		});
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));

		client.disableAutoReconnect();
	}
	
	@Test
	public void testServerTimeout() throws SocketException, InterruptedException
	{
		server.shutdown();
		server = buildServer(2, 100000);
		
		
		server.start(port);	
		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		final ITestService serverService = client.newOutputProxy(ITestService.class);
		
		client.enableAutoReconnect(5, new HeartBeatHandler() {
			
			@Override
			public void onSendHeartBeat() {
				serverService.registerClient("me");
			}
		});
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));

		client.disableAutoReconnect();
	}
	
	@Test
	public void testClientMaxLengthExceed() throws SocketException, InterruptedException
	{
		
		clientPool.shutdown();
		clientPool = buildClient(2, 5);
		
		server.start(port);	
		
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				String name = invocation.getArgumentAt(0, String.class);
				NetSession session = ApcHelper.getCurrentNetSession();
				ITestServiceCallback service = session.newOutputProxy(ITestServiceCallback.class);
				service.onHello(name);
				return null;
			}
			
		}).when(serverCallbackObj).registerClient(Mockito.any(String.class));
		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		final ITestService serverService = client.newOutputProxy(ITestService.class);
		
		client.enableAutoReconnect(5, new HeartBeatHandler() {
			
			@Override
			public void onSendHeartBeat() {
				serverService.registerClient("me");
			}
		});
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		serverService.registerClient("me");
		
		Mockito.reset(serverSessionHandler);
		Mockito.reset(clientSessionHandler);
		
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelExceptionCaught(Mockito.any(NetSession.class), Mockito.any(Throwable.class));
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));
		

		client.disableAutoReconnect();
	}
	
	@Test
	public void testServerMaxLength() throws SocketException, InterruptedException
	{
		
		server.shutdown();
		server = buildServer(2, 5);
		
		server.start(port);	
		
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				String name = invocation.getArgumentAt(0, String.class);
				NetSession session = ApcHelper.getCurrentNetSession();
				ITestServiceCallback service = session.newOutputProxy(ITestServiceCallback.class);
				service.onHello(name);
				return null;
			}
			
		}).when(serverCallbackObj).registerClient(Mockito.any(String.class));
		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		final ITestService serverService = client.newOutputProxy(ITestService.class);
		
		client.enableAutoReconnect(5, new HeartBeatHandler() {
			
			@Override
			public void onSendHeartBeat() {
				serverService.registerClient("me");
			}
		});
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		serverService.registerClient("me");
		
		Mockito.verify(serverSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelExceptionCaught(Mockito.any(NetSession.class), Mockito.any(Throwable.class));
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelDisconnected(Mockito.any(NetSession.class));

		client.disableAutoReconnect();
	}
	
	
	
	volatile Object tag;
	
	@Test
	public void testRelay() throws SocketException, InterruptedException
	{
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				tag = invocation.getArgumentAt(0, Object.class);
				APC apc = invocation.getArgumentAt(1, APC.class);
				NetSession session = ApcHelper.getCurrentNetSession();
				
				session.call(apc);
				return null;
			}
			
		}).when(serverRelayCallbackObj).onRelay(Mockito.any(Object.class), Mockito.any(APC.class));
		
		server.start(port);	
		
		
		
		ClientNetSession client = clientPool.connect(ip, port, clientSessionHandler);
		
		Mockito.verify(clientSessionHandler, Mockito.timeout(TIMEOUT_MS)).channelConnected(Mockito.any(NetSession.class));
		
		ITestServiceCallback serverService = client.newRelayProxy(ITestServiceCallback.class);
		((ApcRelayProxy)serverService).setExtraTag("mytag");
		serverService.onHello("me");
				
		Mockito.verify(clientCallbackObj, Mockito.timeout(TIMEOUT_MS)).onHello(Mockito.eq("me"));
		org.junit.Assert.assertEquals("mytag", tag);
	}
}
