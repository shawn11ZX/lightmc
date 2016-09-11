package com.wooduan.lightmc.test;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;

public class UnSerializableArg implements Externalizable{
	public UnSerializableArg()
	{
		
	}
	ConcurrentHashMap<String, String> h = new ConcurrentHashMap<String, String>();
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		throw new IOException("s");
	}
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		throw new IOException("s");
	}
}
