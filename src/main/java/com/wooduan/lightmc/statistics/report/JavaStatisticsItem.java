package com.wooduan.lightmc.statistics.report;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaStatisticsItem implements StatisticItem {
	private static final Logger Logger = LoggerFactory
			.getLogger(JavaStatisticsItem.class);

	
	JavaStatisticsItem() {
		
	}
	static final List<String> YoungGenCollectorNames = Arrays.asList(new String[] {
			// Oracle (Sun) HotSpot
			// -XX:+UseSerialGC
			"Copy",
			// -XX:+UseParNewGC
			"ParNew",
			// -XX:+UseParallelGC
			"PS Scavenge",

			// Oracle (BEA) JRockit
			// -XgcPrio:pausetime
			"Garbage collection optimized for short pausetimes Young Collector",
			// -XgcPrio:throughput
			"Garbage collection optimized for throughput Young Collector",
			// -XgcPrio:deterministic
			"Garbage collection optimized for deterministic pausetimes Young Collector" });

	static final List<String> OldGenCollectorNames = Arrays.asList(new String[] {
			// Oracle (Sun) HotSpot
			// -XX:+UseSerialGC
			"MarkSweepCompact",
			// -XX:+UseParallelGC and (-XX:+UseParallelOldGC or
			// -XX:+UseParallelOldGCCompacting)
			"PS MarkSweep",
			// -XX:+UseConcMarkSweepGC
			"ConcurrentMarkSweep",

			// Oracle (BEA) JRockit
			// -XgcPrio:pausetime
			"Garbage collection optimized for short pausetimes Old Collector",
			// -XgcPrio:throughput
			"Garbage collection optimized for throughput Old Collector",
			// -XgcPrio:deterministic
			"Garbage collection optimized for deterministic pausetimes Old Collector" });


	public void recordGc(Map<String, Object> output) {
		List<GarbageCollectorMXBean> gcBeans = ManagementFactory
				.getGarbageCollectorMXBeans();
		int youngGCCount = 0;
		int fullGCCount = 0;
		for (GarbageCollectorMXBean gcBean : gcBeans) {
			gcBean.getCollectionCount();
			if (YoungGenCollectorNames.contains(gcBean.getName()))  
		        youngGCCount += gcBean.getCollectionCount();  
			if (OldGenCollectorNames.contains(gcBean.getName()))  
				fullGCCount += gcBean.getCollectionCount();  
		}
		output.put("gc_full", fullGCCount);
		output.put("gc_young", youngGCCount);
	}
	
	
	
	boolean sigarLinkError;
	@Override
	public void saveAndReset(String name, Map<String, Object> output) {
		output.put("mem_heapFreeMemory", Runtime.getRuntime().freeMemory());
		output.put("mem_heapTotalMemory", Runtime.getRuntime().totalMemory());
		output.put("mem_heapFreeMemoryPercent", Runtime.getRuntime().freeMemory() * 1.0 /Runtime.getRuntime().totalMemory());
		MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage memUsage = memoryMxBean.getNonHeapMemoryUsage();
		output.put("mem_nonHeapTotalMemory", memUsage.getUsed());
		
		recordGc(output);
	}

	
}