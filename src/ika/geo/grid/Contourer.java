/*
 * Contourer.java
 *
 * Created on August 15, 2005, 12:06 PM
 *
 */

package ika.geo.grid;

import ika.geo.*;

/**
 *
 * @author jenny
 */
public class Contourer implements GridOperator {
    
    boolean[][] flags;
    private double interval;
    private VectorSymbol vectorSymbol;
    
    /**
     * Flag for special treatement for degrees containing degree values 0..360
     * Which cause problems for cells that have values over high-noon.
     */
    private boolean treatDegreeJump = false;
    
    /** Creates a new instance of Contourer */
    public Contourer() {
        this.vectorSymbol = new VectorSymbol();
        this.vectorSymbol.setFilled(false);
        this.vectorSymbol.setScaleInvariant(true);
        this.vectorSymbol.setStrokeWidth(1);
    }
    
    public String getName() {
        return "Contourer";
    }

    public GeoObject operate(GeoGrid geoGrid) {
        float[] minMax = geoGrid.getMinMax();
        final double firstContourLevel = (float)Math.ceil(minMax[0]/interval)*interval;
        return this.operate(geoGrid, firstContourLevel);
    }
    
    public GeoObject operate(GeoGrid geoGrid, double firstContourLevel) {
        float[] minMax = geoGrid.getMinMax();
        return this.operate(geoGrid, firstContourLevel, minMax[1]);
    }
    
    public GeoObject operate(GeoGrid geoGrid, double firstContourLevel, double lastContourLevel) {
        this.flags = new boolean[geoGrid.getRows()][geoGrid.getCols()];
        
        GeoSet geoSet = new GeoSet();
        
        final int nlevels = (int)((lastContourLevel - firstContourLevel) / this.interval) + 1;
        if (this.treatDegreeJump) {
            GeoSet levelGeoSet = new GeoSet();
            this.contourLevel(geoGrid, 0.f, levelGeoSet);
            levelGeoSet.setName(Float.toString(0.f));
            geoSet.add(levelGeoSet);
        }
        
        for (int i = 0; i < nlevels; ++i) {
            final double contourLevel = firstContourLevel + i * this.interval;
            GeoSet levelGeoSet = new GeoSet();
            this.contourLevel(geoGrid, (float)contourLevel, levelGeoSet);
            levelGeoSet.setName(Double.toString(contourLevel));
            geoSet.add(levelGeoSet);
        }
        return geoSet;
    }
    
    private void contourLevel (GeoGrid geoGrid, float level, GeoSet levelGeoSet) {
        
        final int nbrCellsX = geoGrid.getCols() - 1;
        final int nbrCellsY = geoGrid.getRows() - 1;
        float[][] grid = geoGrid.getGrid();
        double west = geoGrid.getWest();
        double north = geoGrid.getNorth();
        float cellSize = (float)geoGrid.getCellSize();
        
        for (int i = 0; i < this.flags.length; i++)
            java.util.Arrays.fill(this.flags[i], false);
        
        for (int y = 0; y < nbrCellsY; y++) {
            boolean[] flag_row = flags[y];
            for (int x = 0; x < nbrCellsX; x++) {
                if (flag_row[x] == false) {
                    this.traceContour(grid, new int[]{x, y}, level, west, north, cellSize, levelGeoSet);
                }
            }
        }
    }
    
