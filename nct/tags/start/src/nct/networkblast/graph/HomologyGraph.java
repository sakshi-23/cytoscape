package nct.networkblast.graph;

import java.util.*;
import java.util.logging.Logger;
import java.io.*;

import nct.graph.Graph;
import nct.graph.basic.BasicKPartiteGraph;
import nct.graph.SequenceGraph;
import nct.service.homology.HomologyModel;

import org.biojava.bio.*;
import org.biojava.bio.seq.db.*;
import org.biojava.bio.seq.io.*;
import org.biojava.bio.symbol.*;


/**
 * This class creates a K-partite graph based on the homology
 * of proteins between two or more species. 
 */
public class HomologyGraph 
	extends BasicKPartiteGraph<String,Double,SequenceGraph<String,Double>> {

	private static Logger log = Logger.getLogger("networkblast");

	HomologyModel homModel;

	/**
	 * @param homModel The homology model used to generate the edges in the graph.
	 */
	public HomologyGraph(HomologyModel homModel) {
		super();
		this.homModel = homModel;
	}

	/**
	 * @param homModel The homology model used to generate the edges in the graph.
	 * @param graphs A collection of graphs to be added to this graph.
	 */
	public HomologyGraph(HomologyModel homModel, Collection<SequenceGraph<String,Double>> graphs) {
		super();
		this.homModel = homModel;
		for (SequenceGraph<String,Double> sg : graphs)
			addGraph(sg);
	}

	/**
	 * Adds the nodes of the specified graph to this graph and adds the graph as a
	 * partition.
	 * @param sg The SequenceGraph to be added to this graph.
	 * @return Returns true if we're able to add the graph as a partition and we successfully
	 * add at least one node to the graph.
	 */
	public boolean addGraph(SequenceGraph<String,Double> sg) {

		List<SequenceGraph<String,Double>> partitions = getPartitions();
		int numAdded = 0;
		if ( partitions == null || !partitions.contains(sg) ) {
			//System.out.println("adding graph " + sg.toString());
			// add the nodes for the new graph
			for ( String node: sg.getNodes()) {
				//System.out.println("adding node: '" + node + "'");
				if ( addNode(node,sg) )
					numAdded++;
				else
					System.out.println("didn't add node: " + node);
			}
			
			// add all homology edges between each existing graph/partition
			List<SequenceGraph<String,Double>> updatedParts = getPartitions();
			if ( updatedParts.size() > 1 )
				for ( int i = 0; i < updatedParts.size(); i++ )
					for ( int j = i+1; j < updatedParts.size(); j++ )
						createHomologyEdges(updatedParts.get(i),updatedParts.get(j));
			if ( numAdded > 0 )
				return true;
			else
				return false;
		} else
			return false; // graph has already been added
	}

	private void createHomologyEdges(SequenceGraph<String,Double> sg1, SequenceGraph<String,Double> sg2) {
		Map<String,Map<String,Double>> homologyMap = homModel.expectationValues( sg1, sg2 );
		//System.out.println("creating homology edges for:");
		//System.out.println("sg1 " + sg1.toString());
		//System.out.println("sg2 " + sg2.toString());
		//System.out.println("homologyMap size " + homologyMap.size());

		for ( String nodeA: homologyMap.keySet() ) {
			//System.out.println ("A node: " + nodeA );
			for ( String nodeB: homologyMap.get(nodeA).keySet() ) {
				//System.out.print ("B node: " + nodeB );
				//System.out.println ("  value: " + homologyMap.get(nodeA).get(nodeB));
				if ( ! addEdge(nodeA,nodeB,homologyMap.get(nodeA).get(nodeB)) )
					System.out.println("didn't add edge: " + nodeA + " " + nodeB);
			}
		}
		//System.out.println("number of homology edges: " + numberOfEdges());
		
	}
}
