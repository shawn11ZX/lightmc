package com.wooduan.lightmc.statistics.report;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.statistics.DefaultApcStatisticRecorder;
import com.wooduan.lightmc.statistics.zipkin.TraceInfo;

public class CallStatistics extends DefaultApcStatisticRecorder  {
	private static final Logger Logger = LoggerFactory.getLogger(CallStatistics.class);
	private static ConcurrentHashMap<String, CallStatistics> allStatistics = new ConcurrentHashMap<String, CallStatistics>();
	public static CallStatistics getInstance(String name)
	{
		
		CallStatistics r =  allStatistics.get(name);
		if (r == null)
		{
			r = new CallStatistics(name);
			allStatistics.put(name, r);
		}
		return r;
	}
	
	public static void printAllStatistics(StringBuilder sb)
	{
		
		sb.append("==============Call Statistics===============");		
		sb.append("<br style='clear:both;'>");
		sb.append("<br>");
		
		for(CallStatistics s: allStatistics.values())
		{
			s.printOverview(sb);
		}
		
		sb.append("<br style='clear:both;'>");
		sb.append("<br>");
		
		for(CallStatistics s: allStatistics.values())
		{
			s.printPendingApc(sb);
		}
		
		sb.append("<br style='clear:both;'>");
		sb.append("<br>");
		
		for(CallStatistics s: allStatistics.values())
		{
			s.printApcProcessByType(sb);
		}
		
		sb.append("<br>");
		sb.append("<br>");
		
		for(CallStatistics s: allStatistics.values())
		{
			s.printApcOutputByType(sb);
		}
		
		sb.append("<br>");
		sb.append("<br>");
		
		for(CallStatistics s: allStatistics.values())
		{
			s.printApcInputByType(sb);
		}
	}
	
	
	/*********************************************************
	 * 
	 **********************************************************/
	
	final private String name;
	
	final private AutoResetValueItem inputPopCount;
	
	final private AutoResetValueItem connectSuccCount;
	final private AutoResetValueItem connectFailCount;
	final private AutoResetValueItem connectRequestCount;
	final private AutoResetValueItem channelActiveCount;
	final private AutoResetValueItem channelInactiveCount;
	
	final private AutoResetValueItem outputPushCount;
	final private AutoResetValueItem outputPopCount;
	final private ValueItem outputQueueLength;
	final private ValueItem outputQueueLengthMax;
	
	
	final private AutoResetValueItem apcPushCount;
	final private AutoResetValueItem apcPopCount;
	final private ValueItem apcQueueLength;
	final private ValueItem apcQueueLengthMax;
	
	private ConcurrentHashMap<TraceInfo, Long> apcCallingList = new ConcurrentHashMap<TraceInfo, Long>();
	
	private ConcurrentHashMap<String, AverageValueItem> inputApcAvgTimeByTypeMap = new ConcurrentHashMap<String, AverageValueItem>();
	private ConcurrentHashMap<String, AverageValueItem> inputApcLengthByTypeMap = new ConcurrentHashMap<String, AverageValueItem>();
	private ConcurrentHashMap<String, AverageValueItem> outputApcLengthByTypeMap = new ConcurrentHashMap<String, AverageValueItem>();
    
	DecimalFormat doubleFormat = new DecimalFormat("#.###");
    
    
    public String getName() {
		return name;
	}
	private <T extends StatisticItem> T getItem(String name, Class<T> c) {
		return StatisticManager.INSTANCE.getItem("q_" + name + "_" + this.name, c);
	}
	private AverageValueItem inputApcAvgTimeByType(String type)
	{
		AverageValueItem rc = inputApcAvgTimeByTypeMap.get(type);
		if (rc == null)
		{
			rc = StatisticManager.INSTANCE.getItem("m_" + type, AverageValueItem.class);
			inputApcAvgTimeByTypeMap.put(type, rc);
		}
		return rc;
	}
	
	private AverageValueItem outputApcLengthByType(String type)
	{
		AverageValueItem rc = outputApcLengthByTypeMap.get(type);
		if (rc == null)
		{
			rc = StatisticManager.INSTANCE.getItem("o_" + type, AverageValueItem.class);
			outputApcLengthByTypeMap.put(type, rc);
		}
		return rc;
	}
	
	private AverageValueItem inputApcLengthByType(String type)
	{
		AverageValueItem rc = inputApcLengthByTypeMap.get(type);
		if (rc == null)
		{
			rc = StatisticManager.INSTANCE.getItem("i_" + type, AverageValueItem.class);
			inputApcLengthByTypeMap.put(type, rc);
		}
		return rc;
	}
	
	
	private CallStatistics(String name) {
		this.name = name;
		
		inputPopCount = getItem("inputPopCount", AutoResetValueItem.class);
		
		connectSuccCount = getItem("connectSuccCount", AutoResetValueItem.class);
		connectFailCount = getItem("connectFailCount", AutoResetValueItem.class);
		connectRequestCount = getItem("connectRequestCount", AutoResetValueItem.class);
		channelActiveCount = getItem("channelActive", AutoResetValueItem.class);
		channelInactiveCount = getItem("channelInactive", AutoResetValueItem.class);
		
		outputPushCount = getItem("outputPushCount", AutoResetValueItem.class);
		outputPopCount = getItem("outputPopCount", AutoResetValueItem.class);
		outputQueueLength = getItem("outputQueueLength", CumulativeValueItem.class);
		outputQueueLengthMax = getItem("outputQueueLength", AutoResetValueItem.class);
		
		
		apcPushCount = getItem("apcPushCount", AutoResetValueItem.class);
		apcPopCount = getItem("apcPopCount", AutoResetValueItem.class);
		apcQueueLength = getItem("apcQueueLength", CumulativeValueItem.class);
		apcQueueLengthMax = getItem("apcQueueLengthMax", AutoResetValueItem.class);
	}
	
	
	@Override
	public void channelActive() {
		channelActiveCount.increase();
	}

