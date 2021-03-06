
/*
 Copyright (c) 2006, 2007, The Cytoscape Consortium (www.cytoscape.org)

 The Cytoscape Consortium is:
 - Institute for Systems Biology
 - University of California San Diego
 - Memorial Sloan-Kettering Cancer Center
 - Institut Pasteur
 - Agilent Technologies

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications.  In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage.  See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/

package cytoscape.widgets.autocomplete.view;

import cytoscape.widgets.autocomplete.index.TextIndex;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;


/**
 * JComboBox, which provides a built-in auto complete feature.
 * <p/>
 * Most of this code is based on:
 * <A href="http://www.orbital-computer.de/JComboBox/">
 * http://www.orbital-computer.de/JComboBox/</A>, which is in the public
 * domain.
 * <p/>
 *
 * @author Ethan Cerami.
 */
public class TextIndexComboBox extends JComboBox {
	private AutoCompleteDocument doc;
	private TextIndex textIndex;
	private boolean firingFinalSelectionEvent;
	private double popupWindowSizeMultiple;

	/**
	 * A list of final selection event listeners for this component.
	 */
	protected EventListenerList finalSelectionListenerList = new EventListenerList();

	/**
	 * Default:  Maximum Row Count Visible
	 */
	public static final int DEFAULT_MAX_ROWS_VISIBLE = 5;

	/**
	 * Default:  Max number of hits to show in pull-down menu
	 */
	public static final int DEFAULT_MAX_HITS_SHOWN = 20;

	/**
	 * Package-Level Constructor.
	 * <P>To instantiate a TextIndexComoboBox, you must use the
	 * {@link
	 * ComboBoxFactory#createTextIndexComboBox
	 * (csplugins.widgets.autocomplete.index.TextIndex)}
	 * method.
	 *
	 * @param textIndex               TextIndex Object.
	 * @param popupWindowSizeMultiple Indicates the size multiple used
	 *                                to resize the popup window.
	 */
	TextIndexComboBox(TextIndex textIndex, double popupWindowSizeMultiple) {
		this.textIndex = textIndex;
		this.popupWindowSizeMultiple = popupWindowSizeMultiple;
		init();

		//  Populate menu with first X items
		doc.populatePullDownMenu("");
	}

	/**
	 * Sets the Text Index Object.
	 *
	 * @param textIndex TextIndex Object.
	 */
	public void setTextIndex(TextIndex textIndex) {
		this.textIndex = textIndex;

		//  Remove Existing Text in Text Box
		try {
			doc.removeAllText();
		} catch (BadLocationException e) {
		}

		//  Populate menu with first X items
		doc.populatePullDownMenu("");
	}

	/**
	 * Removes all text from combo box.
	 */
	public void removeAllText() {
		try {
			doc.removeAllText();
		} catch (BadLocationException e) {
		}
	}

	/**
	 * Gets the Text Index Object.
	 *
	 * @return Text Index Object.
	 */
	public TextIndex getTextIndex() {
		return this.textIndex;
	}

	/**
	 * Sets selected item.
	 *
	 * @param object Selected Item Object.
	 */
	public void setSelectedItem(Object object) {
		super.setSelectedItem(object);

		//  Only fire final selection event if this is a mouse selection
		if (!doc.getSelecting() && !doc.getCursorKeyPressed()) {
			this.fireFinalSelectionEvent();
		}
	}

	/**
	 * Adds an <code>ActionListener</code>.
	 * <p/>
	 * The <code>ActionListener</code> will receive an <code>ActionEvent</code>
	 * when a <b>final</b> selection has been made by the user. A final
	 * selection occurs when:
	 * <UL>
	 * <LI>A user hits Enter; or
	 * <LI>A user clicks on an item in the pull-down menu.
	 * </UL>
	 *
	 * @param l the <code>ActionListener</code> that is to be notified
	 * @see #setSelectedItem
	 */
	public void addFinalSelectionListener(ActionListener l) {
		finalSelectionListenerList.add(ActionListener.class, l);
	}

	/**
	 * Returns an array of all the <code>ActionListener</code>s added
	 * to this TextComboBox with addFinalSelectionListener().
	 *
	 * @return all of the <code>ActionListener</code>s added or an empty
	 *         array if no listeners have been added
	 * @since 1.4
	 */
	public ActionListener[] getFinalSelectionListeners() {
		return (ActionListener[]) finalSelectionListenerList.getListeners(ActionListener.class);
	}

	/**
	 * Removes an <code>ActionListener</code> added to this TextComboBox
	 * with addFinalSelectionListener().
	 *
	 * @param l the <code>ActionListener</code> to remove
	 */
	public void removeFinalSelectionListener(ActionListener l) {
		finalSelectionListenerList.remove(ActionListener.class, l);
	}

