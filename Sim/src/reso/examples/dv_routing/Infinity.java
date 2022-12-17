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

import reso.common.Network;
import reso.common.Node;
import reso.ip.IPAddress;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPLayer;
import reso.ip.IPRouter;
import reso.scheduler.AbstractScheduler;
import reso.scheduler.Scheduler;
import reso.utilities.FIBDumper;
import reso.utilities.NetworkBuilder;

public class Infinity {
	
	private static IPAddress getRouterID(IPLayer ip) {
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
	
	/**
	 * Configure the routing protocol on every router.
	 * If routerDst is not null, configure a single router as destination.
	 * 
	 * @param network
	 * @param routerDst
	 * @throws Exception
	 */
	private static void setupRoutingProtocol(Network network, String routerDst)
		throws Exception
	{
		for (Node n: network.getNodes()) {
			if (!(n instanceof IPRouter))
				continue;
			IPRouter router= (IPRouter) n;
			boolean advertise= (routerDst == null) || (n.name.equals(routerDst));
			router.addApplication(new DVRoutingProtocol(router, advertise));
			router.start();
		}
	}
	
	public static void main(String [] args) {
		try {
            String filename= "src/reso/data/demo-graph.txt";
            AbstractScheduler scheduler= new Scheduler();
            Network network= NetworkBuilder.loadTopology(filename, scheduler);
            setupRoutingProtocol(network, "R1");
                
            // Run simulation -- first convergence
            scheduler.run();
                
            // Display forwarding table for each node
            FIBDumper.dumpForAllRouters(network);			
                
            // Change topology/nodes properties here ..

            // Run simulation for 0.1 sec 
            scheduler.runUntil(0.100);
                            
            // Display again forwarding table for each node
            FIBDumper.dumpForAllRouters(network);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	/*
	 // Soluce proposée pour aider : 
	public static final String TOPO_FILE = "reso/data/topology.txt";
	public static void main(String [] args) {
		String filename = Demo.class.getClassLoader().getResource(TOPO_FILE).getFile();
		AbstractScheduler scheduler = new Scheduler();
		
		try {
			Network network = NetworkBuilder.loadTopology(filename, scheduler);
			
			((IPHost) network.getNodeByName("R2")).getIPLayer().getInterfaceByName("eth0").setMetric(200);
		}catch(Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
		}
	}
	*/
}
