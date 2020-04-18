package unit731.hunlinter.gui;

import javax.swing.*;


public class TabbedPaneEnableEvent{

	private final JLayeredPane pane;
	private final boolean enable;


	public TabbedPaneEnableEvent(final boolean enable){
		this(null, enable);
	}

	public TabbedPaneEnableEvent(final JLayeredPane pane, final boolean enable){
		this.pane = pane;
		this.enable = enable;
	}

	public JLayeredPane getPane(){
		return pane;
	}

	public boolean isEnable(){
		return enable;
	}

}
