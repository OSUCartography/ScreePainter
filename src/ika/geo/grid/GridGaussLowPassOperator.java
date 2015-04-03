/*
 * GridGaussLowPassOperator.java
 *
 * Created on February 19, 2006, 12:23 AM
 *
 */

package ika.geo.grid;

import ika.geo.GeoGrid;

/**
 * Gaussian blur or low pass filter
 * http://www.gamedev.net/reference/programming/features/imageproc/page2.asp
 * http://planetmath.org/?op=getobj&from=objects&id=4248
 * http://www-personal.engin.umd.umich.edu/~jwvm/ece581/21_GBlur.pdf
 * 
 * This filter does not behave well on borders! Needs to be fixed.
 * @author jenny
 */
public class GridGaussLowPassOperator {
    
    private int filterSize = 5;
    
    /** Creates a new instance of GridGaussLowPassOperator */
    public GridGaussLowPassOperator() {
    }
    
    public String getName() {
        return "Gauss Low Pass";
    }
    
    public GeoGrid operate(GeoGrid geoGrid) {
        if(geoGrid == null)
            throw new IllegalArgumentException();
        
        // make sure filterSize is an odd number
        if (filterSize % 2 != 1)
            return null;
        final int halfFilterSize = this.filterSize / 2;
        
        // compute the size of the new GeoGrid and create it.
        final int old_nrows = geoGrid.getRows();
        final int old_ncols = geoGrid.getCols();
        final int new_nrows = old_nrows - this.filterSize + 1;
        final int new_ncols = old_ncols - this.filterSize + 1;
        final double meshSize = geoGrid.getCellSize();
        GeoGrid newGrid = new GeoGrid(new_ncols, new_nrows, meshSize);
        newGrid.setWest(geoGrid.getWest() + meshSize * halfFilterSize);
        newGrid.setNorth(geoGrid.getNorth() + meshSize * halfFilterSize);
        
        float[][] srcGrid = geoGrid.getGrid();
        float[][] dstGrid = newGrid.getGrid();
        float[][] tmpGrid = new float[old_nrows][old_ncols];
        
        // compute coefficients for gaussian filter, which are a row from
        // Pascal's triangle. In Pascal's triangle, the entries on the th
        // row are given by the binomial coefficients (n k) for k = 1..n
        float coef[] = new float[this.filterSize];
        for (int i = 0; i <= halfFilterSize; i++){
            coef[i] = coef[filterSize - i - 1]
                    = ika.utils.MathUtils.binomialCoeff(this.filterSize-1, i);
        }
        
        final float gaussSum = (this.filterSize - 1) * (this.filterSize - 1);
        
        // Gaussian blurs are separable into row and column operations.
        // first apply filter in horizontal direction.
        for (int row = halfFilterSize; row < old_nrows-halfFilterSize; row++) {
            float[] dstRow = tmpGrid[row];
            
            for (int col = halfFilterSize; col < old_ncols-halfFilterSize; col++) {
                float gauss = 0;
                float[] srcRow = srcGrid[row - halfFilterSize];
                for (int c = col-halfFilterSize, f = 0; c <= col+halfFilterSize; c++, f++) {
                    gauss += srcRow[c] * coef[f];
                }
                dstRow[col] = gauss/gaussSum;
            }
        }
        
        // then apply filter in vertical direction.
        for (int row = halfFilterSize; row < old_nrows-halfFilterSize; row++) {
            float[] dstRow = dstGrid[row - halfFilterSize];
            
            for (int col = halfFilterSize; col < old_ncols-halfFilterSize; col++) {
                float gauss = 0;
                for (int r = row-halfFilterSize, f = 0; r <= row+halfFilterSize; r++, f++) {
                    gauss += tmpGrid[r][col] * coef[f];
                }
                dstRow[col-halfFilterSize] = gauss/gaussSum;
            }
        }
        
        return newGrid;
    }
    
    public int getFilterSize() {
        return filterSize;
    }
    
    public void setFilterSize(int filterSize) {
        if(filterSize % 2 == 0)
            throw new IllegalArgumentException("Filter size must be odd number");
        this.filterSize = filterSize;
    }
}
