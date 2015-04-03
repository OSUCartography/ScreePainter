/*
 * GridScaleToRangeOperator.java
 *
 * Created on February 2, 2006, 8:33 PM
 *
 */

package ika.geo.grid;

import ika.geo.*;

/**
 *
 * @author jenny
 */
public class GridScaleToRangeOperator implements GridOperator{
    
    private float newMin = 0.f;
    private float newMax = 0.f;
    
    /** Creates a new instance of GridScaleToRangeOperator */
    public GridScaleToRangeOperator() {
    }

    public GridScaleToRangeOperator(float newMin, float newMax) {
        this.setRange(newMin, newMax);
    }
    
    @Override
    public String getName() {
        return "Scale To Range";
    }

    @Override
    public ika.geo.GeoGrid operate(ika.geo.GeoGrid geoGrid) {
        if (geoGrid == null)
            throw new IllegalArgumentException();
        
        final int nrows = geoGrid.getRows();
        final int ncols = geoGrid.getCols();
        final float[] minMax = geoGrid.getMinMax();
        final float min = minMax[0];
        final float max = minMax[1];
        GeoGrid newGrid = new GeoGrid(ncols, nrows, geoGrid.getCellSize(), newMin);
        newGrid.setWest(geoGrid.getWest());
        newGrid.setNorth(geoGrid.getNorth());

        final float oldRange = max - min;
        if (oldRange <= 0) {
            return newGrid;
        }
        final float newRange = newMax - newMin;
        
        
        float[][] srcGrid = geoGrid.getGrid();
        float[][] dstGrid = newGrid.getGrid();
        
        if (oldRange == 0)
            return newGrid;
        for (int row = 0; row < nrows; ++row) {
            float[] srcRow = srcGrid[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < ncols; ++col) {
                dstRow[col] = (srcRow[col] - min) / oldRange * newRange + newMin;
            }
        }
        return newGrid;
    }

    public float getNewMin() {
        return newMin;
    }

    public void setNewMin(float newMin) {
        this.newMin = newMin;
    }

    public float getNewMax() {
        return newMax;
    }

    public void setNewMax(float newMax) {
        this.newMax = newMax;
    }
    
    public void setRange(float newMin, float newMax) {
        this.newMin = newMin;
        this.newMax = newMax;
    }
}
