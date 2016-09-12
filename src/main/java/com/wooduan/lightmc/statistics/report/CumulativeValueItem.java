package com.wooduan.lightmc.statistics.report;

import java.util.Map;

/** 计数器，每次发送后会被清零
 * 
 * @author Sheen
 *
 */
public class CumulativeValueItem extends ValueItem {
	
	public void saveAndReset(String name, Map<String, Object> output) {
		save(name, output);
		resetMinMax();
		
	}

	
}