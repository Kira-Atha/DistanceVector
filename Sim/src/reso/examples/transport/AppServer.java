package reso.examples.transport;

import reso.common.AbstractApplication;
import reso.common.Host;
import reso.common.Message;
import reso.ip.IPHost;

public class AppServer extends AbstractApplication {

	private final Connection conn;
	private final int psrc;
	
	public AppServer(Host host, int psrc){
		super(host, "TCP server");
		this.psrc= psrc;
		ProtoTransport transport= new ProtoTransport(((IPHost) host).getIPLayer());
		conn= transport.create();
		conn.setListener(new MyConnectionListener());
	}

	@Override
	public void start() throws Exception {
		conn.listen(psrc);		
	}

	@Override
	public void stop() {
		conn.close();
	}
	
	private class MyConnectionListener implements ConnectionListener {

		@Override
		public void onConnected(Connection conn) {
			System.out.println(name + " :: Connection established");
		}

		@Override
		public void onAccept(Connection conn, Connection cliConn) {
			System.out.println(name + " :: New connection " + cliConn);
			cliConn.setListener(new MyConnectionListener());
		}

		@Override
		public void onReceived(Connection conn, Message m) {
			System.out.println(name + " :: Message received \"" + m + "\"");
		}

		@Override
		public void onClose(Connection conn) {
			System.out.println(name + " :: Connection closed.");
		}

		@Override
		public void onClearToSend(Connection conn) throws Exception { }

	}

}
