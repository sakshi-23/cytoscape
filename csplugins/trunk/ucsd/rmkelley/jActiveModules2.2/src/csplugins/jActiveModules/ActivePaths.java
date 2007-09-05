// ActivePaths.java:  a plugin for CytoscapeWindow,
// which uses VERA & SAM expression data
// to propose active gene regulatory paths
//------------------------------------------------------------------------------
// $Revision$
// $Date$
// $Author$
//------------------------------------------------------------------------------
package csplugins.jActiveModules;
//------------------------------------------------------------------------------
import giny.model.Node;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import csplugins.jActiveModules.data.ActivePathFinderParameters;
import csplugins.jActiveModules.dialogs.ConditionsVsPathwaysTable;
import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.ExpressionData;
import cytoscape.data.SelectFilter;
import cytoscape.data.mRNAMeasurement;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;
//import cytoscape.data.CyNetworkFactory;

//-----------------------------------------------------------------------------------
public class ActivePaths implements ActivePathViewer, Runnable {

	protected boolean showTable = true;
	protected boolean hideOthers = true;
	protected boolean randomize = false;

	//protected ExpressionData expressionData = null;
	protected JMenuBar menubar;
	protected JMenu expressionConditionsMenu;
	protected ConditionsVsPathwaysTable tableDialog;
	protected String currentCondition = "none";
	protected csplugins.jActiveModules.Component[] activePaths;
	protected String[] attrNames;
	protected static boolean activePathsFindingIsAvailable;
	protected JButton activePathToolbarButton;
	protected JFrame mainFrame;
	protected CyNetwork cyNetwork;
	protected String titleForCurrentSelection;
	protected ActivePathFinderParameters apfParams;
	protected static double MIN_SIG = 0.0000000000001;
	protected static double MAX_SIG = 1 - MIN_SIG;

	protected static int resultsCount = 1;
	protected ActiveModulesUI parentUI;

	// ----------------------------------------------------------------
	public ActivePaths(CyNetwork cyNetwork, ActivePathFinderParameters apfParams, ActiveModulesUI parentUI) {
		this.apfParams = apfParams;
		if (cyNetwork == null || cyNetwork.getNodeCount() == 0) {
			throw new IllegalArgumentException("Please select a network");
		}
		//expressionData = Cytoscape.getExpressionData();

		//attrNames = expressionData.getConditionNames();
		attrNames = (String[])apfParams.getExpressionAttributes().toArray(new String[0]);
		Arrays.sort(attrNames);
		if (attrNames.length == 0) {
			throw new RuntimeException("No expression data selected!");
		}
		menubar = Cytoscape.getDesktop().getCyMenus().getMenuBar();
		mainFrame = Cytoscape.getDesktop();
		this.cyNetwork = cyNetwork;
		this.parentUI = parentUI;
	} // ctor

	// --------------------------------------------------------------
	protected void setShowTable(boolean showTable) {
		this.showTable = showTable;
	}
	protected void clearActivePaths() {
		this.activePaths = null;
	}

	public void run() {
	    System.gc();
		long start = System.currentTimeMillis();
		HashMap expressionMap = generateExpressionMap();
		// run the path finding algorithm
		ActivePathsFinder apf = null;
		if(randomize){
		    apf = new ActivePathsFinder(expressionMap, attrNames,
						cyNetwork, apfParams, null, parentUI);
		}else{
		    apf = new ActivePathsFinder(expressionMap, attrNames,
						cyNetwork, apfParams, mainFrame, parentUI);
		}
		activePaths = apf.findActivePaths();
		
		tableDialog = null;
		if (showTable) {
		    showConditionsVsPathwaysTable();
		}
		if(apfParams.getSave() && !apfParams.getRandomizeExpression()){
		    tableDialog.saveState(apfParams.getOutputFile());
		}
		if(apfParams.getExit() && !apfParams.getRandomizeExpression()){
		    System.exit(0);
		}
		
	}
    
	/**
	 * Returns the best scoring path from the last run. This is mostly used by
	 * the score distribution when calculating the distribution
	 */
	protected csplugins.jActiveModules.Component getHighScoringPath() {
		System.err.println("High Scoring Path:");
		System.err.println(activePaths);
		System.err.println("Score: " + activePaths[0].getScore());
		int size = activePaths[0].getNodes().size();
		System.err.println("Size: " + size);
		System.err.println("Raw score: "
				+ activePaths[0].calculateSimpleScore());
		System.err.println("Mean: "
				+ csplugins.jActiveModules.Component.pStats.getMean(size));
		System.err.println("Std: "
				+ csplugins.jActiveModules.Component.pStats.getStd(size));
		return activePaths[0];
	}

