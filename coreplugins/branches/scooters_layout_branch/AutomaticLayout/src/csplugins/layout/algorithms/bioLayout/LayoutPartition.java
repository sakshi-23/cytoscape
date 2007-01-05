/* vim: set ts=2: */
/**
 * Copyright (c) 2006 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions, and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions, and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   3. Redistributions must acknowledge that this software was
 *      originally developed by the UCSF Computer Graphics Laboratory
 *      under support by the NIH National Center for Research Resources,
 *      grant P41-RR01081.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package csplugins.layout.algorithms.bioLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.Random;
import java.util.Date;

import java.awt.Dimension;

import cytoscape.*;
import cytoscape.view.*;
import giny.view.*;

import csplugins.layout.algorithms.bioLayout.LayoutNode;
import csplugins.layout.algorithms.bioLayout.LayoutEdge;
import csplugins.layout.algorithms.bioLayout.Profile;

import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;


/**
 * 
 *
 * @author <a href="mailto:scooter@cgl.ucsf.edu">Scooter Morris</a>
 * @version 0.9
 */

public class LayoutPartition {
	private ArrayList<LayoutNode> nodeList;
	private ArrayList<LayoutEdge> edgeList;
	private static HashMap<CyNode,LayoutNode> nodeToLayoutNode = null;
	private static OpenIntIntHashMap nodesSeenMap = null;
	private static OpenIntIntHashMap edgesSeenMap = null;
	private int nodeIndex = 0;

	// Keep track of the node min and max values
	private double maxX = -100000;
	private double maxY = -100000;
	private double minX = 100000;
	private double minY = 100000;
	private double width = 0;
	private double height = 0;

	// Keep track of average location
	private double averageX = 0;
	private double averageY = 0;

	// Keep track of the number of locked nodes we have in
	// this partition
	private int lockedNodes = 0;

	// Keep track of the edge weight min/max values.  Note that these are intentionally
	// static because we want to normalize weights across all partitions
	private static double minWeight = 100000;
	private static double maxWeight = -100000;
	private static double maxLogWeight = 0.0;
	private static double minLogWeight = 0.0;

  // private constants
  private static final int m_NODE_HAS_NOT_BEEN_SEEN = 0;
  private static final int m_NODE_HAS_BEEN_SEEN     = 1;
	private static double logWeightCeiling = 1074;  // Maximum log value (-Math.log(Double.MIN_VALU))

	// Some static values -- these will be the same for all partitions
	private static double minWeightCutoff = 0;
	private static double maxWeightCutoff = Double.MAX_VALUE;

	/**
	 * LayoutPartition: use this constructor to create an empty LayoutPartition.
	 */
	public LayoutPartition(int nodeCount, int edgeCount) {
		nodeList = new ArrayList(nodeCount);
		edgeList = new ArrayList(edgeCount);
		if (nodeToLayoutNode == null)
			nodeToLayoutNode = new HashMap(nodeCount);
	}

	/**
	 * LayoutPartition: use this constructor to create a LayoutPartition that
	 * includes the entire network.
	 *
	 * @param network the CyNetwork to include
	 * @param networkView the CyNetworkView to use
	 * @param selectedOnly if true, only include selected nodes in the partition
	 * @param edgeAttribute a String that contains the name of the attribute to use
	 *                      for edge weighting
	 */
	public LayoutPartition(CyNetwork network, CyNetworkView networkView, 
	                       boolean selectedOnly, String edgeAttribute) {
		// Initialize
		nodeList = new ArrayList(network.getNodeCount());
		edgeList = new ArrayList(network.getEdgeCount());
		if (nodeToLayoutNode == null)
			nodeToLayoutNode = new HashMap(network.getNodeCount());

		// Now, walk the iterators and fill in the values
		nodeListInitialize(network, networkView, selectedOnly);
		edgeListInitialize(network, networkView, edgeAttribute);
		trimToSize();
	}

	public void addNode(NodeView nv, boolean locked) {
		CyNode node = (CyNode)nv.getNode();
		LayoutNode v = new LayoutNode(nv, nodeIndex++);
		nodeList.add(v);
		nodeToLayoutNode.put(node,v);

		if (locked) {
			v.lock();
			lockedNodes++;
		} else {
			updateMinMax(nv.getXPosition(), nv.getYPosition());
			this.width += Math.sqrt(nv.getWidth());
			this.height += Math.sqrt(nv.getHeight());
		}
	}

