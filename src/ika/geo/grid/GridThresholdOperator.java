/*
 * GridThresholdOperator.java
 *
 * Created on February 6, 2006, 9:45 AM
 *
 */

package ika.geo.grid;

import ika.geo.GeoGrid;

/**
 * Changes all values greater or smaller than a limit value to another value.
 * @author jenny
 */
public class GridThresholdOperator implements GridOperator{
    
    private float thresholdValue = 0.f;
    private float replaceValue = 0.f;
    private boolean smallerThan = true;
    
    /** Creates a new instance of GridThresholdOperator */
    public GridThresholdOperator() {
    }
    
    public String getName() {
        return "Threshold";
    }
    
    public ika.geo.GeoGrid operate(ika.geo.GeoGrid geoGrid) {
        if (geoGrid == null)
            throw new IllegalArgumentException();
        
        final int ncols = geoGrid.getCols();
        final int nrows = geoGrid.getRows();
        GeoGrid newGrid = new GeoGrid(ncols, nrows, geoGrid.getCellSize());
        newGrid.setWest(geoGrid.getWest());
        newGrid.setNorth(geoGrid.getNorth());
        
        float[][] srcGrid = geoGrid.getGrid();
        float[][] dstGrid = newGrid.getGrid();
        
        if (smallerThan) {
            for (int row = 0; row < nrows; row++) {                
                float[] srcRow = srcGrid[row];
                float[] dstRow = dstGrid[row];
                for (int col = 0; col < ncols; ++col) {
                    dstRow[col] = srcRow[col] < this.thresholdValue ? this.replaceValue : srcRow[col];
                }
            }
        } else {
            
            for (int row = 0; row < nrows; row++) {
                float[] srcRow = srcGrid[row];
                float[] dstRow = dstGrid[row];
                for (int col = 0; col < ncols; ++col) {
                    dstRow[col] = srcRow[col] > this.thresholdValue ? this.replaceValue : srcRow[col];
                }
            }
        }
        
        return newGrid;
    }
    
    public float getThresholdValue() {
        return thresholdValue;
    }
    
    public void setThresholdValue(float minValue) {
        this.thresholdValue = minValue;
    }
    
    public float getReplaceValue() {
        return replaceValue;
    }
    
    public void setReplaceValue(float replaceValue) {
        this.replaceValue = replaceValue;
    }
    
    public void clipSmallValues(float val){
        this.thresholdValue = val;
        this.replaceValue = val;
        this.smallerThan = true;
    }
    
    public void clipLargeValues(float val){
        this.thresholdValue = val;
        this.replaceValue = val;
        this.smallerThan = false;
    }
}