    private void traceContour(float[][] grid, int[]cell, float level,
            double west, double north, float cellSize, GeoSet levelGeoSet) {
        
        GeoPath geoPath = null;
        int[]current_cell = new int[]{cell[0], cell[1]};
        
        float[] pt = new float[2];
        pt[0] = (float)(west + cell[0] * cellSize);
        pt[1] = (float)(north + cell[1] * cellSize);
            
        int nbrPts = 0;
        final int cols = grid[0].length;
        final int rows = grid.length;
        
        // first trace contour in backward direction
        while (current_cell[0] >= 0 &&
                current_cell[0] < cols - 1 &&
                current_cell[1] >= 0 &&
                current_cell[1] < rows - 1 &&
                this.contourCell(false, grid, current_cell, level, west, north, cellSize, pt)) {
            
            if (nbrPts > 0)
                geoPath.lineTo(pt[0], pt[1]);
            else {
                geoPath = new GeoPath();
                geoPath.moveTo(pt[0], pt[1]);
            }
            nbrPts++;
        }
        
        if (nbrPts > 1)
            geoPath.invertDirection();
        
        // reset the flag for the starting cell
        flags[cell[1]][cell[0]] = false;
        current_cell[0] = cell[0];
        current_cell[1] = cell[1];
        
        // then trace contour in forward direction
        while (current_cell[0] >= 0 &&
                current_cell[0] < cols - 1 &&
                current_cell[1] >= 0 &&
                current_cell[1] < rows - 1 &&
                this.contourCell(true, grid, current_cell, level, west, north, cellSize, pt)) {
            
            if (nbrPts > 0)
                geoPath.lineTo(pt[0], pt[1]);
            else {
                geoPath = new GeoPath();
                geoPath.moveTo(pt[0], pt[1]);
            }
            nbrPts++;
        }
        
        if (nbrPts > 1) {
            geoPath.setVectorSymbol(this.vectorSymbol);
            levelGeoSet.add(geoPath);
        }
    }
    