	/**
	 * Randomize the graph locations.
	 */
	protected void randomizeLocations() {
		// Get a seeded pseudo random-number generator
		Date today = new Date();
		Random random = new Random(today.getTime());
		// Reset our min and max values
		resetNodes();
		Iterator iter = nodeList.iterator();
		while (iter.hasNext()) { 
			LayoutNode node = (LayoutNode)iter.next();
			if (!node.isLocked()) {
				double x = random.nextDouble()*width;
				double y = random.nextDouble()*height;
				node.setLocation(x, y);
				updateMinMax(x, y);
			}
		}
	}

	public void moveNodeToLocation(LayoutNode node) {
		// We provide this routine so that we can keep our
		// min/max values updated
		node.moveToLocation();
		updateMinMax(node.getX(), node.getY());
	}

	public void addEdge(CyEdge edge, String edgeAttribute) {
		LayoutEdge newEdge = new LayoutEdge(edge);
		updateWeights(newEdge, edgeAttribute);
		edgeList.add(newEdge);
	}

	public void addEdge(CyEdge edge, LayoutNode v1, LayoutNode v2, String edgeAttribute) {
		LayoutEdge newEdge = new LayoutEdge(edge,v1,v2);
		updateWeights(newEdge, edgeAttribute);
		edgeList.add(newEdge);
	}

	public void fixEdges() {
		Iterator edgeIter = edgeList.iterator();
		while (edgeIter.hasNext()) {
			// Get the "layout edge"
			LayoutEdge lEdge = (LayoutEdge)edgeIter.next();
			// Get the underlying edge
			CyEdge edge = lEdge.getEdge();
			CyNode target = (CyNode)edge.getTarget();
			CyNode source = (CyNode)edge.getSource();
			if (nodeToLayoutNode.containsKey(source) && 
					nodeToLayoutNode.containsKey(target)) {
				// Add the connecting nodes
				lEdge.addNodes((LayoutNode)nodeToLayoutNode.get(source),
				               (LayoutNode)nodeToLayoutNode.get(target));
			}
		}
	}

	/**
	 * Calculate and set the edge weights.  Note that this will delete
	 * edges from the calculation (not the graph) when certain conditions
	 * are met.
	 */
	protected void calculateEdgeWeights() {
		// Normalize the weights to between 0 and 1
		boolean logWeights = false;
		ListIterator iter = edgeList.listIterator();
		if (Math.abs(maxLogWeight - minLogWeight) > 3) 
			logWeights = true;
		while (iter.hasNext()) { 
			LayoutEdge edge = (LayoutEdge)iter.next();
			double weight = edge.getWeight();
			// If we're only dealing with selected nodes, drop any edges
			// that don't have any selected nodes
			if (edge.getSource().isLocked() && edge.getTarget().isLocked()) {
				iter.remove();
			} else if (minWeight == maxWeight) {
				continue; // unweighted
			} else if (weight <= minWeightCutoff || weight > maxWeightCutoff) {
			// Drop any edges that are outside of our bounds
				iter.remove();
			} else {
				if (logWeights)
					edge.normalizeWeight(minLogWeight,maxLogWeight,true);
				else
					edge.normalizeWeight(minWeight,maxWeight,false);

				// Drop any edges where the normalized weight is small
				if (edge.getWeight() < .001)
					iter.remove();
			}
		}
	}

	public void trimToSize() {
		nodeList.trimToSize();
		edgeList.trimToSize();
	}

	public int size() { return nodeList.size(); }

	public List<LayoutNode> getNodeList() { return nodeList; }
	public List<LayoutEdge> getEdgeList() { return edgeList; }

	public Iterator nodeIterator() { return nodeList.iterator(); }
	public Iterator edgeIterator() { return edgeList.iterator(); }

	public int nodeCount() { return nodeList.size(); }
	public int edgeCount() { return edgeList.size(); }

	public double getMaxX() { return maxX; }
	public double getMaxY() { return maxY; }
	public double getMinX() { return minX; }
	public double getMinY() { return minY; }
	public double getWidth() { return width; }
	public double getHeight() { return height; }
		
	public int lockedNodeCount() { return lockedNodes; }

	public Dimension getAverageLocation() {
		int nodes = nodeCount()-lockedNodes;
		Dimension result = new Dimension();
		result.setSize(averageX/nodes, averageY/nodes);
		return result;
	}


	/**
	 * Reset routines
	 */
	public void resetNodes() { 
		maxX = -100000;	 
		maxY = -100000;	 
		minX = 100000;	 
		minY = 100000;	 
		averageX = 0;
		averageY = 0;
	}

