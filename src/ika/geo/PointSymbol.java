/*
 * PointSymbol.java
 *
 * Created on May 13, 2005, 12:08 PM
 */

package ika.geo;

import java.awt.geom.*;
import java.awt.*;

/**
 *
 * @author jenny
 */
public class PointSymbol extends VectorSymbol implements java.io.Serializable {
    
    private static final long serialVersionUID = -755322865498970894L;
    
    /**
     * The radius used to draw a circle around the point. We mix here geometry
     * and graphic attributes, which is not how it should be done.
     */
    private double radius = 3;
    
    /**
     * The length of the radial lines used to draw this PointSymbol.
     */
    private double lineLength = 6;
    
    public PointSymbol() {
        this.filled = false;
        this.stroked = true;
        this.strokeColor = Color.BLACK;
        this.strokeWidth = 2;
    }
    
    public double getRadius() {
        return radius;
    }
    
    public void setRadius(double radius) {
        this.radius = radius;
    }
    
    public double getLineLength() {
        return lineLength;
    }
    
    public void setLineLength(double lineLength) {
        this.lineLength = lineLength;
    }
    
    /**
     * Returns a path that can be used to draw this point.
     * @param scale The current scale of the map.
     * @return The path that can be used to draw this point.
     */
    public GeoPath getPointSymbol(double scale, double x, double y) {
        
        // geometry
        final float lineLength = getScaledLineLength(scale);
        final float r = getScaledRadius(scale);
        final float fx = (float)x;
        final float fy = (float)y;
        GeoPath path = GeoPath.newCircle(fx, fy, r);
        if (lineLength > 0) {
            path.moveTo(r+fx, fy);
            path.lineTo(r + lineLength + fx, fy);
            path.moveTo(-r + fx, fy);
            path.lineTo(-r - lineLength + fx, fy);
            path.moveTo(fx, r + fy);
            path.lineTo(fx, r + lineLength + fy);
            path.moveTo(fx, -r + fy);
            path.lineTo(fx, -r - lineLength + fy);
        }
        
        // symbolization
        path.setVectorSymbol(this);
        
        return path;
        
    }
    
    public boolean isPointOnSymbol(java.awt.geom.Point2D point, double tolDist,
            double scale, double x, double y) {
        
        final double px = point.getX();
        final double py = point.getY();
        final double r = this.getScaledRadius(scale);
        final double strokeWidth = this.getScaledStrokeWidth(scale);
        final double halfStrokeWidth = strokeWidth / 2;
        final double lineLength = this.getScaledLineLength(scale);
        
        // test if point is in bounding box (including the radial lines).
        if (px < x - r - lineLength
                || px > x + r + lineLength
                || py < y - r - lineLength
                || py > y + r + lineLength)
            return false;
        
        // test if point is inside central circle
        final double dx = point.getX() - x;
        final double dy = point.getY() - y;
        final double dsquare = dx*dx+dy*dy;
        if (dsquare <=  (r+halfStrokeWidth)*(r+halfStrokeWidth))
            return true;
        
        // test if point is on one of the straight lines
        // right
        if (px >= x + r
                && px <= x + r + lineLength
                && py >= y - halfStrokeWidth
                && py <= y + halfStrokeWidth)
            return true;
        // left
        if (px >= x - r - lineLength
                && px <= x - r
                && py >= y - halfStrokeWidth
                && py <= y + halfStrokeWidth)
            return true;
        // bottom
        if (px >= x - halfStrokeWidth
                && px <= x + halfStrokeWidth
                && py >= y - r - lineLength
                && py <= y - r)
            return true;
        // top
        if (px >= x - halfStrokeWidth
                && px <= x + halfStrokeWidth
                && py >= y + r
                && py <= y + r + lineLength)
            return true;
        
        return false;
    }
    
    private float getScaledLineLength(double scale) {
        final double length = this.getLineLength();
        return scaleInvariant ? (float)(length / scale) : (float)length;
    }
    
    private float getScaledRadius(double scale) {
        final double r = this.getRadius();
        return scaleInvariant ? (float)(r / scale) : (float)r;
    }
    
    protected void drawPointSymbol(RenderParams rp, boolean isSelected, double x, double y) {
        final GeoPath pointSymbol = this.getPointSymbol(rp.scale, x, y);
        pointSymbol.drawNormalState(rp);
    }
}
