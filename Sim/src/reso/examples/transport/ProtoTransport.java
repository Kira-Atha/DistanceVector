package reso.examples.transport;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import reso.ip.Datagram;
import reso.ip.IPAddress;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPInterfaceListener;
import reso.ip.IPLayer;

public class ProtoTransport implements IPInterfaceListener {

	public final IPLayer ip;
	
	public static final int IP_PROTO_TCP= Datagram.allocateProtocolNumber("TCP");
	
	private List<Connection> connections= new LinkedList<>();
	private Map<Integer,Connection> listeningConnections= new TreeMap<>();
	private int nextPort= 1024;
	
	public ProtoTransport(IPLayer ip) {
		this.ip= ip;
		ip.addListener(IP_PROTO_TCP, this);
	}
	
	public Connection create() {
		Connection conn= new Connection(this);
		connections.add(conn);
		return conn;
	}

	public Connection findConnection(IPAddress src, IPAddress dst, int psrc, int pdst) {
		for (Connection conn: connections)
			if ((conn.getLocalPort() == psrc) & (conn.getRemotePort() == pdst))
				return conn;
		return null;
	}
	
	public Connection findConnectionByPort(int pdst) {
		return listeningConnections.get(pdst);
	}
	
	private Connection accept(Connection conn, IPAddress src, IPAddress dst, int psrc, int pdst) {
		// TBD CHECK IF THERE IS NO IDENTICAL CONNECTION
		Connection cliConn= new Connection(this, dst, src, pdst, psrc);
		connections.add(cliConn);
		conn.notifyAccept(cliConn);
		return cliConn;
	}
	
	@Override
	public void receive(IPInterfaceAdapter src, Datagram datagram)
			throws Exception {
		// De-multiplexing
		Segment s= (Segment) datagram.getPayload();
		Connection conn;
		if (s.isSYN() & !s.isACK()) {
			conn= findConnectionByPort(s.dstPort);
			if (conn == null)
				throw new Exception("No listening connection on port " + s.dstPort);
			conn= accept(conn, datagram.src, datagram.dst, s.srcPort, s.dstPort);
		} else
			conn= findConnection(datagram.dst, datagram.src, s.dstPort, s.srcPort);
		conn.recv(s);
	}
	
	/* Bind a client connection to a port */
	public int bind(Connection conn) {
		return nextPort++;
	}
	
	/* Bind a listening connection to a port */
	public void bind(Connection conn, int psrc) throws Exception {
		if (listeningConnections.containsKey(psrc))
			throw new Exception("Port " + psrc + " already in use");
		listeningConnections.put(psrc, conn);
	}
	
	public void send(Connection conn, Segment s) throws Exception {
		ip.send(conn.getLocalAddr(), conn.getRemoteAddr(), ProtoTransport.IP_PROTO_TCP, s);
	}
	
}
