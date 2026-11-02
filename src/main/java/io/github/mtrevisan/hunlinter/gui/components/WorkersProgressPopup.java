/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.gui.components;

import io.github.mtrevisan.hunlinter.gui.MultiProgressBarUI;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Aggregates the progress of all the running workers into the main progress bar, and shows the list of the running workers (each with
 * its own progress bar) when the main progress bar is clicked, like IntelliJ does.
 * <p>
 * NOTE: all the methods have to be called from the event dispatch thread.
 * </p>
 */
public class WorkersProgressPopup{

	private static final int ROW_WIDTH = 320;

	private final JProgressBar mainProgressBar;
	private final Consumer<Object> onCancel;
	private final JPopupMenu popup = new JPopupMenu();

	/** The running workers (keyed by identity), in order of start. */
	private final Map<Object, Row> rows = new LinkedHashMap<>(0);
	/** Workers that terminated during the current batch (a batch ends when no worker is running anymore). */
	private int finishedInBatch;
	/** The value shown by the main progress bar (monotonic inside a batch). */
	private int displayedProgress;


	/**
	 * @param mainProgressBar	The progress bar that shows the overall progress.
	 * @param onCancel	Called (on the event dispatch thread, after the popup has been closed) with the worker whose cancel button has
	 *							been pressed.
	 */
	public WorkersProgressPopup(final JProgressBar mainProgressBar, final Consumer<Object> onCancel){
		Objects.requireNonNull(mainProgressBar, "Main progress bar cannot be null");
		Objects.requireNonNull(onCancel, "On cancel cannot be null");

		this.onCancel = onCancel;
		this.mainProgressBar = mainProgressBar;

		popup.setFocusable(false);
		mainProgressBar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		mainProgressBar.setToolTipText("Click to show the running tasks");
		mainProgressBar.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent evt){
				toggle();
			}
		});
	}

	/**
	 * Register (if not already known) the worker and update its progress.
	 *
	 * @param worker	The worker (identity is used as key).
	 * @param name	The name to be shown.
	 * @param progress	The progress of the worker [0, 100].
	 */
	public final void update(final Object worker, final String name, final int progress){
		Row row = rows.get(worker);
		if(row == null){
			//a new batch starts when nothing was running
			if(rows.isEmpty()){
				finishedInBatch = 0;
				displayedProgress = 0;
			}

			row = new Row(name, () -> {
				popup.setVisible(false);
				onCancel.accept(worker);
			});
			rows.put(worker, row);
			rebuild();
		}
		row.setProgress(progress);

		refreshMainProgressBar();
	}

	/** The worker is terminated (completed, failed or cancelled): it counts as 100% for the overall progress. */
	public final void finish(final Object worker){
		final Row row = rows.remove(worker);
		if(row == null)
			return;

		finishedInBatch ++;
		//keep the progress at the value the batch had so far, the bar will show 100 only when everything is completed
		refreshMainProgressBar();

		if(rows.isEmpty())
			popup.setVisible(false);
		else
			rebuild();
	}

	/** @return	The overall progress [0, 100]: the average of the progresses of all the workers of the current batch. */
	public final int getOverallProgress(){
		final int total = rows.size() + finishedInBatch;
		if(total == 0)
			return 0;

		long sum = 100l * finishedInBatch;
		for(final Row row : rows.values())
			sum += row.progress;
		return (int)(sum / total);
	}

	public final boolean isRunning(){
		return !rows.isEmpty();
	}


	private void refreshMainProgressBar(){
		//NOTE: the value never decreases inside a batch (a worker joining later, or a worker restarting its progress for a further step,
		//would otherwise make the bar jump back); it restarts from zero only when a new batch begins
		displayedProgress = Math.max(displayedProgress, getOverallProgress());
		mainProgressBar.setValue(displayedProgress);
		mainProgressBar.setToolTipText(rows.size() > 1
			? "Click to show the running tasks (" + rows.size() + " running)"
			: "Click to show the running tasks");
	}

	private void toggle(){
		if(popup.isVisible()){
			popup.setVisible(false);
			return;
		}

		rebuild();
		show();
	}

	private void show(){
		//show the popup above the progress bar (which sits at the bottom of the window), aligned to its right edge
		final Dimension size = popup.getPreferredSize();
		popup.show(mainProgressBar, mainProgressBar.getWidth() - size.width, -size.height);
	}

	private void rebuild(){
		popup.removeAll();

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		if(rows.isEmpty()){
			final JLabel none = new JLabel("No running tasks");
			none.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			content.add(none);
		}
		else
			for(final Row row : rows.values())
				content.add(row.panel);
		popup.add(content);
		popup.pack();

		if(popup.isVisible())
			show();
	}


	private static final class Row{
		private final JPanel panel = new JPanel();
		private final JProgressBar bar = new JProgressBar(0, 100);
		private int progress;


		private Row(final String name, final Runnable cancelAction){
			bar.setForeground(MultiProgressBarUI.MAIN_COLOR);
			bar.setUI(new MultiProgressBarUI());
			bar.setPreferredSize(new Dimension(ROW_WIDTH, 15));

			final JLabel label = new JLabel(name, SwingConstants.LEFT);
			label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			bar.setAlignmentX(JComponent.LEFT_ALIGNMENT);

			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			panel.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
			panel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			final JButton cancel = new JButton("✕");
			cancel.setToolTipText("Cancel this task");
			cancel.setFocusable(false);
			cancel.setMargin(new java.awt.Insets(0, 4, 0, 4));
			cancel.addActionListener(evt -> cancelAction.run());

			final JPanel barRow = new JPanel(new java.awt.BorderLayout(6, 0));
			barRow.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			barRow.add(bar, java.awt.BorderLayout.CENTER);
			barRow.add(cancel, java.awt.BorderLayout.EAST);

			panel.add(label);
			panel.add(barRow);
		}

		private void setProgress(final int progress){
			this.progress = progress;
			bar.setValue(progress);
		}
	}

}
