package reso.examples.transport;

import reso.common.AbstractApplication;
import reso.common.Host;
import reso.common.Message;
import reso.ip.IPAddress;
import reso.ip.IPHost;

public class AppClient extends AbstractApplication implements ConnectionListener {
	
	private final Connection conn;
	private final IPAddress dst;
	private final int pdst;
	
	private static final String [] MSG= { "Hello", "World", "of", "TCP", "!", "How", "about", "sending", "more", "messages", "???" };
	private int index;
	
	public AppClient(Host host, IPAddress dst, int pdst) {
		super(host, "TCP client");
		this.dst= dst;
		this.pdst= pdst;
		ProtoTransport transport= new ProtoTransport(((IPHost) host).getIPLayer());
		conn= transport.create();
		conn.setListener(this);
	}

	public static class AppMessage implements Message {
		
		public final String data;
		
		public AppMessage(String data) {
			this.data= data;
		}
		
		@Override
		public String toString() {
			return data;
		}

		@Override
		public int getByteLength() {
			return data.length();
		}
		
	}
	
	@Override
	public void start() throws Exception {
		conn.connect(dst, pdst);
	}

	@Override
	public void stop() {
		conn.close();
	}

	@Override
	public void onConnected(Connection conn) {
		System.out.println(name + " :: Connection established.");
		try {
			while (conn.clearToSend())
				conn.send(new AppMessage(MSG[index++]));
		} catch (Exception e) {
			System.out.println(name + " :: Exception " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void onAccept(Connection conn, Connection cliConn) { }

	@Override
	public void onReceived(Connection conn, Message m) {
		System.out.println(name + " :: Message received " + m);
	}

	@Override
	public void onClose(Connection conn) {
		System.out.println(name + " :: Connection closed.");
	}

	@Override
	public void onClearToSend(Connection conn) throws Exception {
		while ((index < MSG.length) && conn.clearToSend())
			conn.send(new AppMessage(MSG[index++]));
	}

}