	@Override
	public void channelConnectRequested() {
		connectRequestCount.increase();
	}

	@Override
	public void channelConnectFail() {
		connectFailCount.increase();
	}

	@Override
	public void channelConnectSucc() {
		connectSuccCount.increase();
	}
	
	@Override
	public void channelInActive() {
		channelInactiveCount.increase();
	}
	
	@Override
	public void channelException(String exceptionName) {
		
		AutoResetValueItem item =  getItem("channelException_" + exceptionName, AutoResetValueItem.class);
		item.increase();
	}
	@Override
	public void beginInboundDecoding()
	{
		super.beginInboundDecoding();
	}
	
	@Override
	public void endInboundDecoding(TraceInfo apc, int length) {
		int v = apcQueueLength.increase();
		if (v > apcQueueLengthMax.get())
		{
			apcQueueLengthMax.set(v);
		}
		apcPushCount.increase();
		inputPopCount.increase();
		inputApcLengthByType(apc.getName()).record(length);
		super.endInboundDecoding(apc, length);
	}

	@Override
	public void beginInboundDispatching(TraceInfo apc) {
		apcQueueLength.descrease();
		apcPopCount.increase();
		apcCallingList.put(apc, System.currentTimeMillis());
		super.beginInboundDispatching(apc);
	}

	@Override
	public void endInboundDispatching(TraceInfo apc, long durationNano) {
		apcCallingList.remove(apc);		
		AverageValueItem item = inputApcAvgTimeByType(apc.getName());
	    item.record(durationNano/1000L);
		super.beginInboundDecoding();
	}

	@Override
	public void beginOutboundAsyncWriting(TraceInfo apc) {
		int v = outputQueueLength.increase();
		if (v > outputQueueLengthMax.get())
		{
			outputQueueLengthMax.set(v);
		}
		outputPushCount.increase();
		super.beginOutboundAsyncWriting(apc);
	}
	
	@Override
	public void endOutboundAsyncWriting() {
		outputQueueLength.descrease();
		outputPopCount.increase();
		super.endOutboundAsyncWriting();
	}

	@Override
	public void beginOutboundEncoding(TraceInfo apc) {
		super.beginOutboundEncoding(apc);
	}

	@Override
	public void endOutboundEncoding(TraceInfo apc) {
		super.endOutboundEncoding(apc);
	}

	@Override
	public void beginOutboundWriting(TraceInfo apc, int length) {
		outputApcLengthByType(apc.getName()).record(length);
		super.beginOutboundWriting(apc, length);
	}
	
	@Override
	public void endOutboundWriting(TraceInfo apc) {
		super.endOutboundWriting(apc);
	}


	public int getDispatchQueueLength()
	{
		return apcQueueLength.get();
	}
	

	public void printStatistics(StringBuilder string) 
	{
		string.append("<br>============== CallStatistic Begin (" + name + ") ===============<br>");
		
		printOverview(string);
				
		printPendingApc(string);
		
		printApcProcessByType(string);

		printApcOutputByType(string);
		
		printApcInputByType(string);
		
		string.append("==============CallStatistic End ===============<br>");
	}
	private void printOverview(StringBuilder string) {
		string.append("<table width='400px' border='1' align='center' cellpadding='2' cellspacing='1' style='float:left;'>");
		string.append("<caption>[" + name + "] overview </caption>");
		string.append("<tr>");
		string.append("<td>name</td>");
		string.append("<td>value</td>");
		string.append("</tr>");
		
		string.append("<tr bgcolor='#D1DDAA'><td colspan='2'>Input Queue</td></tr>");
		string.append("<tr><td>pop count</td><td>" + inputPopCount.get() + "</td></tr>");
		
		string.append("<tr bgcolor='#D1DDAA'><td colspan='2'>Output Queue</td></tr>");
		string.append("<tr><td>push count</td><td>" + outputPushCount.get() + "</td></tr>");
		string.append("<tr><td>pop count</td><td>" + outputPopCount.get() + "</td></tr>");
		string.append("<tr><td>queue count</td><td>" + outputQueueLength.get() + "</td></tr>");
		string.append("<tr><td>queue count max</td><td>" + outputQueueLengthMax.get() + "</td></tr>");
		
		string.append("<tr bgcolor='#D1DDAA'><td colspan='2'>APC Queue </td></tr>");
		string.append("<tr><td>push count</td><td>" + apcPushCount.get() + "</td></tr>");
		string.append("<tr><td>pop count</td><td>" + apcPopCount.get() + "</td></tr>");
		string.append("<tr><td>queue count</td><td>" + apcQueueLength.get() + "</td></tr>");
		string.append("<tr><td>queue count max</td><td>" + apcQueueLengthMax.get() + "</td></tr>");
		
		string.append("</table>");
	}
	private void printPendingApc(StringBuilder string) {
		
		string.append("<table width='400px' border='1' align='center' cellpadding='2' cellspacing='1' style='float:left;' >");
		string.append("<caption>[" + name +"] Pending APC</caption>");
		string.append("<tr bgcolor='#D1DDAA'>");
		string.append("<td>name</td>");
		string.append("<td>duration(ms)</td>");
		string.append("</tr>");
		for (Map.Entry<TraceInfo, Long> entry: apcCallingList.entrySet())
		{
			string.append("<tr>");
			string.append("<td>").append(entry.getKey().getName()).append("</td>");
			string.append("<td>").append((System.currentTimeMillis() - entry.getValue())).append("ms</td>");
			string.append("</tr>");
		}
		string.append("</table>");
	}
	
