package reso.examples.transport.rdt;

import java.util.LinkedList;
import java.util.Queue;

import reso.common.AbstractTimer;
import reso.common.Message;
import reso.examples.transport.Connection;
import reso.examples.transport.Segment;

public class GoBackN implements RDT {

	public static final int RTO_TIMER = 2; // in seconds
	
	private final Connection conn;
	private final int N; // Max. window size
	private int baseSN; // SN at base of window
	private int nextSN;
	private Queue<Segment> sendWindow= new LinkedList<>();
	private int expSN;
	
	private RetransmitTimer timer;
	
	private class RetransmitTimer extends AbstractTimer {
		
		public RetransmitTimer() {
			super(conn.transport.ip.host.getNetwork().getScheduler(), RTO_TIMER, false);
		}
		
		@Override
		protected void run() throws Exception {
			retransmit();
			start();
		}
		
	}
	
	public GoBackN(Connection conn, int N, int localISN, int remoteISN) {
		this.conn= conn;
		this.N= N;
		baseSN= localISN;
		nextSN= baseSN;
		expSN= remoteISN;
		timer= new RetransmitTimer();
	}
	
	@Override
	public void handleData(int sn, Message m) throws Exception {
		if (sn == expSN) {
			expSN+= m.getByteLength();
			conn.transport.send(conn, Segment.createDATA(conn, nextSN, expSN, null));
			conn.notifyReceived(m);
		}
	}

	@Override
	public void handleACK(int sn) throws Exception {
		Segment s;
		if (sn > baseSN) {
			s= sendWindow.peek();
			while ((s != null) && (s.dataSN + s.payload.getByteLength() <= sn)) {
				sendWindow.poll();
				baseSN+= s.payload.getByteLength();
				s= sendWindow.peek();
			}
			if (sendWindow.size() == 0)
				timer.stop();
			if (sendWindow.size() < N)
				conn.notifyClearToSend();
		}
	}

	@Override
	public boolean send(Message m) throws Exception {
		if (sendWindow.size() >= N)
			return false;
		Segment s= Segment.createDATA(conn, nextSN, expSN, m);
		sendWindow.add(s);
		conn.transport.send(conn, s);
		nextSN+= m.getByteLength();
		if (sendWindow.size() == 1)
			timer.start();
		return true;
	}
	
	private void retransmit() throws Exception {
		// Retransmit all messages upon timer expiration
		for (Segment s: sendWindow) {
			System.out.println("Re-transmit " + s);
			conn.transport.send(conn, s);
		}
	}

	@Override
	public boolean clearToSend() {
		return (sendWindow.size() < N);
	}
	
}
