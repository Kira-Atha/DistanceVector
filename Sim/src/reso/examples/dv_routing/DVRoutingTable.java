package reso.examples.dv_routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import reso.common.Link;
import reso.ethernet.EthernetFrame;
import reso.ip.IPAddress;
import reso.ip.IPEthernetAdapter;
import reso.ip.IPInterfaceAdapter;

public class DVRoutingTable {
	
	private static String findNeighborName(IPAddress neighbor, IPInterfaceAdapter oif) {
		Link<EthernetFrame> l = ((IPEthernetAdapter) oif).iface.getLink();
		if (l.getHead() != ((IPEthernetAdapter) oif).iface)
			return l.getHead().getNode().name;
		else
			return l.getTail().getNode().name;
	}
	
	private static String prettifyMetric(int value) {
		return (value == Integer.MAX_VALUE ? "inf" : Integer.toString(value));
	}
	
	public static class Entry {
		
		// neighbor which advertised the entry
		public final IPAddress neighbor;
		// cost as advertised by the neighbor, not including the link cost
		public final int neighborCost;
		
		// interface through which the entry was received
		public final IPInterfaceAdapter oif;
		// interface cost
		private int linkCost;
		
		public Entry(IPAddress neighbor, int cost, IPInterfaceAdapter oif) {
			this.neighbor = neighbor;
			this.neighborCost = cost;
			this.oif = oif;
		}
		
		// Copy constructor
		public Entry(Entry o) {
			this(o.neighbor, o.neighborCost, o.oif);
			linkCost = o.linkCost;
		}
				
		// Return the entry's cost, that is (neighbor cost + link cost)
		public int getCost() {
			return addMetric(neighborCost, linkCost);
		}
		
		// Update the cached link cost
		public void updateLinkCost() {
			if (oif.isActive())
				linkCost = oif.getMetric();
			else
				linkCost = Integer.MAX_VALUE;
		}
		
		public boolean equals(Entry o) {
			if (o == null)
				return false;
			return (o.neighbor == neighbor) && (o.neighborCost == neighborCost) &&
					(o.oif == oif) && (o.linkCost == linkCost);
		}
		
		public String toString() {
			String neighborName = findNeighborName(neighbor, oif);
			//return "(neighbor=" + neighbor + "," + neighborCost + ";oif=" + oif + "," + linkCost + ")";
			return "(" + neighborName + "," + prettifyMetric(neighborCost) + "," + prettifyMetric(linkCost) + ")";
		}
		
	}
	
	private class Entries {
		
		public Entry best;
		public Map<IPAddress,Entry> content;
		
		public Entries() {
			content = new HashMap<IPAddress,Entry>();
		}
		
	}
	
	private final Map<IPAddress,Entries> content=
		new HashMap<IPAddress,Entries>();
	
	/**
	 * Add a new entry (DV) into the routing table.
	 * 
	 * @param dst destination of the entry
	 * @param neighbor neighbor which advertised the entry
	 * @param oif interface through which the entry was learned
	 * @param dv the entry's cost
	 */
	public void updateEntry(IPAddress dst, IPAddress neighbor, int neighborCost, IPInterfaceAdapter oif) {
		Entries entries = content.get(dst);
		if (entries == null) {
			entries= new Entries();
			content.put(dst, entries);
		}
		entries.content.put(neighbor, new Entry(neighbor, neighborCost, oif));
	}
	
	/**
	 * Return the current best entry for a given destination.
	 * 
	 * @param dst
	 * @return
	 */
	public Entry getBest(IPAddress dst) {
		Entries entries = content.get(dst);
		if (entries == null)
			return null;
		return entries.best;
	}
	
	/**
	 * Compute the new current best route.
	 * 
	 * @param dst destination concerned by the computation
	 * @return the new best route entry
	 */
	public Entry computeBest(IPAddress dst) {
		Entries entries = content.get(dst);
		
		Entry best = null;
		int bestCost = 0;
		
		System.out.println("\told best " + (entries.best != null ? entries.best : "---") + "");
		
		for (Entry e: entries.content.values()) {
						
			// Update entry's link cost
			e.updateLinkCost();
			
			System.out.println("\t" + e);
			
			// Ignore max metric entries (infinity)
			if (e.getCost() == Integer.MAX_VALUE)
				continue;
			
			// Bellman-Ford
			if ((best == null) || (bestCost > e.getCost())) {
				best = e;
				bestCost = e.getCost();
			}
			
		}
		
		System.out.println("\tnew best " + (best != null ? best : "---") + "");
		
		entries.best = best;
		return best;
	}
	
	public Set<IPAddress> getDestinations() {
		return content.keySet();
	}

	public static int addMetric(int m1, int m2) {
		if (((long) m1) + ((long) m2) > Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		return m1 + m2;
	}
	
}
