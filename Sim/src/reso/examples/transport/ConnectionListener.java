package reso.examples.transport;

import reso.common.Message;

public interface ConnectionListener {

	void onAccept(Connection conn, Connection cliConn);
	void onClearToSend(Connection conn) throws Exception;
	void onClose(Connection conn);
	void onConnected(Connection conn);
	void onReceived(Connection conn, Message m);

}
