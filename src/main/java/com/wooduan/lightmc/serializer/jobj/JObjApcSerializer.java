package com.wooduan.lightmc.serializer.jobj;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.EventExecutorGroup;

import com.wooduan.lightmc.ApcSerializer;
import com.wooduan.lightmc.netty.BeginApcStatisticHandler;
import com.wooduan.lightmc.netty.EndApcStatisticHandler;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;

public class JObjApcSerializer implements ApcSerializer {

	private boolean compressed;
	public JObjApcSerializer(boolean compressed) {
		this.compressed = compressed;
	}
	
	@Override
	public void addApcHandler(EventExecutorGroup apcExecutorGroup, ChannelPipeline pipeline, ApcStatisticRecorder statisticRecorder, int maxApcLength) {

		pipeline.addLast(apcExecutorGroup, new LengthFieldBasedFrameDecoder(maxApcLength, 0, 4, 0, 4, true));
		pipeline.addLast(apcExecutorGroup, new LengthFieldPrepender(4, 0));
		pipeline.addLast(apcExecutorGroup, new BeginApcStatisticHandler(statisticRecorder));
		if (compressed)
		{
			pipeline.addLast(apcExecutorGroup, new CompressedJObjInboundHandler());
			pipeline.addLast(apcExecutorGroup, new CompressedJObjOutboundHandler());
		}
		else {
			pipeline.addLast(apcExecutorGroup, new JObjInboundHandler());
			pipeline.addLast(apcExecutorGroup, new JObjOutboundHandler());
		}
	
		pipeline.addLast(apcExecutorGroup, new EndApcStatisticHandler(statisticRecorder));
	}

	@Override
	public void addPreHandler(EventExecutorGroup apcExecutorGroup,
			ChannelPipeline p) {
		
	}
	
	

}
