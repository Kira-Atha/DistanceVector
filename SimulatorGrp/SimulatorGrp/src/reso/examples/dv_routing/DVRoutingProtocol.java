/*******************************************************************************
 * Copyright (c) 2011 Bruno Quoitin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Bruno Quoitin - initial API and implementation
 ******************************************************************************/
package reso.examples.dv_routing;

import reso.common.AbstractApplication;
import reso.common.Interface;
import reso.common.InterfaceAttrListener;
import reso.ip.Datagram;
import reso.ip.IPAddress;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPInterfaceListener;
import reso.ip.IPLayer;
import reso.ip.IPLoopbackAdapter;
import reso.ip.IPRouter;

public class DVRoutingProtocol
extends AbstractApplication
implements IPInterfaceListener, InterfaceAttrListener {

	public static final String PROTOCOL_NAME= "DV_ROUTING";
	public static final int IP_PROTO_DV= Datagram.allocateProtocolNumber(PROTOCOL_NAME);
	
	
	private final IPLayer ip;
	private final boolean advertise;
	
	private final DVRoutingTable rt = new DVRoutingTable();
	
	/** Constructor
	 * 
	 * @param router is the router that hosts the routing protocol
	 * @param advertise specifies if this router will send DV for its own local destinations
	 */
	public DVRoutingProtocol(IPRouter router, boolean advertise) {
		super(router, PROTOCOL_NAME);
		this.ip= router.getIPLayer();
		this.advertise= advertise;
	}

	/**
	 * Elect the router ID, defined as the highest IP address of the router.
	 * The default behaviour is to use that IP address as the destination
	 * advertised by this router.
	 * 
	 * @return
	 */
	private IPAddress getRouterID() {
		IPAddress routerID= null;
		for (IPInterfaceAdapter iface: ip.getInterfaces()) {
			IPAddress addr= iface.getAddress();
			if (routerID == null)
				routerID= addr;
			else if (routerID.compareTo(addr) < 0)
				routerID= addr;
		}
		return routerID;
	}
	
	@Override
	public void start() throws Exception {
		// Register listener for datagrams with DV routing messages
		ip.addListener(IP_PROTO_DV, this);
		
		// Register interface attribute listeners to detect metric and status changes
		for (IPInterfaceAdapter iface: ip.getInterfaces())
			iface.addAttrListener(this);
		
		// Send initial DV
		if (advertise)
			sendToAll(getRouterID(), 0, null, null);
	}
	
	@Override
	public void stop() {
		ip.removeListener(IP_PROTO_DV, this);
		for (IPInterfaceAdapter iface: ip.getInterfaces())
			iface.removeAttrListener(this);
	}
	
	/**
	 *  Send the specified DV component (dst, cost) through all interfaces, in broadcast.
	 *  
	 * @param dst
	 * @param cost
	 * @param oif used to filter advertisements 
	 * @param neighbor used to filter advertisements
	 * @throws Exception
	 */
	private void sendToAll(IPAddress dst, int cost, IPInterfaceAdapter oif, IPAddress neighbor)
	throws Exception {
		for (IPInterfaceAdapter iface: ip.getInterfaces()) {
			if (iface instanceof IPLoopbackAdapter)
				continue;
			DVMessage dvm= new DVMessage();
			int advCost = cost;
			
			dvm.addDV(dst, advCost);
			iface.send(new Datagram(iface.getAddress(), IPAddress.BROADCAST, IP_PROTO_DV, 1, dvm), null);
		}
	}
	
	private void logHeader(Interface iface) {
		System.out.println("------------------------------------------------------------");
		System.out.print(((int) (host.getNetwork().getScheduler().getCurrentTime() * 1000)) + "ms " +
				host.name + " " + iface + " : ");
	}
	
	
	@Override
	public void receive(IPInterfaceAdapter iface, Datagram msg) {
		logHeader(iface);
		System.out.println(msg);
		
		DVMessage dvm= (DVMessage) msg.getPayload();
			
		for (DVMessage.DV dv: dvm.dvs) {

			// Filter local addresses
			if (ip.hasAddress(dv.dst)) {
				System.out.println("\troute to myself; discard !");
				continue;
			}
			
			// Get current DVs for this destination
			rt.updateEntry(dv.dst, msg.src, dv.metric, iface);
			
			updateDestination(dv.dst);
		}
	}
	
	@Override
	public void attrChanged(Interface iface, String attr) {
		logHeader(iface);
		System.out.println("attribute \"" + attr + "\" changed on interface \"" + iface + "\" : " +
				iface.getAttribute(attr));
    }

	/**
	 * This method computes the new best route for the given destination.
	 * Three different outcomes are handled:
	 *   (1) there is no more feasible route,
	 *   (2) the best route has changed,
	 *   (3) the best route has not changed.
	 * In cases (1) and (2), update messages are sent to the neighbors.
	 * 
	 * @param dst the destination for which the new route must be computed.
	 */
	private void updateDestination(IPAddress dst) {
		try {
		
			DVRoutingTable.Entry currentBest = rt.getBest(dst);
			if (currentBest != null)
				currentBest = new DVRoutingTable.Entry(currentBest);
			
			// Compute new best route, update RIB
			DVRoutingTable.Entry best= rt.computeBest(dst);
		
			// CASE (1) No more route => advertise infinite cost
			if (best == null) {
				if (currentBest != null) {
					System.out.println("\tno more route -> update !");
					ip.removeRoute(dst);
					sendToAll(dst, Integer.MAX_VALUE, null, null);
				} else
					System.out.println("\tno route");
				return;
			}
		
			// CASE (3) Same best route => no change
			if (best.equals(currentBest)) {
				System.out.println("\tbest route unchanged.");
				return;
			}
		
			// CASE (2) Best route has changed
			System.out.println("\tbest route changed -> update !");
				
			// ... update FIB ...
			ip.addRoute(new DVRoutingEntry(dst, best.oif, best.getCost()));
		
			// ... and propagate
			sendToAll(dst, best.getCost(), best.oif, best.neighbor);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
		
}
