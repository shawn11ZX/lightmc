package com.wooduan.lightmc.statistics;


public class MicroTime {
	static long bootTsMs = System.currentTimeMillis();
	static long bootTsNano = System.nanoTime();
	
	
	public static long currentTimeMicros()
	{
		return (bootTsMs * 1000L) + (System.nanoTime() - bootTsNano)/1000L; 
	}
	
}
