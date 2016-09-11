package com.wooduan.lightmc.serializer.amf3;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.CharsetUtil;

public class PolicyWriterHandler extends ChannelInboundHandlerAdapter {
	private static final String XML = "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\" /></cross-domain-policy>\0";
	

	public PolicyWriterHandler() {
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {

		ByteBuf buffer = (ByteBuf) msg;

		if (buffer.readableBytes() < 2) {
			return;
		}

		final int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
		final int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);
		boolean isFlashPolicyRequest = (magic1 == '<' && magic2 == 'p');

		if (isFlashPolicyRequest) {
			buffer.skipBytes(buffer.readableBytes());

			removeAllPipelineHandlers(ctx.pipeline());
			ByteBuf policyResponse = Unpooled.copiedBuffer(XML,
		            CharsetUtil.UTF_8);
			ctx.writeAndFlush(policyResponse).addListener(ChannelFutureListener.CLOSE);
			buffer.release();
			return;
		}
		else {
			ctx.pipeline().remove(this);
			ctx.fireChannelRead(buffer);
		}
		
	}

	private void removeAllPipelineHandlers(ChannelPipeline pipeline) {
		while (pipeline.first() != null) {
			pipeline.removeFirst();
		}
	}
}
