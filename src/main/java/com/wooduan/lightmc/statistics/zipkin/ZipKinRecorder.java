package com.wooduan.lightmc.statistics.zipkin;

import java.util.ArrayList;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zipkin.Codec;
import zipkin.Span;

public class ZipKinRecorder {
	private static final Logger logger = LoggerFactory.getLogger(ZipKinRecorder.class);
	

	public static void sendSpan(Span span) {
			//logger.info("span {}", span);
			ArrayList<Span> l = new ArrayList<Span>();
			l.add(span);
			byte[] bytes = Codec.JSON.writeSpans(l);
			sendSpan(bytes);
	}
	
	
	public static void sendSpan(byte[] data) {
		HttpClient httpClient = new HttpClient();
		HttpConnectionManagerParams managerParams = httpClient
				.getHttpConnectionManager().getParams();
		// 设置连接超时时间(单位毫秒)
		managerParams.setConnectionTimeout(5000);
		// 设置读数据超时时间(单位毫秒)
		managerParams.setSoTimeout(5000);

		PostMethod method = new PostMethod(
				"http://192.168.0.2:9411/api/v1/spans");

		method.setRequestEntity(new ByteArrayRequestEntity(data));
		method.setRequestHeader("Content-type", "application/octet-stream");

		try {
			int statusCode = httpClient.executeMethod(method);
			String rc = method.getResponseBodyAsString();

		} catch (Exception e) {
			System.out.print(e);
		} finally {
			method.releaseConnection();
		}
	}
	
}
