package ika.geo.grid;

import ika.geo.GeoGrid;

/**
 * Multiply a grid by a constant factor.
 * @author jenny
 */
public class GridScaleOperator implements GridOperator{
    
    private float scale;

    public GridScaleOperator() {
        this.scale = 1;
    }
    
    public GridScaleOperator(float scale) {
        this.scale = scale;
    }
    
    public String getName() {
        return "Scale";
    }

    public GeoGrid operate(GeoGrid geoGrid) {
        if (geoGrid == null)
            throw new IllegalArgumentException();
        
        final int nrows = geoGrid.getRows();
        final int ncols = geoGrid.getCols();
        GeoGrid newGrid = new GeoGrid(ncols, nrows, geoGrid.getCellSize());
        newGrid.setWest(geoGrid.getWest());
        newGrid.setNorth(geoGrid.getNorth());
        newGrid.setName(geoGrid.getName());
        
        float[][] srcGrid = geoGrid.getGrid();
        float[][] dstGrid = newGrid.getGrid();
        
        for (int row = 0; row < nrows; ++row) {
            float[] srcRow = srcGrid[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < ncols; ++col) {
                dstRow[col] = srcRow[col] * scale;
            }
        }
        return newGrid;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

}
