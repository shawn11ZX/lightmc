package com.wooduan.lightmc.statistics.zipkin;

import java.io.Serializable;

import com.wooduan.lightmc.statistics.MicroTime;

import zipkin.BinaryAnnotation;
import zipkin.Span;

public abstract class TraceInfo implements Serializable {

	private static final long serialVersionUID = -2612352539412154651L;
	
	private long parentSpanId;
	private long traceId;
	private boolean isTrace = true;
	
	private transient long spanId;
	private transient long startTsMicro;
	private transient Span.Builder spanbuilder;
	
	
	public TraceInfo()
	{
		this.traceId = ZipKinThreadState.getCurrentTraceId();
		this.parentSpanId = ZipKinThreadState.getCurrentSpanId();
		this.isTrace = ZipKinSetting.isEnabled() && this.isTrace;
	}
	
	abstract public String getName();

	
	public void beginSpan(long tsMicro) {
		this.startTsMicro = tsMicro;
		if (isTrace)
		{
			this.traceId = (traceId != 0) ? traceId : ZipKinSetting.newTraceId();
			this.spanId = ZipKinSetting.newSpanId();
			this.spanbuilder = Span.builder()
					.traceId(traceId)
		    	    .id(spanId)
		    	    .parentId(parentSpanId)
		    	    .timestamp(tsMicro);
		}
	}
	
	public void saveThreadState() {
		if (isTrace)
		{
			ZipKinThreadState.setCurrentSpanId(this.spanId);
			ZipKinThreadState.setCurrentTraceId(this.traceId);
			ZipKinThreadState.setCurrentSpanBuilder(this.spanbuilder);
		}
	}
	
	public void beginSpan() {
		beginSpan(MicroTime.currentTimeMicros());
	}
	
	public long endSpan(String spanName) {
		long duration = MicroTime.currentTimeMicros() - startTsMicro;
		if(isTrace)
    	{
			if (spanbuilder != null)
			{
				String finalSpanName = spanName + "." + getName();
				BinaryAnnotation annotation = BinaryAnnotation.create(
						"method",
						getName(), 
						ZipKinSetting.getEndpoint());
				
				
				
				
				
		    	Span span = spanbuilder
		    	    .name(finalSpanName)
		    	   	.duration(duration)
		    	    .addBinaryAnnotation(annotation)
		    	    .build();
		    	
		    	
	    		ZipKinRecorder.sendSpan(span);
			}
			
			ZipKinThreadState.clearCurrentSpanBuilder();
			ZipKinThreadState.clearCurrentSpanId();
			ZipKinThreadState.clearCurrentTraceId();
			
			this.parentSpanId = this.spanId;
			this.spanId = 0;
			this.spanbuilder = null;
			this.startTsMicro = 0;
    	}
		
		return duration;
	}
}
