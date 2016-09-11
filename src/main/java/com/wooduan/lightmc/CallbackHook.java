package com.wooduan.lightmc;


public interface CallbackHook {
	public void preCall(APC apc);
	public void postCall(APC apc);
}
