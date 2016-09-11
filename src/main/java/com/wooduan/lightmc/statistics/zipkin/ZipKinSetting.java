package com.wooduan.lightmc.statistics.zipkin;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zipkin.Endpoint;

public class ZipKinSetting {
	private static final Logger logger = LoggerFactory.getLogger(ZipKinSetting.class);
	private static AtomicLong spanIdGenerator = new AtomicLong(System.currentTimeMillis() << 32);
	private static AtomicLong traceIdGenerator = new AtomicLong(System.currentTimeMillis() << 32);
	private static Endpoint endpoint = Endpoint.create("unknown", 0);
	private static boolean enabled = false;
	public static void init(String serviceName, String ipStr, int serviceId)
	{
		traceIdGenerator.set((System.currentTimeMillis() << 24) | (serviceId << 16));
		spanIdGenerator.set((System.currentTimeMillis() << 24) | (serviceId << 16));
		
		int ip = 0;
		
		try {
			InetAddress[] inetAddressArray = InetAddress.getAllByName(ipStr);
			for (int i = 0; i < inetAddressArray.length; i++)
			{
				if (inetAddressArray[i] instanceof Inet4Address)
				{
					Inet4Address ipv4 = (Inet4Address)inetAddressArray[i];
					byte[] bytes = ipv4.getAddress();
					if (bytes.length > 3)
					{
						ip = (bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3];
					}
					break;
				}
			}
			
		} catch (UnknownHostException e) {
			logger.error("init ZipKinSetting error", e);
		}
		
		
		
		endpoint = Endpoint.create(serviceName + "." + serviceId, ip);
	}
	
	
	public static void setEnabled(boolean enabled)
	{
		ZipKinSetting.enabled = enabled;
	}
	
	public static boolean isEnabled()
	{
		return enabled;
	}
	
	public static Endpoint getEndpoint() {
		return endpoint;
	}
	
	public static long newSpanId()
	{
		return spanIdGenerator.incrementAndGet();
	}
	
	public static long newTraceId()
	{
		return traceIdGenerator.incrementAndGet();
	}
	
	
}
