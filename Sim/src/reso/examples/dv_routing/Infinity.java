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

import java.util.Collection;

import reso.common.Interface;
import reso.common.InterfaceAttrListener;
import reso.common.Network;
import reso.common.Node;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPInterfaceAdapter;
import reso.ip.IPLayer;
import reso.ip.IPRouteEntry;
import reso.ip.IPRouter;
import reso.scheduler.AbstractScheduler;
import reso.scheduler.Scheduler;
import reso.utilities.FIBDumper;
import reso.utilities.NetworkBuilder;

public class Infinity implements InterfaceAttrListener {
	
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
            /*
            System.out.println("Before metric update");
            scheduler.run();
            */  
            // Display forwarding table for each node
            FIBDumper.dumpForAllRouters(network);			
                
            // Change topology/nodes properties here ..
            try {
    			// Update metric R3 eth1 -> R1 eth1
    			((IPHost) network.getNodeByName("R3")).getIPLayer().getInterfaceByName("eth1").setMetric(300);
    			
			//TEST
    			System.out.println("New metric : " + ((IPHost) network.getNodeByName("R3")).getIPLayer().getInterfaceByName("eth1").getMetric());
    		}catch(Exception e) {
    			System.err.println(e.getMessage());
    			e.printStackTrace(System.err);
    		}
            
            // Run simulation for 10 sec 
            System.out.println("After metric update");
            //Reload with new metric
            setupRoutingProtocol(network, "R1");
            //launch test
            scheduler.runUntil(10);
                            
            // Display again forwarding table for each node
      
            System.out.println("Dump for all routers after metric updated");
            FIBDumper.dumpForAllRouters(network);
            
// 3.3 (STANDBY) => Test solution infinite count issue => Poisoned Reverse
           // Je veux modifier le coût à l'infini pour un chemin  R3 -> R4 par exemple ? 
            
            Node nodeR3 = network.getNodeByName("R3");
            
            IPHost ipHostR3 = (IPHost)nodeR3;
            IPLayer ipLayerR3 = ipHostR3.getIPLayer();
            //Collection<IPRouteEntry> allRoutes = ipLayerR3.getRoutes();
            

            //
            IPInterfaceAdapter interfaceR3 = ipLayerR3.getInterfaceByName("eth0");
           
            int infinity = (int) (Double.POSITIVE_INFINITY);
            
            // COMMENT SET LE COUT ????? CLASSE DVRoutineTable.java -> Static Class Entry -> updateLinkcost 
            DVRoutingTable.Entry entry = new DVRoutingTable.Entry(IPAddress.getByAddress(10,0,0,4),interfaceR3.getMetric(),interfaceR3);
            
    //PAS DU TOUT SUR??? Ce n'est pas la métrique je veux modifier mais bien le coût. UpdateLinkCost ne modifie pas le coût en soi !!!!
            interfaceR3.setMetric(infinity);
            entry.updateLinkCost();
            
          //Reload with new metric
            setupRoutingProtocol(network, "R1");
            //launch test
            System.out.println("Reverse poisoned???");
            scheduler.runUntil(10);
            
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	
	}

	@Override
	public void attrChanged(Interface iface, String attr) {
		
		
	}
}
