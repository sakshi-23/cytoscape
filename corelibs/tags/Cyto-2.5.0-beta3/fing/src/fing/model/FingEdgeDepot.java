
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

package fing.model;

import giny.model.Edge;
import giny.model.RootGraph;


/**
 * Please try to restrain from using this class, or even looking at it.
 * This class was created so that certain legacy applications would have an
 * easier time using this giny.model implementation.  Please use
 * FingRootGraphFactory instead of this class.
 * @see FingRootGraphFactory
 **/
public interface FingEdgeDepot {
	/**
	 * This either instantiates a new edge or gets one from the recyclery,
	 * initializing it with the parameters specified.
	 **/
	public Edge getEdge(RootGraph root, int index, String id);

	/**
	 * Recycles an edge.  Implementations may choose to do nothing in this
	 * method and instantiate a new edge in each call to getEdge().  This method
	 * is simply a hook for Fing to tell the depository "I'm done using this edge
	 * object -- it's no longer part of a RootGraph".
	 **/
	public void recycleEdge(Edge node);
}