	public static void resetEdges() { 
		maxWeight = -100000;
		minWeight = 100000;
		maxLogWeight = -100000;
		minLogWeight = 100000;
	}

	/**
	 * Private routines
	 */
	private void nodeListInitialize(CyNetwork network, CyNetworkView networkView,
                                  boolean selectedOnly)
	{
		int nodeIndex = 0;
		this.nodeList = new ArrayList(network.getNodeCount());
		Set selectedNodes = null;
		Iterator iter = networkView.getNodeViewsIterator();
		if (selectedOnly) {
			selectedNodes = ((CyNetwork)network).getSelectedNodes();
		}
		while (iter.hasNext()) {
			NodeView nv = (NodeView)iter.next();
			CyNode node = (CyNode)nv.getNode();
			if (selectedNodes != null && !selectedNodes.contains(node)) {
				addNode(nv, true);
			} else {
				addNode(nv, false);
			}
		}
	}

	private void edgeListInitialize(CyNetwork network, CyNetworkView networkView,
                                  String edgeAttribute)
	{
		Iterator iter = network.edgesIterator();
		while (iter.hasNext()) {
			CyEdge edge = (CyEdge)iter.next();

			// Make sure we clean up after any previous layouts
			EdgeView ev = networkView.getEdgeView(edge);
			ev.clearBends();

			CyNode source = (CyNode)edge.getSource();
			CyNode target = (CyNode)edge.getTarget();
			if (source == target) 
				continue;

			LayoutNode v1 = (LayoutNode)nodeToLayoutNode.get(source);
			LayoutNode v2 = (LayoutNode)nodeToLayoutNode.get(target);
			// Do we care about this edge?
			if (v1.isLocked() && v2.isLocked())
				continue; // no, ignore it
			addEdge(edge, v1, v2, edgeAttribute);
		}
	}

	private void updateMinMax(double x, double y) {
		minX = Math.min(minX,x);
		minY = Math.min(minY,y);
		maxX = Math.max(maxX,x);
		maxY = Math.max(maxY,y);
		averageX += x;
		averageY += y;
	}

	private void updateWeights(LayoutEdge newEdge, String edgeAttribute) {
		newEdge.setWeight(edgeAttribute);
		maxWeight = Math.max(maxWeight,newEdge.getWeight());
		minWeight = Math.min(minWeight,newEdge.getWeight());
		maxLogWeight = Math.max(maxLogWeight,newEdge.getLogWeight());
		minLogWeight = Math.min(minLogWeight,newEdge.getLogWeight());
	}

	// Static routines

	/**
	 * Set the edge weight cutoffs
	 */
	public static void setWeightCutoffs(double minCutoff, double maxCutoff) {
		minWeightCutoff = minCutoff;
		maxWeightCutoff = maxCutoff;
	}