    private boolean contourCell(boolean forward, float[][] grid, int[]cellXY, float level,
            double west, double north, float cellSize, float[] pt) {
        
        final int col = cellXY[0];
        final int row = cellXY[1];
        
        // test if this cell has been visited before
        if (this.flags[row][col] == true)   
            return false;
        
        // mark this cell as being visited
        this.flags[row][col] = true;
        
        // extract the four values of the cell
        float v0 = grid[row+1][col];  // lower left
        if (Float.isNaN(v0))
            return false;
        float v1 = grid[row+1][col+1];// lower right
        if (Float.isNaN(v1))
            return false;
        float v2 = grid[row][col];    // upper left
        if (Float.isNaN(v2))
            return false;
        float v3 = grid[row][col+1];  // upper right
        if (Float.isNaN(v3))
            return false;
        
        if (this.treatDegreeJump) {
            
            float v0d = v0 - 180;
            float v1d = v1 - 180;
            float v2d = v2 - 180;
            float v3d = v3 - 180;
            
            final boolean adjustCell =
                    (v0d > 0 && v1d < 0 && v0d - v1d > 90)
                    || (v0d < 0 && v1d > 0 && v1d - v0d > 90)
                    || (v0d > 0 && v2d < 0 && v0d - v2d > 90)
                    || (v0d < 0 && v2d > 0 && v2d - v0d > 90)
                    || (v0d > 0 && v3d < 0 && v0d - v3d > 90)
                    || (v0d < 0 && v3d > 0 && v3d - v0d > 90)
                    
                    || (v1d > 0 && v2d < 0 && v1d - v2d > 90)
                    || (v1d < 0 && v2d > 0 && v2d - v1d > 90)
                    || (v1d > 0 && v3d < 0 && v1d - v3d > 90)
                    || (v1d < 0 && v3d > 0 && v3d - v1d > 90)
                    
                    || (v2d > 0 && v3d < 0 && v2d - v3d > 90)
                    || (v2d < 0 && v3d > 0 && v3d - v2d > 90);
            
            if (adjustCell) {
                
                if (v0 > 180)
                    v0 -= 360;
                if (v1 > 180)
                    v1 -= 360;
                if (v2 > 180)
                    v2 -= 360;
                if (v3 > 180)
                    v3 -= 360;
            }
        }
        
        if (!forward) {
            v0 = -v0;
            v1 = -v1;
            v2 = -v2;
            v3 = -v3;
            level = -level;
        }
        
        int code = 0;
        if (v0 > level)
            code ^= 1;
        if (v1 > level)
            code ^= 2;
        if (v2 > level)
            code ^= 4;
        if (v3 > level)
            code ^= 8;
        if (code == 0 || code == 15) {
            return false;
        }
        
        switch (code) {
            case 1: // enter bottom edge, exit left edge
                pt[0] = (float)(west + col * cellSize);
                pt[1] = (float)(north - row * cellSize) - interpol(level, v2, v0) * cellSize;;
                cellXY[0]--;
                break;
                
            case 2: // enter right edge, exit bottom edge
                pt[0] = (float)(west + col * cellSize) + interpol(level, v0, v1) * cellSize;
                pt[1] = (float)(north - row * cellSize) - cellSize;
                cellXY[1]++;
                break;
                
            case 3: // enter right edge, exit left edge
                pt[0] = (float)(west + col * cellSize);
                pt[1] = (float)(north - row * cellSize) - interpol(level, v2, v0) * cellSize;
                cellXY[0]--;
                break;
                
            case 4: // enter left edge, exit top edge
                pt[0] = (float)(west + col * cellSize) + interpol(level, v2, v3) * cellSize;
                pt[1] = (float)(north - row * cellSize);
                cellXY[1]--;
                break;
                
            case 5: // enter bottom edge, exit top edge
                pt[0] = (float)(west + col * cellSize) + interpol(level, v2, v3) * cellSize;
                pt[1] = (float)(north - row * cellSize);
                cellXY[1]--;
                break;
                
            case 6: // saddle point
                final double topDif = north - pt[1] - row * cellSize;
                
                // distinguish between lines entering from the left and from the right
                if (Math.abs(west + col * cellSize - pt[0]) < 0.5 * cellSize) { // line is entering from left
                    // compute the intersection point on right edge of cell
                    final double rightY = interpol(level, v3, v1) * cellSize;
                    if (rightY > topDif) {
                        // line is entering from left and leaving on top edge: case 4
                        pt[0] = (float)(west + col * cellSize) + interpol(level, v2, v3) * cellSize;
                        pt[1] = (float)(north - row * cellSize);
                        cellXY[1]--;
                    } else {
                        // line is entering from left and leaving on bottom edge: case 14
                        pt[0] = (float)(west + col * cellSize) + interpol(level, v0, v1) * cellSize;
                        pt[1] = (float)(north - row * cellSize) - cellSize;
                        cellXY[1]++;
                    }
                } else {    // line is entering from right
                    // compute the intersection point on left edge of cell
                    final double leftY = interpol(level, v2, v0) * cellSize;
                    if (leftY > topDif) {
                        // line is entering from right and leaving on top edge: case 7
                        pt[0] = (float)(west + col * cellSize) + interpol(level, v2, v3) * cellSize;
                        pt[1] = (float)(north - row * cellSize);
                        cellXY[1]--;
                    } else {
                        // line is entering from right and leaving on bottom edge: case 2
                        pt[0] = (float)(west + col * cellSize) + interpol(level, v0, v1) * cellSize;
                        pt[1] = (float)(north - row * cellSize) - cellSize;
                        cellXY[1]++;
                    }
                }
                break;
                
            case 7: // enter right edge, exit top edge
                pt[0] = (float)(west + col * cellSize) + interpol(level, v2, v3) * cellSize;
                pt[1] = (float)(north - row * cellSize);
                cellXY[1]--;
                break;
                
            case 8: // enter top edge, exit right edge
                pt[0] = (float)(west + cellXY[0] * cellSize) + cellSize;
                pt[1] = (float)(north - cellXY[1] * cellSize) - interpol(level, v3, v1) * cellSize;
                cellXY[0]++;
                break;
                
            case 9: // saddle point
                final double rightDif = pt[0] - west - col * cellSize;
                
                // distinguish between lines entering from the bottom and from the top
                if (Math.abs(north - row * cellSize - pt[1]) < 0.5 * cellSize) { // line is entering from top
                    // compute the intersection point on bottom edge of cell
                    double bottomX = interpol(level, v0, v1) * cellSize;
                    if (bottomX > rightDif) {
                        // line is entering from top and leaving on left edge: case 11
                        pt[0] = (float)(west + col * cellSize);
                        pt[1] = (float)(north - row * cellSize) - interpol(level, v2, v0) * cellSize;
                        cellXY[0]--;
                    } else {
                        // line is entering from top and leaving on right edge: case 8
                        pt[0] = (float)(west + col * cellSize) + cellSize;
                        pt[1] = (float)(north - row * cellSize) - interpol(level, v3, v1) * cellSize;
                        cellXY[0]++;
                    }
                } else  {   // line is entering from bottom
                    // compute the intersection point on top edge of cell
                    double topX = interpol(level, v2, v3) * cellSize;
                    if (topX > rightDif) {
                        // line is entering from bottom and leaving on left edge: case 1
                        pt[0] = (float)(west + col * cellSize);
                        pt[1] = (float)(north - row * cellSize) - interpol(level, v2, v0) * cellSize;;
                        cellXY[0]--;
                    } else {
                        // line is entering from bottom and leaving on right edge: case 13
                        pt[0] = (float)(west + col * cellSize) + cellSize;
                        pt[1] = (float)(north - row * cellSize) - interpol(level, v3, v1) * cellSize;
                        cellXY[0]++;
                    }
                }
                break;
                
            case 10:    // enter top edge, exit bottom edge
                pt[0] = (float)(west + col * cellSize) + interpol(level, v0, v1) * cellSize;
                pt[1] = (float)(north - row * cellSize) - cellSize;
                cellXY[1]++;
                break;
                
            case 11:    // enter top edge, exit left edge
                pt[0] = (float)(west + col * cellSize);
                pt[1] = (float)(north - row * cellSize) - interpol(level, v2, v0) * cellSize;
                cellXY[0]--;
                break;
                
            case 12:    // enter left edge, exit right edge
                pt[0] = (float)(west + col * cellSize) + cellSize;
                pt[1] = (float)(north - row * cellSize) - interpol(level, v3, v1) * cellSize;
                cellXY[0]++;
                break;
                
            case 13:    // enter bottom edge, exit right edge
                pt[0] = (float)(west + col * cellSize) + cellSize;
                pt[1] = (float)(north - row * cellSize) - interpol(level, v3, v1) * cellSize;
                cellXY[0]++;
                break;
                
            case 14:    // enter left edge, exit bottom edge
                pt[0] = (float)(west + col * cellSize) + interpol(level, v0, v1) * cellSize;
                pt[1] = (float)(north - row * cellSize) - cellSize;
                cellXY[1]++;
                break;
        }
        return true;
    }
    
