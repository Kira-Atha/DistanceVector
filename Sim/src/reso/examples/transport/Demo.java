package reso.examples.transport;

import reso.common.Link;
import reso.common.MessageFilter;
import reso.common.Network;
import reso.ethernet.EthernetAddress;
import reso.ethernet.EthernetFrame;
import reso.ethernet.EthernetInterface;
import reso.examples.static_routing.AppSniffer;
import reso.ip.IPAddress;
import reso.ip.IPHost;
import reso.ip.IPRouter;
import reso.scheduler.AbstractScheduler;
import reso.scheduler.Scheduler;
import reso.utilities.NetworkBuilder;

public class Demo {

	public static void main(String [] args) {
		AbstractScheduler scheduler= new Scheduler();
		Network network= new Network(scheduler);
		try {
			final EthernetAddress MAC_ADDR1= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x28);
    		final EthernetAddress MAC_ADDR2= EthernetAddress.getByAddress(0x00, 0x26, 0x91, 0x9f, 0xa9, 0x68);
    		final EthernetAddress MAC_ADDR3= EthernetAddress.getByAddress(0x00, 0x26, 0x91, 0x9f, 0xa9, 0x69);
    		final EthernetAddress MAC_ADDR4= EthernetAddress.getByAddress(0x00, 0x26, 0xbb, 0x4e, 0xfc, 0x29);
    		final IPAddress IP_ADDR1= IPAddress.getByAddress(192, 168, 0, 1);
    		final IPAddress IP_ADDR2= IPAddress.getByAddress(192, 168, 0, 2);
    		final IPAddress IP_ADDR3= IPAddress.getByAddress(192, 168, 1, 1);
    		final IPAddress IP_ADDR4= IPAddress.getByAddress(192, 168, 1, 2);

    		IPHost host1= NetworkBuilder.createHost(network, "H1", IP_ADDR1, MAC_ADDR1);
    		host1.getIPLayer().addRoute(IP_ADDR2, "eth0");
    		host1.addApplication(new AppSniffer(host1, new String [] { "eth0" }));
    		host1.addApplication(new AppClient(host1, IP_ADDR4, 80));
    		
    		IPHost host2= NetworkBuilder.createHost(network, "H2", IP_ADDR4, MAC_ADDR4);
    		host2.getIPLayer().addRoute(IP_ADDR3, "eth0");
    		host2.addApplication(new AppServer(host2, 80));
    		
    		IPRouter router= NetworkBuilder.createRouter(network, "R1",
    				new IPAddress [] { IP_ADDR2, IP_ADDR3 },
    				new EthernetAddress [] { MAC_ADDR2, MAC_ADDR3 });
    		
    		Link<EthernetFrame> l1= new Link<EthernetFrame>((EthernetInterface) host1.getInterfaceByName("eth0"),
    				(EthernetInterface) router.getInterfaceByName("eth0"), 5000000, 100000);
    		//l1.setFilter(new DialogMessageFilter<EthernetFrame>());
    		l1.setFilter(new ListMessageFilter(8));
    		new Link<EthernetFrame>((EthernetInterface) router.getInterfaceByName("eth1"),
    				(EthernetInterface) host2.getInterfaceByName("eth0"), 5000000, 100000);

    		// Add static routes
    		host1.getIPLayer().addRoute(IP_ADDR4, IP_ADDR2);
    		host2.getIPLayer().addRoute(IP_ADDR1, IP_ADDR3);
    		router.getIPLayer().addRoute(IP_ADDR4, "eth1");
    		router.getIPLayer().addRoute(IP_ADDR1, "eth0");
    		
    		host1.start();
    		host2.start();
    		
    		scheduler.runUntil(20);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
}
