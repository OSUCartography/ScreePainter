/*
 * 
 * 
 */
package ika.geo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

/**
 * Extends GeoPath with a fast, but limited contains test. This is not a general 
 * purpose path, but should only be used when fast inside/outside tests are
 * needed for a static path.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class FastContainsGeoPath extends GeoPath {

    private Raster raster;

    /**
     * size of a pixel in the raster.
     */
    private double cellSize;
    
    public FastContainsGeoPath(GeoPath geoPath) {
        super(geoPath);
        VectorSymbol symbol = this.getVectorSymbol().clone();
        symbol.setFilled(true);
        symbol.setFillColor(Color.BLACK);
        symbol.setStroked(false);
        this.setVectorSymbol(symbol);
    }
    
    public void initContainsTest (double cellSize) {

        this.cellSize = cellSize;
        
        Rectangle2D bounds = this.getBounds2D(GeoObject.UNDEFINED_SCALE);
        int cols = (int) Math.ceil(bounds.getWidth() / cellSize);
        int rows = (int) Math.ceil(bounds.getHeight() / cellSize);

        // setup image to rasterize objects
        BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_BYTE_BINARY);
        this.raster = img.getRaster();
        Graphics2D rasterizerG2d = img.createGraphics();
        rasterizerG2d.setColor(Color.white);
        rasterizerG2d.setBackground(Color.black);
        rasterizerG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        
        // transform from Geo space to user space, last transformation first!
        /*
         * Transformation:
         * x_ = (x-west)*scale;
         * y_ = (north-y)*scale = (y-north)*(-scale);
         */
        final double scale = 1./cellSize;
        rasterizerG2d.scale(scale, -scale);
        rasterizerG2d.translate(-bounds.getMinX(), -bounds.getMaxY());
        
        // apply symbol of GeoPath
        VectorSymbol vectorSymbol = this.getVectorSymbol();
        rasterizerG2d.setStroke(new BasicStroke((float) vectorSymbol.getStrokeWidth()));
        
        // render path
        GeneralPath path = new GeneralPath();
        path.append(this.toPathIterator(null), false);
        if (vectorSymbol.isFilled()) {
            rasterizerG2d.fill(path);
        }
        if (vectorSymbol.isStroked()) {
            rasterizerG2d.draw(path);
        }

    }
    
    @Override
    public final boolean contains (double x, double y) {
        final Rectangle2D bounds = this.getBounds2D(GeoObject.UNDEFINED_SCALE);
        final double west = bounds.getMinX();
        final double north = bounds.getMaxY();
        final int c = (int)Math.floor((x - west) / cellSize);
        final int r = (int)Math.floor((north - y) / cellSize);
        final int w = this.raster.getWidth();
        final int h = this.raster.getHeight();
        return r >= 0 && c >= 0 && r < h && c < w && raster.getSample(c, r, 0) == 1;
    }
    
}