	protected HashMap generateExpressionMap() {
		// set up the HashMap which is used to map from nodes
		// to z values. At this point, we are mapping from the
		// p values for expression to z values
		System.err.println("Processing Expression Data into Hash");
		HashMap tempHash = new HashMap();
		System.err.println("Do some testing of the ExpressionData object");
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();

		// Create two identical lists of genes
		List<Node> geneList = new ArrayList<Node>();
		List<Node> shuffledList = new ArrayList<Node>();
		for (Iterator nodeIt = cyNetwork.nodesIterator(); nodeIt.hasNext();) {
			Node n = (Node) nodeIt.next();
			geneList.add(n);
			shuffledList.add(n);
		}

		// If randomize, permute the second list of genes
		if ( randomize ) 
			Collections.shuffle(shuffledList);

		for (int i = 0; i < geneList.size(); i++) {
		
			// If not randomizing these will be identical.
			Node current = geneList.get(i); 
			Node shuffle = shuffledList.get(i);

			// If randomizing, you'll get p-values for a different gene. 
			String canonicalName = shuffle.getIdentifier();

			double[] tempArray = new double[attrNames.length];
			for (int j = 0; j < attrNames.length; j++) {
				Double d = nodeAttributes.getDoubleAttribute(canonicalName,attrNames[j]);

				if (d == null) {
					tempArray[j] = ZStatistics.oneMinusNormalCDFInverse(.5);
				} else {
					double sigValue = d.doubleValue();
					if (sigValue < MIN_SIG) {
						sigValue = MIN_SIG;
						System.err.println("Warning: value for " + current.getIdentifier() + 
						                   " (" + canonicalName + ") adjusted to " + MIN_SIG);
					} 
					if (sigValue > MAX_SIG) {
						sigValue = MAX_SIG;
						System.err.println("Warning: value for " + current.getIdentifier() + 
						                   " (" + canonicalName + ") adjusted to " + MAX_SIG);
					} 

					// transform the p-value into a z-value and store it in the
					// array of z scores for this particular node
					tempArray[j] = ZStatistics.oneMinusNormalCDFInverse(sigValue);
				}
			}
			tempHash.put(current, tempArray);
		}
		System.err.println("Done processing into Hash");
		return tempHash;
	}

	protected void showConditionsVsPathwaysTable() {
		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
		tableDialog = new ConditionsVsPathwaysTable(mainFrame, cyNetwork,
				attrNames, activePaths, this, parentUI);

		cytoPanel.add("jActiveModules Results " + (resultsCount++), tableDialog);
		cytoPanel.setSelectedIndex(cytoPanel.indexOfComponent(tableDialog));
		cytoPanel.setState(CytoPanelState.DOCK);
		tableDialog.setVisible(true);
		addActivePathToolbarButton();
	}

	protected ConditionsVsPathwaysTable getConditionsVsPathwaysTable() {
		return tableDialog;
	}

	/**
	 * Scores the currently selected nodes in the graph, and pops up a window
	 * with the result
	 */
	protected void scoreActivePath() {
		String callerID = "jActiveModules";
		ActivePathsFinder apf = new ActivePathsFinder(generateExpressionMap(),
				attrNames, cyNetwork, apfParams, mainFrame, parentUI);

		long start = System.currentTimeMillis();
		Vector result = new Vector();
		Iterator it = cyNetwork.getSelectedNodes().iterator();
		while (it.hasNext()) {
			result.add(it.next());
		}

		double score = apf.scoreList(result);
		long duration = System.currentTimeMillis() - start;
		System.err.println("-------------- back from score: " + duration
				+ " msecs");
		System.err.println("-------------- score: " + score + " \n");
		JOptionPane.showMessageDialog(mainFrame, "Score: " + score);
	} // scoreActivePath

	protected class ActivePathControllerLauncherAction extends AbstractAction {
		ActivePathControllerLauncherAction() {
			super("Active Modules");
		} // ctor
		public void actionPerformed(ActionEvent e) {
			showConditionsVsPathwaysTable();
		}
	}

	/**
	 * find all of the unique node names in the full set of active paths. there
	 * may be duplicates since some nodes may appear in several paths
	 */
	protected Vector combinePaths(
			csplugins.jActiveModules.Component[] activePaths) {
		HashSet set = new HashSet();
		for (int i = 0; i < activePaths.length; i++) {
			set.addAll(activePaths[i].getNodes());
		} // for i
		return new Vector(set);

	}

	protected void addActivePathToolbarButton() {
		// if (activePathToolbarButton != null)
		// cytoscapeWindow.getCyMenus().getToolBar().remove
		// (activePathToolbarButton);

		// activePathToolbarButton =
		// cytoscapeWindow.getCyMenus().getToolBar().add (new
		// ActivePathControllerLauncherAction ());

	} // addActivePathToolbarButton
	// ------------------------------------------------------------------------------
	public void displayPath(csplugins.jActiveModules.Component activePath,
			boolean clearOthersFirst, String pathTitle) {
		titleForCurrentSelection = pathTitle;
		SelectFilter filter = cyNetwork.getSelectFilter();
		// cytoscapeWindow.selectNodesByName (activePath.getNodes (),
		// clearOthersFirst);
		if (clearOthersFirst) {
			// cyNetwork.unFlagAllNodes();
			filter.unselectAllNodes();
		}
		filter.setSelectedNodes(activePath.getDisplayNodesGeneric(), true);
		// cyNetwork.setFlaggedNodes(activePath.getNodes(),true);
		  Cytoscape.getCurrentNetworkView().redrawGraph(false, true);
	}
	// ------------------------------------------------------------------------------
	public void displayPath(csplugins.jActiveModules.Component activePath,
			String pathTitle) {
		displayPath(activePath, true, pathTitle);
	}
	// ------------------------------------------------------------------------------

} // class ActivePaths (a CytoscapeWindow plugin)
