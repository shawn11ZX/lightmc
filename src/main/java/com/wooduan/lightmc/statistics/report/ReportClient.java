package com.wooduan.lightmc.statistics.report;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public enum ReportClient {
	INSTANCE;
	
	final static String CONF_SERVER_HOST = "reporter.url";
	final static String CONF_KEY = "reporter.seurity.key";
	
	private static final Logger logger = LoggerFactory.getLogger(ReportClient.class);
	
	private static MessageDigest digest = null;
	private String serverUrl = null;
	private String securityKey = null;
	
	
	

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getSecurityKey() {
		return securityKey;
	}

	public void setSecurityKey(String securityKey) {
		this.securityKey = securityKey;
	}

	
	public synchronized static final String hash(String data) {
		if (digest == null) {
			try {
				digest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException nsae) {
				System.err.println("Failed to load the MD5 MessageDigest. "
						+ "Jive will be unable to function normally.");
				nsae.printStackTrace();
			}
		}
		// Now, compute hash.
		digest.update(data.getBytes());
		return toHex(digest.digest());
	}
	
	public static final String toHex(byte hash[]) {
		StringBuffer buf = new StringBuffer(hash.length * 2);
		int i;

		for (i = 0; i < hash.length; i++) {
			if (((int) hash[i] & 0xff) < 0x10) {
				buf.append("0");
			}
			buf.append(Long.toString((int) hash[i] & 0xff, 16));
		}
		return buf.toString();
	}
	
	public void httpSendData(String pairs) throws URISyntaxException{
		HttpClient httpClient = new HttpClient();
		HttpConnectionManagerParams managerParams = httpClient .getHttpConnectionManager().getParams(); 
		//设置连接超时时间(单位毫秒) 
		managerParams.setConnectionTimeout(5000);
		//设置读数据超时时间(单位毫秒) 
		managerParams.setSoTimeout(5000);
		
		URI uri = new URI(serverUrl);
		PostMethod method = new PostMethod(uri.toASCIIString());
		
		String time = System.currentTimeMillis() +"";
		method.addParameter("json", pairs);
		method.addParameter("timestamp", time);
		
		String verify_result = hash(pairs+time+securityKey);
		method.addParameter("sign", verify_result);
		
		method.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "utf-8");
		
		try {
			int statusCode = httpClient.executeMethod(method);
			String rc = method.getResponseBodyAsString();
			
			logger.info("post report to {}, rc {}, code {} length {}", serverUrl, rc, statusCode, method.getRequestEntity().getContentLength());
		} catch (HttpException e) {
			logger.error("httpSendData" + e.getMessage());
		} catch (IOException e) {
			logger.error("httpSendData" +e.getMessage());
		}
		finally {
			method.releaseConnection();
		}
	}
	
	public void report(Map<String, Object> map)
	{
		try {
			if (serverUrl != null && !serverUrl.isEmpty()) {
				
				
				String json;
				try {
					json = new ObjectMapper().writeValueAsString(map);
					httpSendData(json);
				} catch (JsonProcessingException e) {
					logger.error("error report", e);
				}
				
				
			}
		}
		catch (Exception e )
		{
			logger.error("error report", e);
		}
	}
}
