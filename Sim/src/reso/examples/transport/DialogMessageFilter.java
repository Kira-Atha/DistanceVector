package reso.examples.transport;

import javax.swing.JOptionPane;

import reso.common.MessageFilter;

public class DialogMessageFilter<M> implements MessageFilter<M> {

	@Override
	public boolean accept(M m) {
		return JOptionPane.showConfirmDialog(
			    null,
			    "Accept " + m,
			    "Message filter",
			    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

}
