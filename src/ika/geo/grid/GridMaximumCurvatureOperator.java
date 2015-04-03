package ika.geo.grid;

import ika.geo.GeoGrid;

/**
 *
 * http://www.soi.city.ac.uk/~jwo/phd/04param.php
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class GridMaximumCurvatureOperator implements GridOperator {

    public String getName() {
        return "Maximum Curvature";
    }

    public GeoGrid operate(GeoGrid geoGrid) {
        if (geoGrid == null)
            throw new IllegalArgumentException();
        
        final int cols = geoGrid.getCols();
        final int rows = geoGrid.getRows();
        final double cellSize = geoGrid.getCellSize();
        final float gg = (float)(cellSize*cellSize);
        GeoGrid newGrid = new GeoGrid(cols, rows, cellSize);
        newGrid.setName(this.getName());
        newGrid.setWest(geoGrid.getWest());
        newGrid.setNorth(geoGrid.getNorth());
        
        // top row
        for (int col = 0; col < cols; col++) {
            this.operateBorder(geoGrid, newGrid, col, 0, cellSize);
        }
        // bottom row
        for (int col = 0; col < cols; col++) {
            this.operateBorder(geoGrid, newGrid, col, rows - 1, cellSize);
        }
        // left column
        for (int row = 1; row < rows - 1; row++) {
            this.operateBorder(geoGrid, newGrid, 0, row, cellSize);
        }
        // right column
        for (int row = 1; row < rows - 1; row++) {
            this.operateBorder(geoGrid, newGrid, cols - 1, row, cellSize);
        }
        // interior of grid
        float[][] srcGrid = geoGrid.getGrid();
        float[][] dstGrid = newGrid.getGrid();
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < cols - 1; col++) {
                
                final float z1 = srcGrid[row-1][col-1]; // top left
                final float z2 = srcGrid[row-1][col]; // top
                final float z3 = srcGrid[row-1][col+1]; // top right
                final float z4 = srcGrid[row][col-1]; // left
                final float z5 = srcGrid[row][col]; // center
                final float z6 = srcGrid[row][col+1]; // right
                final float z7 = srcGrid[row+1][col-1]; // bottom left
                final float z8 = srcGrid[row+1][col]; // bottom
                final float z9 = srcGrid[row+1][col+1]; // bottom right
                
                final float a = (z1+z3+z4+z6+z7+z9)/6*gg - (z2+z5+z8)/3*gg;
                final float b = (z1+z2+z3+z7+z8+z9)/6*gg - (z4+z5+z6)/3*gg;
                final float c = (z3+z7-z1-z9)/4*gg;
                
                final float profmax = (float)(-a - b + Math.sqrt((a-b)*(a-b)+c*c));
                dstGrid[row][col] = profmax;
                
            }
        }
        
        return newGrid;
    }

    private void operateBorder(GeoGrid src, GeoGrid dst, int col, int row, double cellSize) {
        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();
        final int cols = src.getCols();
        final int rows = src.getRows();
        
        final int rm = row - 1 < 0 ? 0 : row - 1;
        final int rp = row + 1 >= rows ? rows - 1 : row + 1; 
        final int cm = col - 1 < 0 ? 0 : col - 1;
        final int cp = col + 1 >= cols ? cols - 1 : col + 1;

        final float z1 = srcGrid[rm][cm]; // top left
        final float z2 = srcGrid[rm][col]; // top
        final float z3 = srcGrid[rm][cp]; // top right
        final float z4 = srcGrid[row][cm]; // left
        final float z5 = srcGrid[row][col]; // center
        final float z6 = srcGrid[row][cp]; // right
        final float z7 = srcGrid[rp][cm]; // bottom left
        final float z8 = srcGrid[rp][col]; // bottom
        final float z9 = srcGrid[rp][cp]; // bottom right

        final float gg = (float)(cellSize*cellSize);
        final float a = (z1 + z3 + z4 + z6 + z7 + z9) / 6 * gg - (z2 + z5 + z8) / 3 * gg;
        final float b = (z1 + z2 + z3 + z7 + z8 + z9) / 6 * gg - (z4 + z5 + z6) / 3 * gg;
        final float c = (z3 + z7 - z1 - z9) / 4 * gg;

        final float profmin = (float) (-a - b + Math.sqrt((a-b) * (a-b) + c*c));
        dstGrid[row][col] = profmin;
    }
    
}
