package unit731.hunlinter.gui.events;

import javax.swing.*;
import java.util.Objects;


public class TabbedPaneEnableEvent{

	private final JLayeredPane pane;
	private final boolean enable;


	public TabbedPaneEnableEvent(final boolean enable){
		pane = null;
		this.enable = enable;
	}

	public TabbedPaneEnableEvent(final JLayeredPane pane, final boolean enable){
		Objects.requireNonNull(pane);

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
