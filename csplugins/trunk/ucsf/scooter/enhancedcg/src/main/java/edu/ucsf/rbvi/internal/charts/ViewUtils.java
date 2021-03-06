/* vim: set ts=2: */
/**
 * Copyright (c) 2010 The Regents of the University of California.
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
package edu.ucsf.rbvi.enhancedcg.internal.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// System imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Cytoscape imports

/**
 * The NodeChartViewer creates the actual custom graphics
 */
public class ViewUtils {

	public enum Position {
		CENTER ("center"),
		EAST ("east"),
		NORTH ("north"),
		NORTHEAST ("northeast"),
		NORTHWEST ("northwest"),
		SOUTH ("south"),
		SOUTHEAST ("southeast"),
		SOUTHWEST ("southwest"),
		WEST ("west");
	
		private String label;
		private static Map<String, Position>pMap;
	
		Position(String label) { 
			this.label = label; 
			addPosition(this);
		}
	
		public String getLabel() {
			return label;
		}

		public String toString() {
			return label;
		}
	
		private void addPosition(Position pos) {
			if (pMap == null) pMap = new HashMap<String,Position>();
			pMap.put(pos.getLabel(), pos);
		}
	
		static Position getPosition(String label) {
			if (pMap.containsKey(label))
				return pMap.get(label);
			return null;
		}
	}

	/**
 	 * getPosition will return either a Point2D or a Position, depending on whether
 	 * the user provided us with a position keyword or a specific value.
 	 *
 	 * @param position the position argument
 	 * @return a Point2D representing the X,Y offset specified by the user or a Position
 	 * enum that corresponds to the provided keyword.  <b>null</b> is returned if the input
 	 * is illegal.
 	 */
	public static Object getPosition(String position) {
		Position pos = Position.getPosition(position);
		if (pos != null) 
			return pos;

		String [] xy = position.split(",");
		if (xy.length != 2) {
			return null;
		}

		try {
			Double x = Double.valueOf(xy[0]);
			Double y = Double.valueOf(xy[1]);
			return new Point2D.Double(x.doubleValue(), y.doubleValue());
		} catch (NumberFormatException e) {
			return null;
		}
	}


	private static final String DEFAULT_FONT=Font.SANS_SERIF;
	private static final int DEFAULT_STYLE=Font.PLAIN;
	private static final int DEFAULT_SIZE=8;

	public static enum TextAlignment {ALIGN_LEFT, ALIGN_CENTER_TOP, ALIGN_RIGHT, ALIGN_CENTER_BOTTOM, ALIGN_MIDDLE};

	public static Shape getLabelShape(String label, String fontName, int fontStyle, int fontSize) {
		if (fontName == null) fontName = DEFAULT_FONT;
		if (fontStyle == 0) fontStyle = DEFAULT_STYLE;
		if (fontSize == 0) fontSize = DEFAULT_SIZE;

		Font font = new Font(fontName, fontStyle, fontSize);
		// Get the canvas so that we can find the graphics context
		FontRenderContext frc = new FontRenderContext(null, false, false);
		TextLayout tl = new TextLayout(label, font, frc);
		return tl.getOutline(null);
	}

