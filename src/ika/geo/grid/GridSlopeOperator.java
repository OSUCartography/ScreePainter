/*
 * GridSlopeOperator.java
 *
 * Created on January 28, 2006, 2:11 PM
 *
 */

package ika.geo.grid;

import ika.geo.*;

/**
 *
 * @author jenny
 */
public class GridSlopeOperator implements GridOperator {
    
    /** Creates a new instance of GridSlopeOperator */
    public GridSlopeOperator() {
    }
    
    public String getName() {
        return "Grid Slope";
    }
    
    public ika.geo.GeoGrid operate(ika.geo.GeoGrid geoGrid) {
        if (geoGrid == null)
            throw new IllegalArgumentException();
        
        final int newCols = geoGrid.getCols() - 2;
        final int newRows = geoGrid.getRows() - 2;
        final double meshSize = geoGrid.getCellSize();
        GeoGrid newGrid = new GeoGrid(newCols, newRows, meshSize);
        newGrid.setWest(geoGrid.getWest() + meshSize);
        newGrid.setNorth(geoGrid.getNorth() + meshSize);
        
        float[][] srcGrid = geoGrid.getGrid();
        float[][] dstGrid = newGrid.getGrid();
        final int srcRows = geoGrid.getRows();
        final int srcCols = geoGrid.getCols();
        final double inverseDoubleMeshSize = 1. / (2. * meshSize);
        
        for (int row = 1; row < srcRows - 1; row++) {
            for (int col = 1; col < srcCols - 1; col++) {
                final float w = srcGrid[row][col-1];
                final float e = srcGrid[row][col+1];
                final float s = srcGrid[row+1][col];
                final float n = srcGrid[row-1][col];
                final double dH = (e - w);
                final double dV = (n - s);
                final float slope = (float)(Math.atan(
                        Math.sqrt(dH*dH+dV*dV)*inverseDoubleMeshSize));
                dstGrid[row-1][col-1] = slope;
            }
        }
   
        return newGrid;
    }
}
