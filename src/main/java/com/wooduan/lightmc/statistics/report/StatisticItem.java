package com.wooduan.lightmc.statistics.report;

import java.util.Map;

public interface StatisticItem {
	void saveAndReset(String name, Map<String, Object> output);

}