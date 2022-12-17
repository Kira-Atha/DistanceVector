package reso.examples.transport.rdt;

import reso.common.AbstractTimer;
import reso.common.Message;
import reso.examples.transport.Connection;
import reso.examples.transport.Segment;

public class SimpleRDT implements RDT {
	
	public static final int RTO_TIMER = 2; // in seconds 
	
	private final Connection conn;
	private int nextSN= 0;
	private int expSN= 0;

	private Message lastMessage; // last message sent
	private int lastSN;          // ACK of last message
	
	private RetransmitTimer timer;
	
	private class RetransmitTimer extends AbstractTimer {
		
		public RetransmitTimer() {
			super(conn.transport.ip.host.getNetwork().getScheduler(), RTO_TIMER, false);
		}
		
		@Override
		protected void run() throws Exception {
			// Retransmit message upon timer expiration
			retransmit();
			start();
		}
		
	}
	
	public SimpleRDT(Connection conn, int localISN, int remoteISN) {
		this.conn= conn;
		nextSN= localISN;
		expSN= remoteISN;
		timer= new RetransmitTimer();
	}

	public boolean send(Message m) throws Exception {
		if (lastMessage != null)
			return false;
		lastMessage= m;
		lastSN= nextSN;
		conn.transport.send(conn, Segment.createDATA(conn, lastSN, expSN, m));
		nextSN+= m.getByteLength();
		timer.start();
		return true;
	}
	
	@Override
	public void handleData(int sn, Message m) throws Exception {
		if (sn == expSN) {
			expSN+= m.getByteLength();
			// Deliver to application
			conn.notifyReceived(m);
			// Send ACK
			conn.transport.send(conn, Segment.createACK(conn, nextSN, expSN));
		}
	}

	@Override
	public void handleACK(int sn) throws Exception {
		if (lastMessage == null)
			return; // Nothing to ACK
		if (sn >= lastSN + lastMessage.getByteLength()) {
			timer.stop();
			lastMessage= null;
			conn.notifyClearToSend();
		} else {
			// Do nothing (timeout on sender-side responsible for retransmission)
		}
	}

	@Override
	public boolean clearToSend() {
		return (lastMessage == null);
	}
	
	private void retransmit() throws Exception {
		System.out.println("Re-transmit " + lastMessage);
		conn.transport.send(conn, Segment.createDATA(conn, lastSN, expSN, lastMessage));
	}
	
}