	/**
	 * Notifies all listeners that have registered interest for
	 * notification on this event type.
	 *
	 * @see EventListenerList
	 */
	protected void fireFinalSelectionEvent() {
		if (!firingFinalSelectionEvent) {
			// Set flag to ensure that an infinite loop is not created
			firingFinalSelectionEvent = true;

			ActionEvent e = null;

			// Guaranteed to return a non-null array
			Object[] listeners = finalSelectionListenerList.getListenerList();
			long mostRecentEventTime = EventQueue.getMostRecentEventTime();
			int modifiers = 0;
			AWTEvent currentEvent = EventQueue.getCurrentEvent();

			if (currentEvent instanceof InputEvent) {
				modifiers = ((InputEvent) currentEvent).getModifiers();
			} else if (currentEvent instanceof ActionEvent) {
				modifiers = ((ActionEvent) currentEvent).getModifiers();
			}

			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == ActionListener.class) {
					// Lazily create the event:
					if (e == null) {
						e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand(),
						                    mostRecentEventTime, modifiers);
					}

					((ActionListener) listeners[i + 1]).actionPerformed(e);
				}
			}

			firingFinalSelectionEvent = false;
		}
	}

	/**
	 * Initializes the Component.
	 */
	private void init() {
		//  Enable Editing on ComboBox
		setEditable(true);

		//  Set Max Rows Displayed
		setMaximumRowCount(DEFAULT_MAX_ROWS_VISIBLE);

		//  Set Custom Document for processing user input
		final JTextComponent editor = (JTextComponent) getEditor().getEditorComponent();
		doc = new AutoCompleteDocument(this);
		editor.setDocument(doc);

		//  Set Custom Renderer
		setRenderer(new TextBoxRenderer(this, popupWindowSizeMultiple));
		setBackground(Color.WHITE);

		//  Add Hit Listener, to auto pop-up the menu, and to detect
		//  specific keystrokes.
		KeyAdapter editorKeyListener = new UserKeyListener(doc, editor, this);
		editor.addKeyListener(editorKeyListener);

		//  Add PopupMenuListener, in order to resize size of popup window
		addPopupMenuListener(new UserPopupListener(popupWindowSizeMultiple));
	}
}


/**
 * KeyListener for listening to specific key press / release events.
 *
 * @author Ethan Cerami
 */
class UserKeyListener extends KeyAdapter {
	private AutoCompleteDocument doc;
	private JTextComponent editor;
	private TextIndexComboBox comboBox;

	/**
	 * Constructor.
	 *
	 * @param doc      Document.
	 * @param editor   Editor.
	 * @param comboBox JComboBox.
	 */
	UserKeyListener(AutoCompleteDocument doc, JTextComponent editor, TextIndexComboBox comboBox) {
		this.doc = doc;
		this.editor = editor;
		this.comboBox = comboBox;
	}

	/**
	 * User has pressed a key.
	 *
	 * @param e KeyEvent Object.
	 */
	public void keyPressed(KeyEvent e) {
		doc.setHitBackspace(false);

		switch (e.getKeyCode()) {
			// determine if the pressed key is backspace
			// (needed by the remove method)
			case KeyEvent.VK_BACK_SPACE:
				doc.setHitBackspace(true);
				doc.setHitBackspaceOnSelection(editor.getSelectionStart() != editor.getSelectionEnd());

				break;

			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_PAGE_DOWN:
			case KeyEvent.VK_PAGE_UP:
				doc.setCursorKeyPressed(true);

				break;

			case KeyEvent.VK_ENTER:
				comboBox.fireFinalSelectionEvent();

				break;
		}
	}

	/**
	 * User has released a key.
	 *
	 * @param e KeyEvent Object.
	 */
	public void keyReleased(KeyEvent e) {
		if ((e.getKeyCode() == KeyEvent.VK_UP) || (e.getKeyCode() == KeyEvent.VK_DOWN)
		    || (e.getKeyCode() == KeyEvent.VK_PAGE_UP) || (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN)) {
			doc.setCursorKeyPressed(false);
		}
	}
}


/**
 * PopupMenu Listener.
 * <p/>
 * Hack used to make the size of the pop-up menu bigger than the size
 * of the JComboBox, to which it is attached.
 * Hack is described here:
 * http://forums.java.net/jive/thread.jspa?threadID=9017&messageID=61267
 *
 * @author Ethan Cerami.
 */
class UserPopupListener implements PopupMenuListener {
	private double popupWindowSizeMultiple;

	/**
	 * Constructor.
	 *
	 * @param popupWindowSizeMultiple Indicates the size multiple used
	 *                                to resize the popup window.
	 */
	public UserPopupListener(double popupWindowSizeMultiple) {
		this.popupWindowSizeMultiple = popupWindowSizeMultiple;
	}

	/**
	 * PopupMenu Will Become Visible:  Resize.
	 *
	 * @param e PopupMenuEvent.
	 */
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		JComboBox box = (JComboBox) e.getSource();
		Object comp = box.getUI().getAccessibleChild(box, 0);

		if (!(comp instanceof JPopupMenu)) {
			return;
		}

		JPopupMenu popUpMenu = (JPopupMenu) comp;
		//  Set to Flow Layout, fixes bug in Mac OS X, Java 1.5
		popUpMenu.setLayout(new FlowLayout(FlowLayout.LEFT));

		//  Set size of ScrollPane
		JScrollPane scrollPane = (JScrollPane) popUpMenu.getComponent(0);
		scrollPane.setOpaque(true);

		Dimension size = scrollPane.getPreferredSize();
		size.width = (int) (size.width * popupWindowSizeMultiple);
		scrollPane.setPreferredSize(size);
	}

	/**
	 * PopupMenu Will Become Invisible:  No-op.
	 *
	 * @param e PopupMenuEvent.
	 */
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		//  No-op
	}

	/**
	 * PopupMenu Canceled:  No-op.
	 *
	 * @param e PopupMenuEvent.
	 */
	public void popupMenuCanceled(PopupMenuEvent e) {
		//  No-op
	}
}
