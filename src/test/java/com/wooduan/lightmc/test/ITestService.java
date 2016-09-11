package com.wooduan.lightmc.test;

import java.util.Map;


interface ITestService {
	void registerClient(String name);
	void sendMap(Map<String, String> m);
	void unserializableArgument(UnSerializableArg arg);
	void heartBeat();
}