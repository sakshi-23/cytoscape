/*
 * Created on May 24, 2005
 *
 */
package cytoscape.editor.actions;

import giny.view.EdgeView;
import giny.view.NodeView;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.undo.AbstractUndoableEdit;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeModifiedNetworkManager;
import cytoscape.editor.CytoscapeEditorManager;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.CyNetworkView;

/**
 * 
 * action for deleting selected Cytoscape nodes and edges
 * 
 * @author Allan Kuchinsky, Agilent Technologies
 * @version 1.0
 *  
 */
public class DeleteAction extends CytoscapeAction {
	private static Image _my_icon;

	//~ Instance fields

	private Object _cyObj = null;

	private String _label = null;

	//~ Constructors
	// ///////////////////////////////////////////////////////////

	/**
	 * action for deleting selected Cytoscape nodes and edges
	 */
	public DeleteAction() {
		super("Delete Selected Nodes and Edges");
	}

	/**
	 * perform deletion on the input object. if object is a Node, then this will
	 * result in also deleting the edges adjacent to the node
	 * 
	 * @param obj
	 *            the object to be deleted
	 */
	public DeleteAction(Object obj) {
		super("Delete Selected Nodes and Edges");
		_cyObj = obj;
	}

	/**
	 * perform deletion on the input object. if object is a Node, then this will
	 * result in also deleting the edges adjacent to the node
	 * 
	 * @param obj
	 *            the object to be deleted
	 * @param label
	 *            the name of the object to be deleted
	 */
	public DeleteAction(Object obj, String label) {
		super("Delete Selected Nodes and Edges");
		_cyObj = obj;
		_label = label;
	}

	//~ Methods
	// ////////////////////////////////////////////////////////////////

	/**
	 * delete all selected nodes and edges groups selected nodes and edges and
	 * then deletes both sets (nodes and edges) for deleted nodes, also collects
	 * and deletes the edges that are adjacent to the node
	 */
	public void actionPerformed(ActionEvent ae) {
		CyNetworkView myView = Cytoscape.getCurrentNetworkView();
		java.util.List edgeViews = myView.getSelectedEdges();
		java.util.List nodeViews = myView.getSelectedNodes();
		final CyNetwork cyNet = myView.getNetwork();

		final int[] nodes = new int[nodeViews.size() + 1];
		int[] allEdges = new int[0];

		// first collect the selected nodes and their adjacent edges
		for (int i = 0; i < nodeViews.size(); i++) {
			NodeView nview = (NodeView) nodeViews.get(i);
			CyNode cyNode = (CyNode) nview.getNode();
			int nodeIdx = cyNode.getRootGraphIndex();
			nodes[i] = nodeIdx;
			int[] edgesList = cyNet.getAdjacentEdgeIndicesArray(nodeIdx, true,
					true, true);
			int[] bigEdges = new int[allEdges.length + edgesList.length];
			for (int m = 0; m < allEdges.length; m++) {
				bigEdges[m] = allEdges[m];
			}
			for (int p = 0; p < edgesList.length; p++) {
				bigEdges[allEdges.length + p] = edgesList[p];
			}
			allEdges = bigEdges;
		}

		// then collect and add the selected edges
		for (int j = 0; j < edgeViews.size(); j++) {
			EdgeView eview = (EdgeView) edgeViews.get(j); // n.b.
			CyEdge cyEdge = (CyEdge) eview.getEdge();
			int edgeIdx = cyEdge.getRootGraphIndex();
			int[] bigEdges = new int[allEdges.length + 1];
			for (int m = 0; m < allEdges.length; m++) {
				bigEdges[m] = allEdges[m];
			}
			bigEdges[allEdges.length] = edgeIdx;
			allEdges = bigEdges;
		}

		// if there is an input argument that is a node, then add it to the
		// collection of nodes to be deleted,
		//   also add its adjacent edges to the collection of edges to be deleted.
		if (_cyObj instanceof giny.model.Node) {
			CyNode cyNode = (CyNode) _cyObj;
			int nodeIdx = cyNode.getRootGraphIndex();
			nodes[nodeViews.size()] = nodeIdx;
			int[] edgesList = cyNet.getAdjacentEdgeIndicesArray(nodeIdx, true,
					true, true);
			int[] bigEdges = new int[allEdges.length + edgesList.length];
			for (int m = 0; m < allEdges.length; m++) {
				bigEdges[m] = allEdges[m];
			}
			for (int p = 0; p < edgesList.length; p++) {
				bigEdges[allEdges.length + p] = edgesList[p];
			}
			allEdges = bigEdges;
		}

		// if there is an input argument that is a node, then add it to the
		// collection of nodes to be deleted
		if (_cyObj instanceof giny.model.Edge) {
			CyEdge myEdge = (CyEdge) _cyObj;
			int edgeIdx = myEdge.getRootGraphIndex();
			int[] bigEdges = new int[allEdges.length + 1];
			for (int m = 0; m < allEdges.length; m++) {
				bigEdges[m] = allEdges[m];
			}
			bigEdges[allEdges.length] = edgeIdx;
			allEdges = bigEdges;
		}

		// now do the deletions
		final int[] edges = allEdges;

		cyNet.hideNodes(nodes);
		cyNet.hideEdges(edges);
		
		// setup the clipboard and undo manager to be able to undo the deletion operation
		CytoscapeEditorManager.getNodeClipBoard().elements(nodes);
		CytoscapeEditorManager.getEdgeClipBoard().elements(edges);
		CytoscapeEditorManager.setNetworkClipBoard(cyNet.getIdentifier());

		CytoscapeEditorManager.addEdit(new AbstractUndoableEdit() {

			final String network_id = cyNet.getIdentifier();

			public String getPresentationName() {
				return "Delete";
			}

			public String getRedoPresentationName() {
				if (edges.length == 0)
					return "Redo: Deleted Nodes";
				else
					return "Redo: Deleted Nodes and Edges";
			}

			public String getUndoPresentationName() {

				if (edges.length == 0)
					return "Undo: Deleted Nodes";
				else
					return "Undo: Deleted Nodes and Edges";

			}

			public void redo() {
				// removes the removed nodes and edges from the network
				CyNetwork network = Cytoscape.getNetwork(network_id);
				if (network != null) {
					network.hideEdges(edges);
					network.hideNodes(nodes);
					CytoscapeEditorManager.getNodeClipBoard().elements(nodes);
					CytoscapeEditorManager.getEdgeClipBoard().elements(edges); // sets elements
				}

			}

			public void undo() {
				CyNetwork network = Cytoscape.getNetwork(network_id);
				if (network != null) {
					network.restoreNodes(nodes);
					network.restoreEdges(edges);
				}
			}

		});

		Cytoscape.firePropertyChange(Cytoscape.NETWORK_MODIFIED, null, cyNet);

	}

}