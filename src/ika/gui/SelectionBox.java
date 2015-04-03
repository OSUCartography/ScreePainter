/*
 * SelectionBox.java
 *
 * Created on May 31, 2006, 5:06 PM
 *
 */

package ika.gui;

import java.awt.geom.*;
import java.awt.*;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class SelectionBox {
    
    public static final int SELECTION_HANDLE_NONE = -1;
    
    // counter clock-wise direction starting with lower left handle
    public static final int SELECTION_HANDLE_LOWER_LEFT = 0;
    public static final int SELECTION_HANDLE_LOWER_RIGHT = 1;
    public static final int SELECTION_HANDLE_UPPER_RIGHT= 2;
    public static final int SELECTION_HANDLE_UPPER_LEFT  = 3;
    public static final int SELECTION_HANDLE_UPPER_CENTER  = 4;
    public static final int SELECTION_HANDLE_LOWER_CENTER  = 5;
    public static final int SELECTION_HANDLE_LEFT_CENTER  = 6;
    public static final int SELECTION_HANDLE_RIGHT_CENTER  = 7;
    public static final double HANDLE_BOX_SIZE = 6;
    
    public static final double SELECTED_STROKE_WIDTH = 0.5;
    
    public static Rectangle2D[] getSelectionBoxHandles(Rectangle2D selectionBox,
            double mapScale) {
        
        if (selectionBox == null)
            return null;
        
        Rectangle2D[] handles = new Rectangle2D[8];
        final double minX = selectionBox.getMinX();
        final double minY = selectionBox.getMinY();
        final double maxX = selectionBox.getMaxX();
        final double maxY = selectionBox.getMaxY();
        final double centerX = (minX + maxX) / 2;
        final double centerY = (minY + maxY) / 2;
        
        final double handleDim = HANDLE_BOX_SIZE / mapScale;
        final double halfHandleDim = handleDim / 2.;
        handles[SELECTION_HANDLE_LOWER_LEFT] = new Rectangle2D.Double(
                minX - halfHandleDim, minY - halfHandleDim, handleDim, handleDim);
        handles[SELECTION_HANDLE_LOWER_RIGHT] = new Rectangle2D.Double(
                maxX - halfHandleDim, minY - halfHandleDim, handleDim, handleDim);
        handles[SELECTION_HANDLE_UPPER_RIGHT] = new Rectangle2D.Double(
                maxX - halfHandleDim, maxY - halfHandleDim, handleDim, handleDim);
        handles[SELECTION_HANDLE_UPPER_LEFT] = new Rectangle2D.Double(
                minX - halfHandleDim, maxY - halfHandleDim, handleDim, handleDim);
        
        handles[SELECTION_HANDLE_UPPER_CENTER] = new Rectangle2D.Double(
                centerX - halfHandleDim, maxY - halfHandleDim, handleDim, handleDim);
        handles[SELECTION_HANDLE_LOWER_CENTER] = new Rectangle2D.Double(
                centerX - halfHandleDim, minY - halfHandleDim, handleDim, handleDim);
        handles[SELECTION_HANDLE_LEFT_CENTER] = new Rectangle2D.Double(
                minX - halfHandleDim, centerY - halfHandleDim, handleDim, handleDim);
        handles[SELECTION_HANDLE_RIGHT_CENTER] = new Rectangle2D.Double(
                maxX - halfHandleDim, centerY - halfHandleDim, handleDim, handleDim);
        
        return handles;
    }
    
    public static void paintSelectionBox(Rectangle2D selectionBox,
            Graphics2D g2d, double mapScale, boolean drawHandles) {
        
        // don't test for selectionBox.isEmpty() which returns true if
        // the width or height are negative!
        
        if (selectionBox == null || g2d == null)
            return;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        
        // setup color
        final Color highlightColor = ika.utils.ColorUtils.getHighlightColor();
        g2d.setColor(highlightColor);
        
        // setup stroke. Use a line width of 0 for a thin line. This is ok, see
        // java bug #4114921 and #4093921.
        g2d.setStroke(new BasicStroke(0));
        
        // draw selection box
        double w = selectionBox.getWidth();
        double h = selectionBox.getHeight();
        
        // make sure the rectangle has positive width and height for drawing
        if (w >= 0 && h >= 0) {
            g2d.draw(selectionBox);
        } else {
            final double x = selectionBox.getX();
            final double y = selectionBox.getY();
            if (w < 0 && h < 0) {
                w = Math.abs(w);
                h = Math.abs(h);
                g2d.draw(new Rectangle2D.Double(x-w, y-h, w, h));
            } else if (w < 0) {
                w = Math.abs(w);
                g2d.draw(new Rectangle2D.Double(x-w, y, w, h));
            } else {
                h = Math.abs(h);
                g2d.draw(new Rectangle2D.Double(x, y-h, w, h));
            }
        }
        
        // draw handles
        if (drawHandles) {
            Rectangle2D[] handles = SelectionBox.getSelectionBoxHandles(
                    selectionBox, mapScale);
            if (handles != null) {
                g2d.setColor(Color.WHITE);
                for (int i = 0; i < handles.length; i++)
                    g2d.fill(handles[i]);
                g2d.setColor(highlightColor);
                for (int i = 0; i < handles.length; i++)
                    g2d.draw(handles[i]);
            }
        }

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
    }
    
    /**
     * Returns a handle ID for a passed point.
     * @param point The point for which to search the handle ID.
     * @selectionBox The bounding box.
     * @mapScale The current map scale.
     * @return The handle ID between 0 and 7, or SELECTION_HANDLE_NONE if
     * the passed point is not on a handle.
     */
    public static int findBoxHandle(Point2D point,
            Rectangle2D selectionBox, double mapScale) {
        
        Rectangle2D[] handles = SelectionBox.getSelectionBoxHandles(selectionBox, mapScale);
        if (handles != null) {
            for (int i = 0; i < handles.length; i++) {
                if (handles[i].contains(point))
                    return i;
            }
        }
        return SELECTION_HANDLE_NONE;
    }
    
    public static void adjustSelectionBox(Rectangle2D selectionBox,
            int draggedHandle,
            Point2D.Double point,
            boolean uniformScaling,
            double initialRatio) {
        
        final double x = selectionBox.getMinX();
        final double y = selectionBox.getMinY();
        final double w = selectionBox.getWidth();
        final double h = selectionBox.getHeight();
        final double px = point.getX();
        final double py = point.getY();
        double newX = x;
        double newY = y;
        double newW = w;
        double newH = h;
        switch (draggedHandle) {
            // corner handles
            case SelectionBox.SELECTION_HANDLE_LOWER_LEFT:
                newX = px;
                newY = py;
                newW = w + x - px;
                newH = h + y - py;
                break;
            case SelectionBox.SELECTION_HANDLE_LOWER_RIGHT:
                newY = py;
                newW = px - x;
                newH = h + y - py;
                break;
            case SelectionBox.SELECTION_HANDLE_UPPER_RIGHT:
                newW = px - x;
                newH = py - y;
                break;
            case SelectionBox.SELECTION_HANDLE_UPPER_LEFT:
                newX = px;
                newW = w + x - px;
                newH = py - y;
                break;
                
                // central handles
            case SelectionBox.SELECTION_HANDLE_UPPER_CENTER:
                newH = py - y;
                break;
            case SelectionBox.SELECTION_HANDLE_LOWER_CENTER:
                newY = py;
                newH = h + y - py;
                break;
            case SelectionBox.SELECTION_HANDLE_LEFT_CENTER:
                newX = px;
                newW = w + x - px;
                break;
            case SelectionBox.SELECTION_HANDLE_RIGHT_CENTER:
                newW = px - x;
                break;
        }
        
        if (uniformScaling) {
            final double wRatio = newW / w;
            final double hRatio = newH / h;
            if (wRatio < hRatio) {
                newH = newW / initialRatio;
            } else {
                newW = initialRatio * newH;
            }
        }
        
        selectionBox.setFrame(newX, newY, newW, newH);
    }
}
