package cytoscape.bubbleRouter;

import giny.model.Node;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class LayoutRegion extends Component {

	private double x1;

	private double y1;

	private double w1;

	private double h1;

	private Paint paint;

	private List nodeViews;

	private Object attValue;

	/**
	 * @param value
	 * @param x1
	 * @param y1
	 * @param w1
	 * @param h1
	 */
	public LayoutRegion(Object value, double x1, double y1, double w1, double h1) {
		super();
		// TODO Auto-generated constructor stub
		attValue = value;
		this.paint = Color.red;
		setBounds((int)x1,(int)y1,(int)w1,(int)h1);

		nodeViews = new ArrayList();

	}

	/**
	 * Empty Constructor
	 * 
	 */
	public LayoutRegion() {
		super();

		nodeViews = new ArrayList();

	}

	/**
	 * @return Returns the attValue.
	 */
	public Object getAttValue() {
		return attValue;
	}

	/**
	 * @param attValue
	 *            The attValue to set.
	 */
	public void setAttValue(Object attValue) {
		this.attValue = attValue;
	}

	/**
	 * @return Returns the h1.
	 */
	public double getH1() {
		return h1;
	}

	/**
	 * @param h1
	 *            The h1 to set.
	 */
	public void setH1(double h1) {
		this.h1 = h1;
	}

	/**
	 * @return Returns the w1.
	 */
	public double getW1() {
		return w1;
	}

	/**
	 * @param w1
	 *            The w1 to set.
	 */
	public void setW1(double w1) {
		this.w1 = w1;
	}

	/**
	 * @return Returns the x1.
	 */
	public double getX1() {
		return x1;
	}

	/**
	 * @param x1
	 *            The x1 to set.
	 */
	public void setX1(double x1) {
		this.x1 = x1;
	}

	/**
	 * @return Returns the y1.
	 */
	public double getY1() {
		return y1;
	}

	/**
	 * @param y1
	 *            The y1 to set.
	 */
	public void setY1(double y1) {
		this.y1 = y1;
	}

	/**
	 * @return Returns the paint.
	 */
	public Paint getPaint() {
		return paint;
	}

	/**
	 * @param paint
	 *            The paint to set.
	 */
	public void setPaint(Paint paint) {
		this.paint = paint;
	}

	/**
	 * @return Returns the nodeViews.
	 */
	public List getNodeViews() {
		return nodeViews;
	}

	// AJK: 09/02/06 BEGIN
	// select all nodeViews with specified attribute value for attribute
	public void populateNodeViews(String attributeName) {
		CyAttributes attribs = Cytoscape.getNodeAttributes();
		Iterator it = Cytoscape.getCurrentNetwork().nodesIterator();
		Collection selectedNodes = new ArrayList();
		while (it.hasNext()) {
			Cytoscape.getCurrentNetwork().unselectAllNodes();
			Node node = (Node) it.next();
			String val = attribs.getStringAttribute(node.getIdentifier(),
					attributeName);
			if (val != null) {
				if (val.equalsIgnoreCase(this.attValue.toString())) {
					selectedNodes.add(node);
				}
			} else if (attValue.equals("unassigned")) {
				selectedNodes.add(node);
			}
		}
		Cytoscape.getCurrentNetwork().setSelectedNodeState(selectedNodes, true);
		System.out.println("selected " + selectedNodes.size()
				+ " nodes for layout.");

		// APico 9.16.06
		// Run hierarchical layout on selected nodes, unless no nodes are
		// selected
		if (selectedNodes.size() > 0) {
			HierarchicalLayoutListener hierarchicalListener = new HierarchicalLayoutListener();
			System.out.println("running hierarchical layout algorithm.");
			hierarchicalListener.actionPerformed(null);

			NodeViewsTransformer
					.transform(Cytoscape.getCurrentNetworkView()
							.getSelectedNodes(), new Rectangle2D.Double(x1, y1,
							w1, h1));

			Cytoscape.getCurrentNetworkView().redrawGraph(true, true);
		}
	}

	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		x1 = x;
		y1 = y;
		w1 = width;
		h1 = height;
	}

	public void paint(Graphics g) {

		// before anything, lets make sure we have a color
		Color currentColor = (paint instanceof Color) ? (Color)paint : null;
		if (currentColor == null) {
			System.out.println("LayoutRegion.paint(), currentColor is null");
			return;
		}

		// image to draw
		BufferedImage image = new BufferedImage((int)w1,(int)h1,BufferedImage.TYPE_INT_ARGB);
		Graphics2D image2D = image.createGraphics();

		// set proper translucency
		Color regionColor = new Color(currentColor.getRed(),
									  currentColor.getGreen(),
									  currentColor.getBlue(),
									  26);

		// draw into the image
		Composite origComposite = image2D.getComposite();
		image2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
		image2D.setPaint(regionColor);
		image2D.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
		image2D.setColor(Color.black);
		image2D.drawRect(0,0, image.getWidth(null)-1, image.getHeight(null)-1);
		image2D.setComposite(origComposite);
		((Graphics2D)g).drawImage(image, null, 0,0);
	}
	// AJK: 09/02/06 END
}