    static final private float interpol(float level, float v0, float v1) {
        return (level - v0) / (v1 - v0);
    }
    
    public double getInterval() {
        return interval;
    }
    
    public void setInterval(double interval) {
        if (interval <= 0.f)
            throw new IllegalArgumentException("Interval must be bigger than zero");
        this.interval = interval;
    }
    
    public VectorSymbol getVectorSymbol() {
        return vectorSymbol;
    }
    
    public void setVectorSymbol(VectorSymbol vectorSymbol) {
        this.vectorSymbol = vectorSymbol;
    }
    
    public boolean isTreatDegreeJump() {
        return treatDegreeJump;
    }
    
    public void setTreatDegreeJump(boolean treatDegreeJump) {
        this.treatDegreeJump = treatDegreeJump;
    }
    
    /*
    public static void main(String[] params) {
        GeoGrid geoGrid = new GeoGrid(3, 3, 1);
        geoGrid.setWest(0);
        geoGrid.setNorth(2);
        geoGrid.setMeshSize(1);
        float[][] grid = geoGrid.getGrid();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                grid[i][j] = 0.999999f;
            }
        }
        grid[1][1] = 0.000001f;
     
     
        grid[0][0] = 0;
        grid[0][1] = 1;
        grid[0][2] = 2;
        grid[1][0] = 1;
        grid[1][1] = 2;
        grid[1][2] = 3;
        grid[2][0] = 2;
        grid[2][1] = 3;
        grid[2][2] = 4;
     
        Contourer contourer = new Contourer();
        contourer.setGeoGrid(geoGrid);
        contourer.setInterval(0.5f);
        contourer.contour();
     
        ika.geoexport.GeoSetExporter exporter = new ika.geoexport.SVGExporter();
        try {
            exporter.export(contourer.getGeoSet(), "/Users/jenny/Desktop/contours.svg");
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
     */
}