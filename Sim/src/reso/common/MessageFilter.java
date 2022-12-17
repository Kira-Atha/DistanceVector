package reso.common;

public interface MessageFilter<M> {

	boolean accept(M m);
}
