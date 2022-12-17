package reso.examples.transport;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import reso.common.MessageFilter;

public class ListMessageFilter<M> implements MessageFilter<M> {

	private final Set<Integer> indices;
	private int index= 0;
	
	public ListMessageFilter(Integer... indices) {
		this.indices= new HashSet<Integer>(Arrays.asList(indices));
	}
	
	@Override
	public boolean accept(M m) {
		return (!indices.contains(index++));
	}

}
