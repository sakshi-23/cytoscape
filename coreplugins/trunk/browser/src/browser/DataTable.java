package browser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.view.cytopanels.CytoPanelListener;
import cytoscape.view.cytopanels.CytoPanelState;

/**
 * @author kono
 * 
 * DataTable class constructs all Panels for the browser.
 * 
 * For this version, CytoPanels are used as: 1. Not used. Just default Network
 * Tree Viewer will be shown. 2. Main Attribute Browser. 3. "Advanced Window."
 * Mainly for filtering.
 * 
 */
public class DataTable {

	// Panels to be added on the CytoPanels
	ModPanel modPanel;
	SelectPanel selectionPanel;
	DataTableModel tableModel;

	// Small toolbar panel on the top of browser
	AttributeBrowserPanel attributePanel2;

	// Index number for the panels
	int attributePanelIndex;
	int modPanelIndex;
	int tableIndex;

	int browserIndex;

	// Each Attribute Browser operates on one CytoscapeData object, and on
	// either Nodes or Edges.
	CyAttributes data;

	public static int NODES = 0;
	public static int EDGES = 1;
	private String type = null;
	
	public int graphObjectType;

	public DataTable(CyAttributes data, int graphObjectType) {

		// set up CytoscapeData Object and GraphObject Type
		this.data = data;
		this.graphObjectType = graphObjectType;

		// Make display title
		type = "Node";
		if (graphObjectType != NODES)
			type = "Edge";

		// Create table model.
		tableModel = (DataTableModel) makeModel(data);
		tableModel.setGraphObjectType(graphObjectType);

		// List of attributes and labels: CytoPanel 1

		// Toolbar for selecting attributes and create new attribute.
		attributePanel2 = new AttributeBrowserPanel(data, new AttributeModel(data),
				new LabelModel(data), graphObjectType);
		attributePanel2.setTableModel(tableModel);

		// the attribute table display: CytoPanel 2, horizontal SOUTH panel.
		JPanel mainPanel = new JPanel(); // Container for table and toolbar.
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setPreferredSize(new java.awt.Dimension(400, 180));
		mainPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null,
				type + " Attribute Browser",
				javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
				javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));

		JScrollPane mainTable = new JScrollPane(new JSortTable(tableModel));
		mainPanel.setName( type + "AttributeBrowser" );
		mainPanel.add(mainTable, java.awt.BorderLayout.CENTER);
		mainPanel.add(attributePanel2, java.awt.BorderLayout.NORTH);
		// BrowserPanel mainPanel = new BrowserPanel(new
		// JSortTable(tableModel));

		//
		// Advanced Window: CytoPanel 2
		//
		JTabbedPane advancedPanel = new JTabbedPane();
		advancedPanel.setPreferredSize(new Dimension(200,100));
		
		modPanel = new ModPanel(data, tableModel,
				graphObjectType);
		selectionPanel = new SelectPanel(tableModel, graphObjectType);
		advancedPanel.add("Selection", selectionPanel);
		advancedPanel.add("Modification", modPanel);

		// Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST).add(
		// type + "Attributes", attributePanel);

		// Add advanced panel to the CytoPanel 3 (EAST)
		Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST).add(
				type + "Attr Mod/ Object Select", advancedPanel);

		
		// Cytoscape.getDesktop().getCytoPanel(SwingConstants.SOUTH).add(
		// type + " Attribute Browser", mainPanel);
		//		

		// Add main browser panel to CytoPanel 2 (SOUTH)
		Cytoscape.getDesktop().getCytoPanel(SwingConstants.SOUTH).add(
				type + " Attribute Browser", mainPanel);

	
		// Get indexes for the panels.
		modPanelIndex = Cytoscape.getDesktop()
				.getCytoPanel(SwingConstants.EAST).indexOfComponent(
						advancedPanel);

		tableIndex = Cytoscape.getDesktop().getCytoPanel(SwingConstants.SOUTH)
				.indexOfComponent(mainPanel);

		Cytoscape.getDesktop().getCytoPanel(SwingConstants.SOUTH)
				.addCytoPanelListener(
						new Listener(attributePanelIndex, -1, modPanelIndex,
								tableIndex));

		Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST)
				.addCytoPanelListener(
						new Listener(attributePanelIndex, tableIndex, -1,
								modPanelIndex));

		Cytoscape.getDesktop().getCytoPanel(SwingConstants.SOUTH).setState(
				CytoPanelState.DOCK);

	}

	// 
	class Listener implements CytoPanelListener {

		int WEST;
		int SOUTH;
		int EAST;
		int myIndex;

		Listener(int w, int s, int e, int my) {

			WEST = w;
			SOUTH = s;
			EAST = e;
			myIndex = my;

		}

		public void onComponentAdded(int count) {
		}

		public void onComponentRemoved(int count) {
		}

		public void onComponentSelected(int componentIndex) {

			if (componentIndex == myIndex) {
				if (WEST != -1) {
					Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST)
							.setSelectedIndex(WEST);

				}
				if (SOUTH != -1) {
					Cytoscape.getDesktop().getCytoPanel(SwingConstants.SOUTH)
							.setSelectedIndex(SOUTH);

				}
				if (EAST != -1) {
					Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST)
							.setSelectedIndex(EAST);

				}
			}

		}

		public void onStateChange(CytoPanelState newState) {
		}
	}



	public int getGraphObjectType() {
		return graphObjectType;
	}

	public CyAttributes getData() {
		return data;
	}

	//
	// Make sort model by using given CyAttributes
	//
	protected SortTableModel makeModel(CyAttributes data) {

		List attributes = Arrays.asList(data.getAttributeNames());
		List graph_objects = getFlaggedGraphObjects();

		DataTableModel model = new DataTableModel();
		model.setTableData(data, graph_objects, attributes);
		return model;
	}

	private List getFlaggedGraphObjects() {
		if (graphObjectType == NODES) {
//			return new ArrayList(Cytoscape.getCurrentNetwork()
//					.getFlaggedNodes());
			return new ArrayList(Cytoscape.getCurrentNetwork().getSelectedNodes());
		} else {
			return new ArrayList(Cytoscape.getCurrentNetwork().getSelectedEdges());
		}
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("JSortTable Test");
		frame.getContentPane().setLayout(new GridLayout());
		frame.getContentPane().add(new JSortTableTest());
		frame.pack();
		frame.show();
	}
}
