package com.wooduan.lightmc.serializer.amf3;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.EventExecutorGroup;

import com.wooduan.lightmc.APC;
import com.wooduan.lightmc.ApcSerializer;
import com.wooduan.lightmc.netty.AttackFilterHandler;
import com.wooduan.lightmc.netty.BeginApcStatisticHandler;
import com.wooduan.lightmc.netty.EndApcStatisticHandler;
import com.wooduan.lightmc.serializer.ApcMigration;
import com.wooduan.lightmc.statistics.ApcStatisticRecorder;
import com.wooduan.lightmc.statistics.zipkin.TraceInfo;

import flex.messaging.io.BeanProxy;
import flex.messaging.io.ClassAliasRegistry;

public class Amf3ApcSerializer implements ApcSerializer {
	static {
		BeanProxy.addIgnoreProperty(APC.class, "netsession");
		BeanProxy.addIgnoreProperty(TraceInfo.class, "spanId");
		BeanProxy.addIgnoreProperty(TraceInfo.class, "startTsMicro");
		BeanProxy.addIgnoreProperty(TraceInfo.class, "spanbuilder");
		ClassAliasRegistry.getRegistry().registerAlias(ApcMigration.AMF3_NAME, APC.class.getName());
	}
	
	
	private boolean isServer;
	private boolean compressed;
	public Amf3ApcSerializer(boolean isServer, boolean compressed) {
		this.isServer = isServer;
		this.compressed = compressed;
	}
	
	@Override
	public void addApcHandler(
			EventExecutorGroup apcExecutorGroup, 
			ChannelPipeline pipeline, 
			ApcStatisticRecorder statisticRecorder, int maxApcLength) {
		
		pipeline.addLast(apcExecutorGroup, new LengthFieldBasedFrameDecoder(maxApcLength, 0, 4, 0, 4, true));
		pipeline.addLast(apcExecutorGroup, new LengthFieldPrepender(4, 0));
		
		pipeline.addLast(apcExecutorGroup, new BeginApcStatisticHandler(statisticRecorder));

		if (compressed)
		{
			pipeline.addLast(apcExecutorGroup, new DecompressInputHandler());
			pipeline.addLast(apcExecutorGroup, new CompressOutputHandler());
		}
		pipeline.addLast(apcExecutorGroup, new Amf3InboundHandler());
		pipeline.addLast(apcExecutorGroup, new Amf3OutboundHandler());
		
		pipeline.addLast(apcExecutorGroup, new EndApcStatisticHandler(statisticRecorder));
		
		pipeline.addLast(apcExecutorGroup, new AttackFilterHandler(10, 1000));
	}

	@Override
	public void addPreHandler(EventExecutorGroup apcExecutorGroup,
			ChannelPipeline pipeline) {
		if (isServer)
		{
			// remove will fail if add to apcExecutorGroup
			pipeline.addLast(new PolicyWriterHandler());
		}
	}

}
