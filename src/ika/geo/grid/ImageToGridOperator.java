/*
 * ImageToGridOperator.java
 *
 * Created on October 25, 2007, 4:20 PM
 *
 */

package ika.geo.grid;

import ika.geo.GeoGrid;
import ika.geo.GeoGridShort;
import ika.geo.GeoImage;
import java.awt.image.BufferedImage;

/**
 * Convert a GeoImage to a GeoGrid. RGB colors are converted to gray and stored
 * with a range of 0 to 255.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ImageToGridOperator {
    
    /** Creates a new instance of ImageToGridOperator */
    public ImageToGridOperator() {
    }
    
    /** Returns a descriptive name of this GridOperator
     * @return The name of this GridOperator.
     */
    public String getName() {
        return "Grid to Image";
    }
    
    public GeoGrid operate (GeoImage geoImage) {       
        int cols = geoImage.getCols();
        int rows = geoImage.getRows();
        double cellSize = geoImage.getCellSize();
        GeoGrid grid = new GeoGrid(cols, rows, cellSize);
        grid.setWest(geoImage.getWest() + 0.5 * cellSize);
        grid.setNorth(geoImage.getNorth() - 0.5 * cellSize);
                
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                grid.setValue(geoImage.getGray(col, row), col, row);
            }
        }
        return grid;
    }

    public GeoGridShort operateToShort (GeoImage geoImage) {
        int cols = geoImage.getCols();
        int rows = geoImage.getRows();
        double cellSize = geoImage.getCellSize();
        GeoGridShort grid = new GeoGridShort(cols, rows, cellSize);
        grid.setWest(geoImage.getWest() + 0.5 * cellSize);
        grid.setNorth(geoImage.getNorth() - 0.5 * cellSize);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                grid.setValue((short)geoImage.getGray(col, row), col, row);
            }
        }
        return grid;
    }
}
