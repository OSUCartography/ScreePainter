/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ika.geo.grid;

import ika.geo.GeoGrid;
import ika.geo.GeoObject;

/**
 *
 * @author jenny
 */
public class GridCombineOperator implements GridOperator{

    public String getName() {
        return "Combination";
    }

    public GeoObject operate(GeoGrid geoGrid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public GeoGrid operate(GeoGrid grid1, GeoGrid grid2, GeoGrid weightGrid) {
        
        if (grid1 == null || grid2 == null || weightGrid == null)
            throw new IllegalArgumentException();
        if (grid1.getCols() != grid2.getCols() || grid1.getCols() != weightGrid.getCols())
            throw new IllegalArgumentException("Grids must be of same width");
        if (grid1.getRows() != grid2.getRows() || grid1.getRows() != weightGrid.getRows())
            throw new IllegalArgumentException("Grids must be of same height");
        if (grid1.getCellSize() != grid2.getCellSize() || grid1.getCellSize() != weightGrid.getCellSize())
            throw new IllegalArgumentException("Grids must be of same cell size");
        
        final int nrows = grid1.getRows();
        final int ncols = grid1.getCols();
        GeoGrid newGrid = new GeoGrid(ncols, nrows, grid1.getCellSize());
        newGrid.setWest(grid1.getWest());
        newGrid.setNorth(grid1.getNorth());
        
        float[][] src1 = grid1.getGrid();
        float[][] src2 = grid2.getGrid();
        float[][] w = weightGrid.getGrid();
        float[][] dstGrid = newGrid.getGrid();
        
        for (int row = 0; row < nrows; ++row) {
            float[] srcRow1 = src1[row];
            float[] srcRow2 = src2[row];
            float[] wRow = w[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < ncols; ++col) {
                dstRow[col] = srcRow1[col] * wRow[col] + srcRow2[col] * (1f - wRow[col]);
            }
        }
        return newGrid;
        
    }

    /**
     * Sums the values of two grids.
     * @param grid1
     * @param grid2
     * @return
     */
    public GeoGrid operate(GeoGrid grid1, GeoGrid grid2) {

        if (grid1 == null || grid2 == null)
            throw new IllegalArgumentException();
        if (grid1.getCols() != grid2.getCols())
            throw new IllegalArgumentException("Grids must be of same width");
        if (grid1.getRows() != grid2.getRows())
            throw new IllegalArgumentException("Grids must be of same height");
        if (grid1.getCellSize() != grid2.getCellSize())
            throw new IllegalArgumentException("Grids must be of same cell size");

        final int nrows = grid1.getRows();
        final int ncols = grid1.getCols();
        GeoGrid newGrid = new GeoGrid(ncols, nrows, grid1.getCellSize());
        newGrid.setWest(grid1.getWest());
        newGrid.setNorth(grid1.getNorth());

        float[][] src1 = grid1.getGrid();
        float[][] src2 = grid2.getGrid();
        float[][] dstGrid = newGrid.getGrid();

        for (int row = 0; row < nrows; ++row) {
            float[] srcRow1 = src1[row];
            float[] srcRow2 = src2[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < ncols; ++col) {
                dstRow[col] = srcRow1[col] + srcRow2[col];
            }
        }
        return newGrid;

    }
}
