/*
 * 
 * 
 */
package ika.geo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

/**
 * Extends GeoPath with a fast, but limited contains test. This is not a general
 * purpose path, but should only be used when fast inside/outside tests are
 * needed for a static path.
 *
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
        VectorSymbol symbol = getVectorSymbol().clone();
        symbol.setFilled(true);
        symbol.setFillColor(Color.BLACK);
        symbol.setStroked(false);
        setVectorSymbol(symbol);
    }

    public void initContainsTest(double cellSize) {

        Rectangle2D bounds = getBounds2D(GeoObject.UNDEFINED_SCALE);
        long cols = (long) Math.ceil(bounds.getWidth() / cellSize);
        long rows = (long) Math.ceil(bounds.getHeight() / cellSize);

        while (cols * rows > Integer.MAX_VALUE) {
            cellSize *= 2;
            cols = (long) Math.ceil(bounds.getWidth() / cellSize);
            rows = (long) Math.ceil(bounds.getHeight() / cellSize);
        }
        
        this.cellSize = cellSize;

        // setup image to rasterize objects
        BufferedImage img = new BufferedImage((int) cols, (int) rows, BufferedImage.TYPE_BYTE_BINARY);
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
        final double scale = 1. / cellSize;
        rasterizerG2d.scale(scale, -scale);
        rasterizerG2d.translate(-bounds.getMinX(), -bounds.getMaxY());

        // apply symbol of GeoPath
        VectorSymbol vectorSymbol = getVectorSymbol();
        rasterizerG2d.setStroke(new BasicStroke((float) vectorSymbol.getStrokeWidth()));

        // render path
        Path2D.Double p = toPath();
        if (vectorSymbol.isFilled()) {
            rasterizerG2d.fill(p);
        }
        if (vectorSymbol.isStroked()) {
            rasterizerG2d.draw(p);
        }

    }

    @Override
    public final boolean contains(double x, double y) {
        final Rectangle2D bounds = this.getBounds2D(GeoObject.UNDEFINED_SCALE);
        final double west = bounds.getMinX();
        final double north = bounds.getMaxY();
        final int c = (int) Math.floor((x - west) / cellSize);
        final int r = (int) Math.floor((north - y) / cellSize);
        final int w = raster.getWidth();
        final int h = raster.getHeight();
        return r >= 0 && c >= 0 && r < h && c < w && raster.getSample(c, r, 0) == 1;
    }

}
