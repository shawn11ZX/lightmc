package com.wooduan.lightmc;

import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.EventExecutorGroup;

import com.wooduan.lightmc.serializer.amf3.Amf3ApcSerializer;
import com.wooduan.lightmc.serializer.jobj.JObjApcSerializer;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;

public interface ApcSerializer {
	
	public final ApcSerializer Amf3Server = new Amf3ApcSerializer(true, false);
	public final ApcSerializer Amf3Client = new Amf3ApcSerializer(false, false);
	public final ApcSerializer CompressedAmf3Server = new Amf3ApcSerializer(true, true);
	public final ApcSerializer CompressedAmf3Client = new Amf3ApcSerializer(false, true);
	public final ApcSerializer JObj = new JObjApcSerializer(false);
	public final ApcSerializer JObjCompress = new JObjApcSerializer(true);
	
	public void addPreHandler(EventExecutorGroup apcExecutorGroup, ChannelPipeline p);
	public void addApcHandler(EventExecutorGroup apcExecutorGroup, ChannelPipeline p, ApcStatisticRecorder statisticRecorder, int maxApcLength);
	
}