	public static Shape positionLabel(Shape lShape, Point2D position, TextAlignment tAlign, 
	                                  double maxHeight, double maxWidth, double rotation) {

		// System.out.println("  Label = "+label);

		// Figure out how to move the text to center it on the bbox
		double textWidth = lShape.getBounds2D().getWidth(); 
		double textHeight = lShape.getBounds2D().getHeight();

		// Before we go any further, scale the text, if necessary
		if (maxHeight > 0.0 || maxWidth > 0.0) {
			double scaleWidth = 1.0;
			double scaleHeight = 1.0;
			if (maxWidth > 0.0 && textWidth > maxWidth)
				scaleWidth = maxWidth/textWidth * 0.9;
			if (maxHeight > 0.0 && textHeight > maxHeight)
				scaleHeight = maxHeight/textHeight * 0.9;

			double scale = Math.min(scaleWidth, scaleHeight);

			// We don't want to scale down too far.  If scale < 20% of the font size, skip the label
			if (scale < 0.20)
				return null;
			// System.out.println("scale = "+scale);
			AffineTransform sTransform = new AffineTransform();
			sTransform.scale(scale, scale);
			lShape = sTransform.createTransformedShape(lShape);
		}

		// System.out.println("  Text size = ("+textWidth+","+textHeight+")");

		double pointX = position.getX();
		double pointY = position.getY();

		double textStartX = pointX;
		double textStartY = pointY;

		switch (tAlign) {
		case ALIGN_CENTER_TOP:
			// System.out.println("  Align = CENTER_TOP");
			textStartX = pointX - textWidth/2;
			textStartY = pointY - textHeight/2;
			break;
		case ALIGN_CENTER_BOTTOM:
			// System.out.println("  Align = CENTER_BOTTOM");
			textStartX = pointX - textWidth/2;
			textStartY = pointY + textHeight;
			break;
		case ALIGN_RIGHT:
			// System.out.println("  Align = RIGHT");
			textStartX = pointX - textWidth;
			textStartY = pointY + textHeight/2;
			break;
		case ALIGN_LEFT:
			// System.out.println("  Align = LEFT");
			textStartX = pointX;
			textStartY = pointY + textHeight/2;
			break;
		case ALIGN_MIDDLE:
			textStartX = pointX - textWidth/2;;
			textStartY = pointY + textHeight/2;
			break;
		default:
			// System.out.println("  Align = "+tAlign);
		}

		// System.out.println("  Text bounds = "+lShape.getBounds2D());
		// System.out.println("  Position = "+position);

		// System.out.println("  Offset = ("+textStartX+","+textStartY+")");

		// Use the bounding box to create an Affine transform.  We may need to scale the font
		// shape if things are too cramped, but not beneath some arbitrary minimum
		AffineTransform trans = new AffineTransform();
		if (rotation != 0.0)
			trans.rotate(Math.toRadians(rotation), pointX, pointY);
		trans.translate(textStartX, textStartY);

		// System.out.println("  Transform: "+trans);
		return trans.createTransformedShape(lShape);
	}

	/**
 	 * This is used to draw a line from a text box to an object -- for example from a pie label to
 	 * the pie slice itself.
 	 */
	public static Shape getLabelLine(Rectangle2D textBounds, Point2D labelPosition, TextAlignment tAlign) {
		double lineStartX = 0;
		double lineStartY = 0;
		switch (tAlign) {
			case ALIGN_CENTER_TOP:
				lineStartY = textBounds.getMaxY()+1;
				lineStartX = textBounds.getCenterX();
			break;
			case ALIGN_CENTER_BOTTOM:
				lineStartY = textBounds.getMinY()-1;
				lineStartX = textBounds.getCenterX();
			break;
			case ALIGN_RIGHT:
				lineStartY = textBounds.getCenterY();
				lineStartX = textBounds.getMaxX()+1;
			break;
			case ALIGN_LEFT:
				lineStartY = textBounds.getCenterY();
				lineStartX = textBounds.getMinX()-1;
			break;
		}

		BasicStroke stroke = new BasicStroke(0.5f);
		return stroke.createStrokedShape(new Line2D.Double(lineStartX, lineStartY, labelPosition.getX(), labelPosition.getY()));
	}

	private static Rectangle2D positionAdjust(Rectangle2D bbox, double nodeHeight, double nodeWidth, Object pos) {
		if (pos == null)
			return bbox;

		double height = bbox.getHeight();
		double width = bbox.getWidth();
		double x = bbox.getX();
		double y = bbox.getY();

		if (pos instanceof Position) {
			Position p = (Position) pos;

			switch (p) {
			case EAST:
				x = nodeWidth/2;
				break;
			case WEST:
				x = -nodeWidth*1.5;
				break;
			case NORTH:
				y = -nodeHeight*1.5;
				break;
			case SOUTH:
				y = nodeHeight/2;
				break;
			case NORTHEAST:
				x = nodeWidth/2;
				y = -nodeHeight*1.5;
				break;
			case NORTHWEST:
				x = -nodeWidth*1.5;
				y = -nodeHeight*1.5;
				break;
			case SOUTHEAST:
				x = nodeWidth/2;
				y = nodeHeight/2;
				break;
			case SOUTHWEST:
				x = -nodeWidth*1.5;
				y = nodeHeight/2;
				break;
			case CENTER:
			default:
			}
		} else if (pos instanceof Point2D.Double) {
			x += ((Point2D.Double)pos).getX();
			y += ((Point2D.Double)pos).getY();
		}

		return new Rectangle2D.Double(x,y,width,height);
	}

}
