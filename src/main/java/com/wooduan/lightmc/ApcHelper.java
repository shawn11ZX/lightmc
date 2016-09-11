package com.wooduan.lightmc;


public class ApcHelper {

	private static final ThreadLocal<NetSession> currentNetSession = new ThreadLocal<NetSession>();
	private static final ThreadLocal<APC> currentAPC = new ThreadLocal<APC>();

    public static NetSession getCurrentNetSession() {
        return currentNetSession.get();
    }
    
    public static void setCurrentNetSession(NetSession netSession) {
        currentNetSession.set(netSession);
    }
    
    
    public static APC getCurrentAPC() {
        return currentAPC.get();
    }
    
    public static void setCurrentAPC(APC netSession) {
    	currentAPC.set(netSession);
    }
    
}