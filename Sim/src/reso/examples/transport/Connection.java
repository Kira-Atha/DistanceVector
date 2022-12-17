package reso.examples.transport;

import reso.common.Message;
import reso.examples.transport.rdt.GoBackN;
import reso.examples.transport.rdt.RDT;
import reso.ip.IPAddress;

public class Connection {

	enum State { IDLE, LISTENING, SYN_SENT, SYN_RCVD, ESTABLISHED, FIN_SENT, CLOSED };

	// Connection state
	private State state= State.IDLE;	
	private int expSN= 0;
	private int nextSN= 0;
	
	// Identifier of connection
	private IPAddress localAddr;
	private IPAddress remoteAddr;
	private int localPort;
	private int remotePort;
	
	private ConnectionListener listener;
	
	private RDT rdt;
	
	
	public final ProtoTransport transport;
		
	public Connection(ProtoTransport transport) {
		this.transport= transport;
	}
	
	public Connection(ProtoTransport transport,
			          IPAddress localAddr, IPAddress remoteAddr,
			          int localPort, int remotePort) {
		this.transport= transport;
		this.localAddr= localAddr;
		this.remoteAddr= remoteAddr;
		this.localPort= localPort;
		this.remotePort= remotePort;
		state= State.LISTENING;
	}
	
	public void setListener(ConnectionListener l) {
		listener= l;
	}
	
	@Override
	public String toString() {
		return "Connection [" +
				" local=" + localAddr + ":" + localPort +
				" remote=" + remoteAddr + ":" + remotePort +
				" ]";
	}

	///// Connection Identification /////
	
	public IPAddress getRemoteAddr() {
		return remoteAddr;
	}
	
	public IPAddress getLocalAddr() {
		return localAddr;
	}
	
	public int getRemotePort() {
		return remotePort;
	}

	public int getLocalPort() {
		return localPort;
	}
		
	
	///// Connection services /////
	
	public void listen(int psrc) throws Exception {
		transport.bind(this, psrc);
		this.localAddr= IPAddress.ANY;
		this.localPort= psrc;
		state= State.LISTENING;
	}
	
	public void connect(IPAddress dst, int pdst) throws Exception {
		this.localAddr= IPAddress.ANY;
		this.remoteAddr= dst;
		this.remotePort= pdst;
		this.localPort= transport.bind(this);
		transport.send(this, Segment.createSYN(this, nextSN));
		nextSN++; // although SYN segment contains no data, it counts as 1
		state= State.SYN_SENT;
	}
	
	public boolean clearToSend() {
		return rdt.clearToSend();
	}
	
	public void notifyAccept(Connection cliConn) {
		if (listener == null)
			return;
		listener.onAccept(this, cliConn);
	}
	
	public void notifyClearToSend() throws Exception {
		if (listener == null)
			return;
		listener.onClearToSend(this);
	}
	
	public void notifyConnected() {
		if (listener == null)
			return;
		listener.onConnected(this);
	}
	
	public void notifyReceived(Message m) {
		if (listener == null)
			return;
		listener.onReceived(this, m);
	}
	
	public void close() {
		// send FIN message
	}
	
	public void send(Message msg) throws Exception {
		if (state != State.ESTABLISHED)
			throw new Exception("Connection not established");
		rdt.send(msg);
	}
		
	void recv(Segment s) throws Exception {
		switch (state) {
		
		case LISTENING:
			if (s.isSYN() & !s.isACK()) {
				// always accept ?
				// limit pending ?
				
				// Received SN = initial SN of client
				expSN= s.dataSN + 1;
				transport.send(this, Segment.createSYNACK(this, nextSN, expSN));
				nextSN++; // although SYN segment contains no data, it counts as 1
				state= State.SYN_RCVD;
			} else
				System.out.println("Invalid segment received in state " + state + " : " + s);
			break;
			
		case SYN_SENT:
			if (s.isACK() & s.isSYN()) {
				// Received SN = initial SN of server
				expSN= s.dataSN + 1;
				transport.send(this, Segment.createACK(this, nextSN, expSN));
				state= State.ESTABLISHED;
				//rdt= new SimpleRDT(this, nextSN, expSN);
				rdt= new GoBackN(this, 4, nextSN, expSN);
				notifyConnected();
			} else if (s.isRST())
				System.out.println("Connection closed (RST received)");
			else
				System.out.println("Invalid segment received in state " + state + " : " + s);
			break;
			
		case SYN_RCVD:
			if (s.isACK() & !s.isSYN()) {
				state= State.ESTABLISHED;
				//rdt= new SimpleRDT(this, nextSN, expSN);
				rdt= new GoBackN(this, 4, nextSN, expSN);
				notifyConnected();
			} 
			else
				System.out.println("Invalid segment received in state " + state + " : " + s);			
			break;
			
		case ESTABLISHED:
			if (s.isFIN()) {
				state= State.CLOSED;
			} else if (s.isRST()) {
				state= State.CLOSED;
			} else
				if (s.payload != null)
					rdt.handleData(s.dataSN, s.payload);
				rdt.handleACK(s.ackSN);
			break;
			
		default:
			System.out.println("Invalid segment received in state " + state + " : " + s);
			break;
			
		}
	}
		
}