package com.wooduan.lightmc.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wooduan.lightmc.HeartBeatHandler;

public class HeartBeatTask implements Runnable {
	private static final Logger Logger = LoggerFactory.getLogger(HeartBeatTask.class);
	ClientNetSession apcClient;
	HeartBeatHandler calback;
	public HeartBeatTask(ClientNetSession clientNetSession,
			HeartBeatHandler calback) {
		this.apcClient = clientNetSession;
		this.calback = calback;
	}

	@Override
	public void run() {
		if (apcClient.isConnected()) 
		{
			
			try 
			{
				calback.onSendHeartBeat();
			} 
			catch (Exception e) 
			{
				Logger.error(e.getMessage(), e);
			}
		} 
		else 
		{
			try 
			{
				if (!apcClient.isConnected())
					apcClient.resetConnect();
			} 
			catch(Exception e) 
			{
				Logger.error("thread(id:" + Thread.currentThread().getId() + ") has Exception:" + e.getMessage(), e);
			}
		}
	}

}
