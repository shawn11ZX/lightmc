package com.wooduan.lightmc;

import java.io.Serializable;

import com.wooduan.lightmc.serializer.ApcMigration;
import com.wooduan.lightmc.statistics.zipkin.TraceInfo;

import flex.messaging.io.ClassAlias;

// asynchronous procedure call
public class APC extends TraceInfo implements Serializable, ClassAlias {
	
	
	
	static Object[] NULL_PARAM = new Object[0];
	
	/**
	 * for migratoin purpose
	 */
	@Override
	public String getAlias() {
		return ApcMigration.AMF3_NAME;
	}
	
	private static final long serialVersionUID = -1300444721079633780L;
	public String functionName;
	public Object[] parameters = NULL_PARAM;
	public transient NetSession netsession;
	
	
	public APC() {
		
	}
	
	
	public APC(String functionName, Object[] parameters)
	{
		this.functionName = functionName;
		this.parameters = parameters;
	}
	
	public APC(String functionName)
	{
		this.functionName = functionName;
	}
	
	
	
	public String getFunctionName() {
		return this.functionName;
	}
	
	public Object[] getParameters() {
		return this.parameters;
	}
	
	public void setNetsession(NetSession session)
	{
		this.netsession = session;
	}
	
	public NetSession getNetsession() {
		return this.netsession;
	}
	
	@Override
	public String getName() {
		return functionName;
	}
	

	@Override
	public String toString() {
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("[" + functionName);
			sb.append("(" + parameters.length);
			for (int i = 0; i < parameters.length; i++)
			{
				if (i != 0)
					sb.append(",");
				sb.append(parameters[i]);
			}
			sb.append(")]");
			return sb.toString();
		}
		catch (Exception e)
		{
			return "[toString of APC fail]" + functionName;
		}
	}


	
}
