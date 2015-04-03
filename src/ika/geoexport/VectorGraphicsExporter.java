/*
 * VectorGraphicsExporter.java
 *
 * Created on April 23, 2007, 9:56 AM
 *
 */

package ika.geoexport;

import ika.geo.GeoObject;
import ika.geo.GeoSet;
import ika.gui.PageFormat;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public abstract class VectorGraphicsExporter extends GeoSetExporter {
    
    /** rounding of coordinates */
    private static int NBR_AFTER_COMA_DECIMALS = 2;
    private static float AFTER_COMA_ROUNDER = 
            (float)Math.pow(10, NBR_AFTER_COMA_DECIMALS);
    
    protected PageFormat pageFormat = null;
    
    /** Creates a new instance of VectorGraphicsExporter */
    public VectorGraphicsExporter() {
    }

    public PageFormat getPageFormat() {
        return pageFormat;
    }

    public void setPageFormat(PageFormat pageFormat) {
        this.pageFormat = pageFormat;
    }
    
    public double getMaximumExportSize() {
        return Double.MAX_VALUE;
    }
    
    @Override
    public void export (GeoSet geoSet, OutputStream outputStream)
    throws IOException {
        
        // make sure there is a valid page format defined.
        if (this.pageFormat == null) {
            Rectangle2D bounds = geoSet.getBounds2D(GeoObject.UNDEFINED_SCALE);
            double maxSize = this.getMaximumExportSize();
            if (maxSize == Double.MAX_VALUE)
                maxSize = 1000;
            double scale = maxSize / Math.max(bounds.getWidth(), bounds.getHeight());
            this.pageFormat = new PageFormat();
            this.pageFormat.setPageScale(scale);
            this.pageFormat.setPageLeft(bounds.getMinX());
            this.pageFormat.setPageBottom(bounds.getMinY());
            this.pageFormat.setPageHeightWorldCoordinates(bounds.getHeight());
            this.pageFormat.setPageWidthWorldCoordinates(bounds.getWidth());
        }
        
        super.export(geoSet, outputStream);
    }
    
    /**
     * Transforms a horizontal x coordinate to the scale 
     * and bounding box defined by the PageFormat.
     * @param x The horizontal coordinate.
     * @return Returns the coordinate in the page coordinate system.
     */
    protected float xToPage(float x) {
        final double mapScale = this.pageFormat.getPageScale();
        final double west = this.pageFormat.getPageLeft();
        return (float)((x - west) / mapScale);
    }
    
    /**
     * Transforms a horizontal x coordinate (usually in meters) to pixels. Takes 
     * the scale and bounding box defined by the PageFormat into account.
     * @param x The horizontal coordinate.
     * @return Returns the coordinate in pixels.
     */
    protected float xToPagePx(float x) {
        return (float)(this.xToPage(x) * 1000 * PageFormat.MM2PX);
    }
    
    /**
     * Transforms a vertical y coordinate to the scale 
     * and bounding box defined by the PageFormat.
     * @param y The vertical coordinate.
     * @return Returns the coordinate in the page coordinate system.
     */
    protected float yToPage(float y) {
        final double mapScale = this.pageFormat.getPageScale();
        final double north = this.pageFormat.getPageTop();
        return (float)((north - y) / mapScale);
    }
    
    /**
     * Transforms a vertical y coordinate (usually in meters) to pixels. Takes 
     * the scale and bounding box defined by the PageFormat into account. Converts
     * the direction of the coordinates from bottom-to-top to top-to-bottom.
     * @param y The vertical coordinate.
     * @return Returns the coordinate in pixels.
     */
    protected float yToPagePx(float y) {
        return (float)(this.yToPage(y) * 1000 * PageFormat.MM2PX);
    }
        
    /**
     * Transforms a distance to the scale defined by the PageFormat.
     * @param d The distance to transform.
     * @return The scaled distance.
     */
    protected float dimToPage(float d) {
        final double mapScale = this.pageFormat.getPageScale();
        return (float)(d / mapScale);
    }
    
    /**
     * Transforms a distance to pixels. Takes the scale defined by the 
     * PageFormat into acount.
     * @param d The distance to transform.
     * @return The scaled distance in pixels.
     */
    protected float dimToPagePx(float d) {
        return (float)(this.dimToPage(d) * 1000 * PageFormat.MM2PX);
    }
    
    /**
     * Transforms and rounds a horizontal x coordinate (usually in meters) 
     * to pixels. Takes the scale and bounding box defined by the PageFormat 
     * into acount.
     * @param x The horizontal coordinate.
     * @return Returns the coordinate in pixels.
     */
    protected float xToPageRoundedPx(float x) {
        return this.round(this.xToPagePx(x));
    }
    
    /**
     * Transforms and rounds a vertical y coordinate (usually in meters) 
     * to pixels. Takes the scale and bounding box defined by the PageFormat 
     * into acount.
     * @param y The vertical coordinate.
     * @return Returns the coordinate in pixels.
     */
    protected float yToPageRoundedPx(float y) {
        return this.round(this.yToPagePx(y));
    }
    
    /**
     * Transforms and rounds a distance to pixels. Takes the scale defined by the 
     * PageFormat into acount.
     * @param d The distance to transform.
     * @return The distance in pixels.
     */
    protected float dimToPageRoundedPx(float d) {
        return this.round(this.dimToPagePx(d));
    }
    
    private float round(float d){
        return Math.round(AFTER_COMA_ROUNDER * d) / AFTER_COMA_ROUNDER;
    }
}
