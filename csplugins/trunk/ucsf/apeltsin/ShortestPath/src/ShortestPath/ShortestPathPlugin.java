


import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.view.CyNetworkView;
import cytoscape.plugin.CytoscapePlugin;

/**
 * Plugin for Cytoscape to find the shortest path between 2 nodes in a network.
 * It is possible to find the shortest path in directed and undirected networks
 * 
 * @author mrsva
 *
 */
public class ShortestPathPlugin extends CytoscapePlugin {
	
	/**
	 * Constructor
	 * 
	 * Will set up the menu entries in the <pre>Plugins</pre> menu. 
	 *
	 */
	public ShortestPathPlugin() {
		JMenu menu = new JMenu("Shortest Path...");
		Cytoscape.getDesktop().getCyMenus().getMenuBar().getMenu("Plugins").add(menu);
		// final SelectSetup selecter = new SelectSetup();
		CyAttributes edgeAttributes = Cytoscape.getEdgeAttributes();
		String[] attributeNames = edgeAttributes.getAttributeNames();

		addMenuItem(menu, "Hop Distance");

		//Finds all attributes that are integers or doubles, and adds them to list
		for(int i = 0; i < attributeNames.length; i++)
		{
			String name = attributeNames[i];
			byte type = edgeAttributes.getType(name);

			if((type == edgeAttributes.TYPE_INTEGER) || (type == edgeAttributes.TYPE_FLOATING)) {
				addMenuItem(menu, name);
			}
		}
	}

	private JMenuItem addMenuItem(JMenu menu, String label) {
		JMenuItem item = new JMenuItem(label);
		{
			MenuActionListener va = new MenuActionListener(label);
			item.addActionListener(va);
		}
		menu.add(item);
		return item;
	}

	private class MenuActionListener extends AbstractAction {
		String selectedAttribute = null;

		public MenuActionListener (String label) {
			this.selectedAttribute = label;
		}

		public void actionPerformed(ActionEvent ev) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ShortestPathDialog d = new ShortestPathDialog(Cytoscape.getDesktop(),selectedAttribute);
					d.pack();
					d.setLocationRelativeTo(Cytoscape.getDesktop());
					d.setVisible(true);
				}
			});
		}
	}
}
