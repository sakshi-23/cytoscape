
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

package ding.view;

import giny.model.Edge;

import giny.view.GraphView;


final class GraphViewEdgesUnselectedEvent extends GraphViewChangeEventAdapter {
	private final GraphView m_view;
	private final int[] m_unselectedEdgeInx;

	GraphViewEdgesUnselectedEvent(GraphView view, int[] unselectedEdgeInx) {
		super(view);
		m_view = view;
		m_unselectedEdgeInx = unselectedEdgeInx;
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public final int getType() {
		return EDGES_UNSELECTED_TYPE;
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public final Edge[] getUnselectedEdges() {
		final Edge[] returnThis = new Edge[m_unselectedEdgeInx.length];

		for (int i = 0; i < returnThis.length; i++)
			returnThis[i] = m_view.getRootGraph().getEdge(m_unselectedEdgeInx[i]);

		return returnThis;
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public final int[] getUnselectedEdgeIndices() {
		final int[] returnThis = new int[m_unselectedEdgeInx.length];

		for (int i = 0; i < returnThis.length; i++)
			returnThis[i] = m_unselectedEdgeInx[i];

		return returnThis;
	}
}
