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
	final public static ConcurrentHashMap<String, CallStatistics> allStatistics = new ConcurrentHashMap<String, CallStatistics>();
	
	final public int SHORT_HISTORY = 10;
	final public int LONG_HISTORY = StatisticHisotry.MAX_HISTORY_COUNT;
	
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
	
	
	
	
	
	
	
	/*********************************************************
	 * 
	 **********************************************************/
	
	final public String name;
	
	final public AutoResetValueItem inputPopCount;
	
	final public AutoResetValueItem connectSuccCount;
	final public AutoResetValueItem connectFailCount;
	final public AutoResetValueItem connectRequestCount;
	final public AutoResetValueItem channelActiveCount;
	final public AutoResetValueItem channelInactiveCount;
	
	final public AutoResetValueItem outputPushCount;
	final public AutoResetValueItem outputPopCount;
	final public ValueItem outputQueueLength;
	
	
	final public AutoResetValueItem apcPushCount;
	final public AutoResetValueItem apcPopCount;
	final public ValueItem apcQueueLength;
	
	final public ConcurrentHashMap<TraceInfo, Long> apcCallingList = new ConcurrentHashMap<TraceInfo, Long>();
	
	final public ConcurrentHashMap<String, AverageValueItem> inputApcAvgTimeByTypeMap = new ConcurrentHashMap<String, AverageValueItem>();
	final public ConcurrentHashMap<String, AverageValueItem> inputApcLengthByTypeMap = new ConcurrentHashMap<String, AverageValueItem>();
	final public ConcurrentHashMap<String, AverageValueItem> outputApcLengthByTypeMap = new ConcurrentHashMap<String, AverageValueItem>();
    
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
		
		
		apcPushCount = getItem("apcPushCount", AutoResetValueItem.class);
		apcPopCount = getItem("apcPopCount", AutoResetValueItem.class);
		apcQueueLength = getItem("apcQueueLength", CumulativeValueItem.class);
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
		if (apc != null)
		{
			int v = apcQueueLength.increase();
			apcPushCount.increase();
			inputPopCount.increase();
			inputApcLengthByType(apc.getName()).record(length);
			super.endInboundDecoding(apc, length);
		}
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
		return apcQueueLength.getValue();
	}
	

	
	
}