	private void printApcProcessByType(StringBuilder string) {
		
		string.append("<table width='100%' border='1' align='center' cellpadding='2' cellspacing='1' >");
		string.append("<caption>[" + name +"] Apc Process By Type</caption>");
		string.append("<tr bgcolor='#D1DDAA'>");
		string.append("<td width='20%'>name</td>");
		string.append("<td width='10%'>Qps</td>");
		string.append("<td width='10%'>Count</td>");
		string.append("<td width='10%'>AvgMicro</td>");
		string.append("<td width='10%'>TotalMicro</td>");
		string.append("<td width='10%'>TotalMicro%</td>");
		string.append("</tr>");
		formatAvgRows(string, inputApcAvgTimeByTypeMap);
		string.append("</table>");
	}
	
	
	private void printApcOutputByType(StringBuilder string) {
		string.append("<table width='100%' border='1' align='center' cellpadding='2' cellspacing='1' >");
		string.append(" <caption>[" + name +"] Apc Output By Type </caption>");
		string.append("<tr bgcolor='#D1DDAA'>");
		string.append("<td width='20%'>name</td>");
		string.append("<td width='10%'>Qps</td>");
		string.append("<td width='10%'>Count</td>");
		string.append("<td width='10%'>AvgLength</td>");
		string.append("<td width='10%'>TotalLength</td>");
		string.append("<td width='10%'>TotalLength%</td>");
		string.append("</tr>");
		formatAvgRows(string, outputApcLengthByTypeMap);
		string.append("</table>");
	}
	private void printApcInputByType(StringBuilder string) {
		string.append("<table width='100%' border='1' align='center' cellpadding='2' cellspacing='1' >");
		string.append("<caption>[" + name +"] Apc Input By Type</caption>");
		string.append("<tr bgcolor='#D1DDAA'>");
		string.append("<td width='20%'>name</td>");
		string.append("<td width='10%'>Qps</td>");
		string.append("<td width='10%'>Count</td>");
		string.append("<td width='10%'>AvgLength</td>");
		string.append("<td width='10%'>TotalLength</td>");
		string.append("<td width='10%'>TotalLength%</td>");
		string.append("</tr>");
		formatAvgRows(string, inputApcLengthByTypeMap);
		string.append("</table>");
	}
	
	private void formatAvgRows(StringBuilder string, ConcurrentHashMap<String, AverageValueItem> map) {
		long totalMicro = 0;
		long totalCount = 0;
		for (Map.Entry<String, AverageValueItem> entry: map.entrySet())
		{
			AverageValueItem v = entry.getValue();
			totalMicro += v.getLastTotalValue();
			totalCount += v.getLastCount();
		}
		if (totalMicro == 0)
			totalMicro = 1;
		for (Map.Entry<String, AverageValueItem> entry: map.entrySet())
		{
			AverageValueItem v = entry.getValue();
			string.append("<tr>");
			string.append("<td>").append(entry.getKey()).append("</td>");
			string.append("<td>").append(doubleFormat.format(v.getLastQps())).append("</td>");
			string.append("<td>").append(v.getLastCount()).append("</td>");
			string.append("<td>").append(v.getLastAvgValue()).append("</td>");
			string.append("<td>").append(v.getLastTotalValue()).append("</td>");
			string.append("<td>").append(doubleFormat.format(v.getLastTotalValue()*1.0/(totalMicro))).append("</td>");
			string.append("</tr>");
		}
		
		string.append("<tr>");
		string.append("<td>[SUMMARY]").append("</td>");
		string.append("<td>").append("</td>");
		string.append("<td>").append(totalCount).append("</td>");
		string.append("<td>").append("</td>");
		string.append("<td>").append(totalMicro).append("</td>");
		string.append("<td>").append("</td>");
		string.append("</tr>");
	}
	

	

	
	
}