	/**
	 * Partition the graph -- this builds the LayoutEdge and LayoutNode
	 * arrays as a byproduct.  The algorithm for this was taken from
	 * algorithms/graphPartition/SGraphPartition.java.
	 */
	public static List partition(CyNetwork network, CyNetworkView networkView, 
	                             boolean selectedOnly, String edgeAttribute) {
		ArrayList partitions = new ArrayList();
		
		nodesSeenMap = new OpenIntIntHashMap(network.getNodeCount());
		edgesSeenMap = new OpenIntIntHashMap(network.getEdgeCount());
		OpenIntObjectHashMap nodesToViews = new 
						OpenIntObjectHashMap(network.getNodeCount());
		nodeToLayoutNode = new HashMap(network.getNodeCount());


		// Initialize the maps
		Iterator nodeViewIter = networkView.getNodeViewsIterator();
		while (nodeViewIter.hasNext()) {
			NodeView nv = (NodeView)nodeViewIter.next();
			int node = nv.getNode().getRootGraphIndex();
			nodesSeenMap.put(node, m_NODE_HAS_NOT_BEEN_SEEN);
			nodesToViews.put(node, nv);
		}

		// Initialize/reset edge weighting
		LayoutEdge.setLogWeightCeiling(logWeightCeiling);
		LayoutPartition.resetEdges();

		Iterator edgeIter = network.edgesIterator();
		while (edgeIter.hasNext()) {
			int edge = ((CyEdge)edgeIter.next()).getRootGraphIndex();
			edgesSeenMap.put(edge, m_NODE_HAS_NOT_BEEN_SEEN);
		}

		// OK, now get new iterators and traverse the graph
		Iterator nodeIter = null;
		if (selectedOnly) {
			nodeIter = ((CyNetwork)network).getSelectedNodes().iterator();
		} else {
			nodeIter = network.nodesIterator();
		}

		while (nodeIter.hasNext()) {
			CyNode node = (CyNode)nodeIter.next();
			int nodeIndex = node.getRootGraphIndex();

			// Have we seen this already?
			if (nodesSeenMap.get(nodeIndex) == m_NODE_HAS_BEEN_SEEN) continue;

			// Nope, first time
			LayoutPartition part = new LayoutPartition(network.getNodeCount(), network.getEdgeCount());

			nodesSeenMap.put(nodeIndex, m_NODE_HAS_BEEN_SEEN);

			// Traverse through all connected nodes
			traverse(network, networkView, nodesToViews, 
			         node, part, edgeAttribute);

			// Done -- finalize the parition
			part.trimToSize();

			// Finally, now that we're sure we've touched all of our
			// nodes.  Fix up our edgeLayout list to have all of our
			// layoutNodes
			part.fixEdges();
		
			partitions.add(part);
		}

		// Now sort the partitions based on the partition's node count
		Object parts[] = partitions.toArray();
		Arrays.sort(parts, new Comparator()
			{
				public int compare(Object o1, Object o2)
				{
					LayoutPartition p1 = (LayoutPartition)o1;
					LayoutPartition p2 = (LayoutPartition)o2;
					return (p2.size() - p1.size());
				}

				public boolean equals(Object obj) { return false; }
			});
		
		return Arrays.asList(parts);
	}

  /**
    * This method traverses nodes connected to the specified node.
    * @param network				The CyNetwork we are laying out
    * @param networkView		The CyNetworkView we are laying out
    * @param nodesToViews		A map that maps between nodes and views
    * @param node						The node to search for connected nodes.
    * @param partition			The partition that holds all of the nodes and edges.
    * @param edgeAttribute	A String that is the name of the attribute to use
		*                     	for weights
    */
  private static void traverse(CyNetwork network,
	                             CyNetworkView networkView,
                               OpenIntObjectHashMap nodesToViews,
                               CyNode node, LayoutPartition partition,
	                             String edgeAttribute)
  {
		int nodeIndex = node.getRootGraphIndex();

		// Get the nodeView
		NodeView nv = (NodeView)nodesToViews.get(nodeIndex);

		// Add this node to the partition
		partition.addNode(nv, false);

		// Get the list of edges connected to this node
		int incidentEdges[] = network.getAdjacentEdgeIndicesArray(nodeIndex,
                                     true, true, true);

    // Iterate through each connected edge
    for (int i = 0; i < incidentEdges.length; i++)
    {
			// Get the actual edge
			CyEdge incidentEdge = (CyEdge)network.getEdge(incidentEdges[i]);

			int edgeIndex = incidentEdge.getRootGraphIndex();
			// Have we already seen this edge?
			if (!edgesSeenMap.containsKey(edgeIndex) ||
			    edgesSeenMap.get(edgeIndex) == m_NODE_HAS_BEEN_SEEN) {
				// Yes, continue since it means we *must* have seen both sides
				continue;
			}

			edgesSeenMap.put(edgeIndex, m_NODE_HAS_BEEN_SEEN);

			// Make sure we clean up after any previous layouts
			EdgeView ev = networkView.getEdgeView(incidentEdge);
			ev.clearBends();

			// Add the edge to the partition
			partition.addEdge(incidentEdge, edgeAttribute);

			// Determine the node's index that is on the other side of the edge
			CyNode otherNode;
			if (incidentEdge.getSource() == node) {
				otherNode = (CyNode)incidentEdge.getTarget();
			} else {
				otherNode = (CyNode)incidentEdge.getSource();
			}

			int incidentNodeIndex = otherNode.getRootGraphIndex();

			// Have we seen the connecting node yet?
			if (nodesSeenMap.containsKey(incidentNodeIndex) &&
					nodesSeenMap.get(incidentNodeIndex) == m_NODE_HAS_NOT_BEEN_SEEN) {
				// Mak it as having been seen
				nodesSeenMap.put(incidentNodeIndex, m_NODE_HAS_BEEN_SEEN);

				// Traverse through this one
				traverse(network, networkView, nodesToViews, 
				         otherNode, partition, edgeAttribute);
			}
		}
	}
}
