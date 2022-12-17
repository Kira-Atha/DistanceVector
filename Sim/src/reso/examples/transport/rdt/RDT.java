package reso.examples.transport.rdt;

import reso.common.Message;

public interface RDT {

	public void handleData(int sn, Message m) throws Exception;
	public void handleACK(int sn) throws Exception;
	public boolean send(Message m) throws Exception;
	public boolean clearToSend();
	
}
