package ika.geo.grid;

import ika.geo.GeoGrid;

/**
 * Computes the absolute values in a grid.
 * @author jenny
 */
public class GridAbsoluteOperator implements GridOperator{

    public String getName() {
        return "Absolute";
    }

    public GeoGrid operate(GeoGrid geoGrid) {
        
        if (geoGrid == null)
            throw new IllegalArgumentException();
        
        final int nrows = geoGrid.getRows();
        final int ncols = geoGrid.getCols();
        GeoGrid newGrid = new GeoGrid(ncols, nrows, geoGrid.getCellSize());
        newGrid.setWest(geoGrid.getWest());
        newGrid.setNorth(geoGrid.getNorth());
        
        float[][] srcGrid = geoGrid.getGrid();
        float[][] dstGrid = newGrid.getGrid();
        
        for (int row = 0; row < nrows; ++row) {
            float[] srcRow = srcGrid[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < ncols; ++col) {
                dstRow[col] = Math.abs(srcRow[col]);
            }
        }
        return newGrid;
    }

}
