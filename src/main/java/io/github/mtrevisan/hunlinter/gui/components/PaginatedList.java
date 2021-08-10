/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
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

import io.github.mtrevisan.hunlinter.gui.models.SortableListModel;

import javax.swing.*;
import java.util.List;


/**
 * A paginated list. Only displays a specific number of rows and allows you to page backwards and forwards through the list
 * with the help of a toolbar.
 */
public class PaginatedList extends JList<String>{

	private final JList<String> list;
	private final int pageSize;

	private List<String> listData;
	private int currentPage;
	private int lastPage;


	/**
	 * @param pageSize	The number of rows visible in the JList.
	 */
	public PaginatedList(final int pageSize){
		super();

		this.list = new JList<>();
		this.pageSize = pageSize;
	}

	public void loadLines(final List<String> listData){
		this.listData = listData;
		//work out how many pages there are
		this.currentPage = 1;
		final int size = listData.size();
		this.lastPage = size / pageSize + (size % pageSize != 0? 1: 0);

		updatePage();
	}

	public void advanceCurrentPage(){
		currentPage = Math.min(currentPage + 1, lastPage);

		updatePage();
	}

	public void retreatCurrentPage(){
		currentPage = Math.max(currentPage - 1, 1);

		updatePage();
	}

	public void setFirstPage(){
		this.currentPage = 1;

		updatePage();
	}

	public void setLastPage(){
		this.currentPage = lastPage;

		updatePage();
	}

	private void updatePage(){
		//replace the list's model with a new model containing only the entries in the current page
		final int start = (currentPage - 1) * pageSize;
		final int end = Math.min(start + pageSize, listData.size());
		final SortableListModel page = new SortableListModel();
		page.addAll(listData.subList(start, end));
//		for(int i = start; i < end; i ++)
//			page.addElement(listData.get(i));
		list.setModel(page);
	}

	//	private JLabel countLabel;
	//	private JButton first;
	//	private JButton prev;
	//	private JButton next;
	//	private JButton last;
	//
	//	private void initializeButtons(final JList<T> list){
	//		setLayout(new BorderLayout());
	//		countLabel = new JLabel();
	//		add(countLabel, BorderLayout.NORTH);
	//		add(list, BorderLayout.CENTER);
	//		add(createControls(), BorderLayout.SOUTH);
	//	}
	//
	//	private JPanel createControls(){
	//		first = new JButton(new AbstractAction("<<"){
	//			public void actionPerformed(final ActionEvent e){
	//				paginatedList.setFirstPage();
	//
	//				updateButtons();
	//			}
	//		});
	//
	//		prev = new JButton(new AbstractAction("<"){
	//			public void actionPerformed(final ActionEvent e){
	//				paginatedList.retreatCurrentPage();
	//
	//				updateButtons();
	//			}
	//		});
	//
	//		next = new JButton(new AbstractAction(">"){
	//			public void actionPerformed(final ActionEvent e){
	//				paginatedList.advanceCurrentPage();
	//
	//				updateButtons();
	//			}
	//		});
	//
	//		last = new JButton(new AbstractAction(">>"){
	//			public void actionPerformed(final ActionEvent e){
	//				paginatedList.setLastPage();
	//
	//				updateButtons();
	//			}
	//		});
	//
	//		final JPanel bar = new JPanel(new GridLayout(1, 4));
	//		bar.add(first);
	//		bar.add(prev);
	//		bar.add(next);
	//		bar.add(last);
	//		return bar;
	//	}
	//
	//	private void updateButtons(){
	//		//update the label
	//		countLabel.setText("Page " + currentPage + "/" + lastPage);
	//
	//		//update buttons
	//		final boolean canGoBack = (currentPage != 1);
	//		final boolean canGoFwd = (currentPage != lastPage);
	//		first.setEnabled(canGoBack);
	//		prev.setEnabled(canGoBack);
	//		next.setEnabled(canGoFwd);
	//		last.setEnabled(canGoFwd);
	//	}

}
