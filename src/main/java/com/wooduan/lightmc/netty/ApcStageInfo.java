package com.wooduan.lightmc.netty;

public class ApcStageInfo {
	public static enum Stage {
		BeginDecoding, EndDecoding, BeginDispatching, EndDispatching, BeginEncoding, EndEncoding;
	}
	
	long BeginDecodingTS;
	long EndDecodingTS;
	long BeginDispatchingTS;
	long EndDispatchingTS;
	long BeginEncodingTS;
	long EndEncodingTS;
}
