package cytoscape.fung;

import java.awt.Color;
import java.awt.Paint;

public final class EdgeViewDefaults
{

  public final static Color DEFAULT_COLOR_LOW_DETAIL = Color.blue;
  public final static Color DEFAULT_SELECTED_COLOR_LOW_DETAIL = Color.red;
  public final static byte DEFAULT_SOURCE_ARROW = EdgeView.ARROW_NONE;
  public final static double DEFAULT_SOURCE_ARROW_SIZE = 2.0d;
  public final static Paint DEFAULT_SOURCE_ARROW_PAINT = Color.black;
  public final static byte DEFAULT_TARGET_ARROW = EdgeView.ARROW_NONE;
  public final static double DEFAULT_TARGET_ARROW_SIZE = 2.0d;
  public final static Paint DEFAULT_TARGET_ARROW_PAINT = Color.black;
  public final static double DEFAULT_SEGMENT_THICKNESS = 1.0d;
  public final static Paint DEFAULT_SEGMENT_PAINT = Color.blue;
  public final static Paint DEFAULT_SELECTED_SEGMENT_PAINT = Color.red;
  public final static double DEFAULT_SEGMENT_DASH_LENGTH = 0.0d;

  final Color m_colorLowDetail;
  final Color m_selectedColorLowDetail;
  final byte m_sourceArrow;
  final float m_sourceArrowSize;
  final Paint m_sourceArrowPaint;
  final byte m_targetArrow;
  final float m_targetArrowSize;
  final Paint m_targetArrowPaint;
  final float m_segmentThickness;
  final Paint m_segmentPaint;
  final Paint m_selectedSegmentPaint;
  final float m_segmentDashLength;

  public EdgeViewDefaults()
  {
    this(DEFAULT_COLOR_LOW_DETAIL, DEFAULT_SELECTED_COLOR_LOW_DETAIL,
         DEFAULT_SOURCE_ARROW, DEFAULT_SOURCE_ARROW_SIZE,
         DEFAULT_SOURCE_ARROW_PAINT, DEFAULT_TARGET_ARROW,
         DEFAULT_TARGET_ARROW_SIZE, DEFAULT_TARGET_ARROW_PAINT,
         DEFAULT_SEGMENT_THICKNESS, DEFAULT_SEGMENT_PAINT,
         DEFAULT_SELECTED_SEGMENT_PAINT, DEFAULT_SEGMENT_DASH_LENGTH);
  }

  public EdgeViewDefaults(final Color colorLowDetail,
                          final Color selectedColorLowDetail,
                          final byte sourceArrow,
                          final double sourceArrowSize,
                          final Paint sourceArrowPaint,
                          final byte targetArrow,
                          final double targetArrowSize,
                          final Paint targetArrowPaint,
                          final double segmentThickness,
                          final Paint segmentPaint,
                          final Paint selectedSegmentPaint,
                          final double segmentDashLength)
  {
    m_colorLowDetail = colorLowDetail;
    if (m_colorLowDetail.getAlpha() != 255) {
      throw new IllegalArgumentException("colorLowDetail must be opaque"); }
    m_selectedColorLowDetail = selectedColorLowDetail;
    if (m_selectedColorLowDetail.getAlpha() != 255) {
      throw new IllegalArgumentException
        ("selectedColorLowDetail must be opaque"); }
    m_sourceArrow = sourceArrow;
    switch (m_sourceArrow) {
    case EdgeView.ARROW_NONE:
    case EdgeView.ARROW_DELTA:
    case EdgeView.ARROW_DIAMOND:
    case EdgeView.ARROW_DISC:
    case EdgeView.ARROW_TEE:
      break;
    default:
      throw new IllegalArgumentException("sourceArrow is unrecognized"); }
    m_sourceArrowSize = (float) sourceArrowSize;
    m_sourceArrowPaint = sourceArrowPaint;
    if (m_sourceArrowPaint == null) {
      throw new NullPointerException("sourceArrowPaint is null"); }
    m_targetArrow = targetArrow;
    switch (m_targetArrow) {
    case EdgeView.ARROW_NONE:
    case EdgeView.ARROW_DELTA:
    case EdgeView.ARROW_DIAMOND:
    case EdgeView.ARROW_DISC:
    case EdgeView.ARROW_TEE:
      break;
    default:
      throw new IllegalArgumentException("targetArrow is unrecognized"); }
    m_targetArrowSize = (float) targetArrowSize;
    m_targetArrowPaint = targetArrowPaint;
    if (m_targetArrowPaint == null) {
      throw new NullPointerException("targetArrowPaint is null"); }
    m_segmentThickness = (float) segmentThickness;
    { // Check segment thickness and arrow sizes.
      if (!(m_segmentThickness >= 0.0f)) {
        throw new IllegalArgumentException("segmentThickness is negative"); }
      if (m_sourceArrow != EdgeView.ARROW_NONE &&
          !(m_sourceArrowSize >= m_segmentThickness)) {
        throw new IllegalArgumentException
          ("sourceArrowSize too small relative to segmentThickness"); }
      if (m_targetArrow != EdgeView.ARROW_NONE &&
          !(m_targetArrowSize >= m_segmentThickness)) {
        throw new IllegalArgumentException
          ("targetArrowSize too small relative to segmentThickness"); }
    }
    m_segmentPaint = segmentPaint;
    if (m_segmentPaint == null) {
      throw new NullPointerException("segmentPaint is null"); }
    m_selectedSegmentPaint = selectedSegmentPaint;
    if (m_selectedSegmentPaint == null) {
      throw new NullPointerException("selectedSegmentPaint is null"); }
    m_segmentDashLength = (float) segmentDashLength;
    if (!(m_segmentDashLength >= 0.0f)) {
      throw new IllegalArgumentException("segmentDashLength is negative"); }
  }

  public final Color getColorLowDetail()
  {
    return m_colorLowDetail;
  }

  public final Color getSelectedColorLowDetail()
  {
    return m_selectedColorLowDetail;
  }

  public final byte getSourceArrow()
  {
    return m_sourceArrow;
  }

  public final double getSourceArrowSize()
  {
    return m_sourceArrowSize;
  }

  public final Paint getSourceArrowPaint()
  {
    return m_sourceArrowPaint;
  }

  public final byte getTargetArrow()
  {
    return m_targetArrow;
  }

  public final double getTargetArrowSize()
  {
    return m_targetArrowSize;
  }

  public final Paint getTargetArrowPaint()
  {
    return m_targetArrowPaint;
  }

  public final double getSegmentThickness()
  {
    return m_segmentThickness;
  }

  public final Paint getSegmentPaint()
  {
    return m_segmentPaint;
  }

  public final Paint getSelectedSegmentPaint()
  {
    return m_selectedSegmentPaint;
  }

  public final double getSegmentDashLength()
  {
    return m_segmentDashLength;
  }

}
