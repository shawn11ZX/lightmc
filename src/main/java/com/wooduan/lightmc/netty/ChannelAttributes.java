package com.wooduan.lightmc.netty;

import io.netty.util.AttributeKey;

import com.wooduan.lightmc.APC;

public class ChannelAttributes {
	static public AttributeKey<APC> CURRENT_WRITE_APC = AttributeKey.newInstance("cwa");
	static public AttributeKey<APC> CURRENT_READ_APC = AttributeKey.newInstance("cra");
}
