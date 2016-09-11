package com.wooduan.lightmc.statistics.zipkin;

import com.wooduan.lightmc.statistics.MicroTime;

import zipkin.Annotation;
import zipkin.BinaryAnnotation;
import zipkin.Span;


public class ZipKinThreadState {
	private static ThreadLocal<Long> currentTraceIdContainer = new ThreadLocal<Long>();
	private static ThreadLocal<Long> currentSpanIdContainer = new ThreadLocal<Long>();
	private static ThreadLocal<Span.Builder> currentSpanBuilderContainer = new ThreadLocal<Span.Builder>();
	
	
	public static void setCurrentSpanId(Long currentSpanId) {
		currentSpanIdContainer.set(currentSpanId);
	}
	
	public static long getCurrentSpanId() {
		Long rc =  currentSpanIdContainer.get();
		if (rc != null)
			return rc;
		else 
			return 0;
	}
	
	
	public static void clearCurrentSpanId() {
		currentSpanIdContainer.remove();;
	}
	
	
	public static void setCurrentTraceId(Long currentTraceId) {
		currentTraceIdContainer.set(currentTraceId);
	}
	
	
	public static long getCurrentTraceId() {
		Long rc =  currentTraceIdContainer.get();
		if (rc != null)
			return rc;
		else 
			return 0;
	}

	
	public static void clearCurrentTraceId() {
		currentTraceIdContainer.remove();
	}
	
	
	public static void setCurrentSpanBuilder(Span.Builder builder) {
		currentSpanBuilderContainer.set(builder);
	}
	
	
	

	
	public static void clearCurrentSpanBuilder() {
		currentSpanBuilderContainer.remove();
	}

	public static void trace(String string) {
		Span.Builder builder = currentSpanBuilderContainer.get();
		if (builder != null)
		{
			Annotation annotation = Annotation.create(
					MicroTime.currentTimeMicros(), 
					string, 
					ZipKinSetting.getEndpoint());
			builder.addAnnotation(annotation);
		}
	}
	
	public static void trace(String key, String value) {
		Span.Builder builder = currentSpanBuilderContainer.get();
		if (builder != null)
		{
			BinaryAnnotation annotation = BinaryAnnotation.create(
					key, 
					value,
					ZipKinSetting.getEndpoint());
			builder.addBinaryAnnotation(annotation);
		}
	}
	
	
	
}
