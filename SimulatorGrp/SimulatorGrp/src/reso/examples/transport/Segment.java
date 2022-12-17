package reso.examples.transport;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import reso.common.Message;

public class Segment implements Message {

	public static final int HEADER_LEN= 20;
	
	public final int srcPort, dstPort; 
	public final int dataSN;
	public final int ackSN;
	enum Flag { ACK, FIN, RST, SYN };
	private final Set<Flag> flags;
	public final Message payload;
	
	private Segment(Connection conn, Message payload,
			       int dataSN, int ackSN, Flag... flags) {
		srcPort= conn.getLocalPort();
		dstPort= conn.getRemotePort();
		
		this.dataSN= dataSN;
		this.ackSN= ackSN;
		this.flags= new TreeSet<>(Arrays.asList(flags));
		
		this.payload= payload;
	}
	
	public static Segment createSYN(Connection conn, int dataSN) {
		return new Segment(conn, null, dataSN, 0, Flag.SYN);
	}

	public static Segment createSYNACK(Connection conn, int dataSN, int ackSN) {
		return new Segment(conn, null, dataSN, ackSN, Flag.ACK, Flag.SYN);
	}
	
	public static Segment createDATA(Connection conn, int dataSN, int ackSN, Message payload) {
		return new Segment(conn, payload, dataSN, ackSN, Flag.ACK);
	}
	
	public static Segment createACK(Connection conn, int dataSN, int ackSN) {
		return createDATA(conn, dataSN, ackSN, null);
	}

	public boolean isACK() {
		return flags.contains(Flag.ACK);
	}
	
	public boolean isFIN() {
		return flags.contains(Flag.FIN);
	}

	public boolean isRST() {
		return flags.contains(Flag.RST);
	}

	public boolean isSYN() {
		return flags.contains(Flag.SYN);
	}
	
	@Override
	public int getByteLength() {
		return HEADER_LEN + (payload == null ? 0 : payload.getByteLength());
	}
	
	@Override
	public String toString() {
		return "TCPSegment [" +
				" sport=" + srcPort +
				" dport=" + dstPort +
				(isACK() ? " ACK" : "") +
				(isFIN() ? " FIN" : "") +
				(isRST() ? " RST" : "") +
				(isSYN() ? " SYN" : "") +
				" SN=" + dataSN +
				(isACK() ? " ACK-SN=" + ackSN : "") +
				(payload != null ? " data=[" + payload + "]": "") +
				" ]";
	}
	
}