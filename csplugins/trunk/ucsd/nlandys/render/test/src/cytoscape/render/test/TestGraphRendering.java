package cytoscape.render.test;

import cytoscape.geom.rtree.RTree;
import cytoscape.geom.spacial.SpacialIndex2D;
import cytoscape.graph.dynamic.DynamicGraph;
import cytoscape.graph.dynamic.util.DynamicGraphFactory;
import cytoscape.graph.fixed.FixedGraph;
import cytoscape.render.immed.GraphGraphics;
import cytoscape.render.stateful.EdgeDetails;
import cytoscape.render.stateful.GraphLOD;
import cytoscape.render.stateful.GraphRenderer;
import cytoscape.render.stateful.NodeDetails;
import cytoscape.util.intr.IntHash;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

public class TestGraphRendering
  extends Frame implements MouseListener, MouseMotionListener
{

  public static void main(String[] args) throws Exception
  {
    final DynamicGraph graph = DynamicGraphFactory.instantiateDynamicGraph();
    final RTree rtree = new RTree();
    final int N = Integer.parseInt(args[0]);
    final double maxDim = 10;
    final double minDim = 5.01;
    final double areaDim = Math.sqrt((double) N) * maxDim * 2;
    final Random r = new Random();
    for (int i = 0; i < N; i++) {
      final double centerX =
        ((double) r.nextInt()) / Integer.MAX_VALUE * (areaDim / 2.0d);
      final double centerY =
        ((double) r.nextInt()) / Integer.MAX_VALUE * (areaDim / 2.0d);
      final double width =
        ((double) (0x7fffffff & r.nextInt())) / Integer.MAX_VALUE *
        (maxDim - minDim) + minDim;
      final double height =
        ((double) (0x7fffffff & r.nextInt())) / Integer.MAX_VALUE *
        (maxDim - minDim) + minDim;
      final float xMin = (float) (centerX - (width / 2));
      final float yMin = (float) (centerY - (height / 2));
      final float xMax = (float) (centerX + (width / 2));
      final float yMax = (float) (centerY + (height / 2));
      rtree.insert(graph.nodeCreate(), xMin, yMin, xMax, yMax); }

    final byte[] shapes = new byte[9];
    shapes[0] = GraphGraphics.SHAPE_RECTANGLE;
    shapes[1] = GraphGraphics.SHAPE_DIAMOND;
    shapes[2] = GraphGraphics.SHAPE_ELLIPSE;
    shapes[3] = GraphGraphics.SHAPE_HEXAGON;
    shapes[4] = GraphGraphics.SHAPE_OCTAGON;
    shapes[5] = GraphGraphics.SHAPE_PARALLELOGRAM;
    shapes[6] = GraphGraphics.SHAPE_ROUNDED_RECTANGLE;
    shapes[7] = GraphGraphics.SHAPE_TRIANGLE;
    shapes[8] = GraphGraphics.SHAPE_VEE;
    final Color[] nodeColors = new Color[256];
    final Color[] nodeColorsLow = new Color[256];
    for (int i = 0; i < nodeColors.length; i++) {
      final int color = (0x00ffffff & r.nextInt()) | 0x7f000000;
      nodeColors[i] = new Color(color, true);
      nodeColorsLow[i] = new Color(color, false); }
    final GraphLOD lod = new GraphLOD();
    final NodeDetails nodeDetails = new NodeDetails() {
        private final float borderWidth = (float) (minDim / 12);
        private final Color borderColor = new Color(63, 63, 63, 127);
        private final Font font = new Font(null, Font.PLAIN, 1);
        private final double fontScaleFactor = minDim / 2;
        private final Color labelColor = new Color(0, 0, 0, 255);
        public Color colorLowDetail(int node) {
          return nodeColorsLow[node % nodeColorsLow.length]; }
        public byte shape(int node) {
          return shapes[node % shapes.length]; }
        public Color fillColor(int node) {
          return nodeColors[node % nodeColors.length]; }
        public float borderWidth(int node) {
          return borderWidth; }
        public Color borderColor(int node) {
          return borderColor; }
        public String label(int node) {
          return "" + node; }
        public Font font(int node) {
          return font; }
        public double fontScaleFactor(int node) {
          return fontScaleFactor; }
        public Color labelColor(int node) {
          return labelColor; } };
        
    final EdgeDetails edgeDetails = new EdgeDetails();
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          Frame f = new TestGraphRendering(graph, rtree, lod,
                                           nodeDetails, edgeDetails);
          f.show();
          f.addWindowListener(new WindowAdapter() {
              public void windowClosing(WindowEvent e) {
                System.exit(0); } }); } });
  }

  private final int m_imgWidth = 800;
  private final int m_imgHeight = 600;
  private final FixedGraph m_graph;
  private final SpacialIndex2D m_spacial;
  private final GraphLOD m_lod;
  private final NodeDetails m_nodeDetails;
  private final EdgeDetails m_edgeDetails;
  private final IntHash m_hash;
  private final Image m_img;
  private final GraphGraphics m_grafx;

  private double m_currXCenter = 0.0d;
  private double m_currYCenter = 0.0d;
  private double m_currScale = 1.0d;
  private int m_currMouseButton = 0; // 0: none; 2: middle; 3: right.
  private int m_lastXMousePos = 0;
  private int m_lastYMousePos = 0;

  public TestGraphRendering(FixedGraph graph,
                            SpacialIndex2D spacial,
                            GraphLOD lod,
                            NodeDetails nodeDetails,
                            EdgeDetails edgeDetails)
  {
    super();
    m_graph = graph;
    m_spacial = spacial;
    m_lod = lod;
    m_nodeDetails = nodeDetails;
    m_edgeDetails = edgeDetails;
    m_hash = new IntHash();
    addNotify();
    m_img = createImage(m_imgWidth, m_imgHeight);
    m_grafx = new GraphGraphics(m_img, true);
    updateImage();
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public void paint(Graphics g)
  {
    final Insets insets = insets();
    g.drawImage(m_img, insets.left, insets.top, null);
    resize(m_imgWidth + insets.left + insets.right,
           m_imgHeight + insets.top + insets.bottom);
  }

  public void update(Graphics g)
  {
    final Insets insets = insets();
    updateImage();
    g.drawImage(m_img, insets.left, insets.top, null);
  }

  private void updateImage()
  {
    GraphRenderer.renderGraph(m_graph, m_spacial, m_lod,
                              m_nodeDetails, m_edgeDetails, m_hash,
                              m_grafx, Color.white,
                              m_currXCenter, m_currYCenter, m_currScale);
  }

  public void mouseClicked(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

  public void mousePressed(MouseEvent e)
  {
    if (e.getButton() == MouseEvent.BUTTON3) {
      m_currMouseButton = 3;
      m_lastXMousePos = e.getX();
      m_lastYMousePos = e.getY(); }
    else if (e.getButton() == MouseEvent.BUTTON2) {
      m_currMouseButton = 2;
      m_lastXMousePos = e.getX();
      m_lastYMousePos = e.getY(); }
  }

  public void mouseReleased(MouseEvent e)
  {
    if (e.getButton() == MouseEvent.BUTTON3) {
      if (m_currMouseButton == 3) m_currMouseButton = 0; }
    else if (e.getButton() == MouseEvent.BUTTON2) {
      if (m_currMouseButton == 2) m_currMouseButton = 0; }
  }

  public void mouseDragged(MouseEvent e)
  {
    if (m_currMouseButton == 3) {
      double deltaX = e.getX() - m_lastXMousePos;
      double deltaY = e.getY() - m_lastYMousePos;
      m_lastXMousePos = e.getX();
      m_lastYMousePos = e.getY();
      m_currXCenter -= deltaX / m_currScale;
      m_currYCenter += deltaY / m_currScale; // y orientations are opposite.
      repaint(); }
    else if (m_currMouseButton == 2) {
      double deltaY = e.getY() - m_lastYMousePos;
      m_lastXMousePos = e.getX();
      m_lastYMousePos = e.getY();
      m_currScale *= Math.pow(2, -deltaY / 300.0d);
      repaint(); }
  }

  public void mouseMoved(MouseEvent e) {}

  public boolean isResizable() { return false; }

}
