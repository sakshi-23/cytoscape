package ding.view;

import cytoscape.render.stateful.NodeDetails;
import cytoscape.util.intr.IntObjHash;
import java.awt.Color;
import java.awt.Paint;
import java.util.HashMap;

/*
 * Access to the methods of this class should be synchronized externally if
 * there is a threat of multiple threads.
 */
class DNodeDetails extends NodeDetails
{

  final IntObjHash m_colorsLowDetail = new IntObjHash();
  final Object m_deletedEntry = new Object();

  // The values are Byte objects; the bytes are shapes defined in
  // cytoscape.render.immed.GraphGraphics.
  final HashMap m_shapes = new HashMap();
  final HashMap m_fillPaints = new HashMap();
  final HashMap m_borderPaints = new HashMap();

  public Color colorLowDetail(int node)
  {
    final Object o = m_colorsLowDetail.get(node);
    if (o == null || o == m_deletedEntry) {
      return super.colorLowDetail(node); }
    return (Color) o;
  }

  /*
   * The color argument must be pre-checked for null.  Don't pass null in.
   */
  void overrideColorLowDetail(int node, Color color)
  {
    if (super.colorLowDetail(node).equals(color)) {
      final Object val = m_colorsLowDetail.get(node);
      if (val != null && val != m_deletedEntry) {
        m_colorsLowDetail.put(node, m_deletedEntry); } }
    else {
      m_colorsLowDetail.put(node, color); }
  }

  public byte shape(int node)
  {
    final Object o = m_shapes.get(new Integer(node));
    if (o == null) { return super.shape(node); }
    return ((Byte) o).byteValue();
  }

  /*
   * The shape argument must be pre-checked for correctness.
   */
  void overrideShape(int node, byte shape)
  {
    if (super.shape(node) == shape) { m_shapes.remove(new Integer(node)); }
    else { m_shapes.put(new Integer(node), new Byte(shape)); }
  }

  public Paint fillPaint(int node)
  {
    final Object o = m_fillPaints.get(new Integer(node));
    if (o == null) { return super.fillPaint(node); }
    return (Paint) o;
  }

  /*
   * The paint argument must be pre-checked for null.  Don't pass null in.
   */
  void overrideFillPaint(int node, Paint paint)
  {
    if (super.fillPaint(node).equals(paint)) {
      m_fillPaints.remove(new Integer(node)); }
    else { m_fillPaints.put(new Integer(node), paint); }
  }

  public Paint borderPaint(int node)
  {
    final Object o = m_borderPaints.get(new Integer(node));
    if (o == null) { return super.borderPaint(node); }
    return (Paint) o;
  }

  /*
   * The paint argument must be pre-checked for null.  Don't pass null in.
   */
  void overrideBorderPaint(int node, Paint paint)
  {
    if (super.borderPaint(node).equals(paint)) {
      m_borderPaints.remove(new Integer(node)); }
    else { m_borderPaints.put(new Integer(node), paint); }
  }

}
