
/**
 * This package enables us to establish message deliver channels between clients and servers.
 * 
 * Messages are encoded according to the provided {@link com.wooduan.lightmc.ApcSerializer}
 * 
 * <h2>Reliability</h2>
 * There is no guaranteed transfer of messages, so you have to design you applications to deal with message lost situations.
 * However, you can configure channels to continues trying sending messages until channel is disconnected. 
 * And when that happens, you can have you client/server states reset. 
 * Since channel disconnection event is guaranteed to be received at both sides, 
 * you can be sure that the state of your client/server with return to normal finally.   
 * 
 * <p>
 * Though, you should be cautious about receiving a new connection before the old connection is disconnected. 
 * You can avoid this problem by rejecting a new connection if there is an existing one.
 * 
 * refer to SampleClient and SampleServer for sample code.
 * 
 */
package com.wooduan.lightmc;


