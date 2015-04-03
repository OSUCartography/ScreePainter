/*
 * PointTool.java
 *
 * Created on April 8, 2005, 7:59 AM
 */

package ika.map.tools;

import ika.geo.grid.*;
import java.awt.Color;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.ArrayList;
import ika.geo.*;
import ika.gui.MapComponent;

/**
 * PointSetterTool - a tool that adds GeoPoints to a map by simple clicks.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class FalllineTool extends MapTool {
    
    private GeoSet linesGeoSet;
    
    private GeoGrid grid;
        
    private float minSlopeDegree = 0.1f;

    private double stoneDistance = 1./1000 * 25000;
    
    private float stoneDiameter = 0.5f / 1000 * 25000;
    
    private double slopeDisplacement;

    private double curveDisplacement;
    
    public void updateLines() {

        getLinesGeoSet().removeAllGeoObjects();
        
        GridFalllineOperator operator = new GridFalllineOperator();
        operator.setSearchMethod(GridFalllineOperator.SearchMethod.DOWN);
        operator.setMinSlopeDegree(minSlopeDegree);
        
        int pointsCount = this.destinationGeoSet.getNumberOfChildren();
        for (int i = 0; i < pointsCount; i++) {
            GeoPoint point = (GeoPoint)destinationGeoSet.getGeoObject(i);
            operator.setStartX(point.getX());
            operator.setStartY(point.getY());
            GeoPath path = (GeoPath)operator.operate(this.grid);
            if (path != null && path.getPointsCount() > 1) {
                this.linesGeoSet.add(path);
                
                ArrayList<Point2D> pts = path.toBeads(stoneDistance, this.slopeDisplacement, this.curveDisplacement);
                for (Point2D pt : pts) {
                    GeoPath stone = GeoPath.newSquare((float)pt.getX(), (float)pt.getY(), stoneDiameter);
                    stone.setVectorSymbol(new VectorSymbol(Color.BLACK, null, 0));
                    this.getLinesGeoSet().add(stone);
                }
            }
        }
    }
    
    private PointSymbol pointSymbol;
    
    /**
     * Create a new instance.
     * @param mapComponent The MapComponent for which this MapTool provides its services.
     */
    public FalllineTool(MapComponent mapComponent) {
        super(mapComponent);
    }
    
    /**
     * Create a new instance.
     * @param mapComponent The MapComponent for which this MapTool provides its services.
     * @param pointSymbol The PointSymbol used to draw newly created points.
     */
    public FalllineTool(MapComponent mapComponent,
            PointSymbol pointSymbol) {
        super(mapComponent);
        this.pointSymbol = pointSymbol;
    }
    
    /**
     * The mouse was clicked, while this MapTool was the active one.
     * @param point The location of the mouse in world coordinates.
     * @param evt The original event.
     */
    public void mouseClicked(Point2D.Double point, MouseEvent evt) {                       
        
        
        System.out.println("\nPoint Clicked: " + point.x + " " + point.y);
        System.out.println("Slope: " + Math.toDegrees(this.grid.getBilinearInterpol(point.x, point.y)));
        System.out.println("Aspect: " + Math.toDegrees(this.grid.getBilinearInterpol(point.x, point.y)));
        
        // add a new point
        GeoPoint geoPoint = new GeoControlPoint(point);
        if (this.pointSymbol != null)
            geoPoint.setPointSymbol(this.pointSymbol);
        geoPoint.setSelected(true);
        
        // deselect all current GeoObjects in destination GeoSet
        if (this.destinationGeoSet != null) {
            this.destinationGeoSet.setSelected(false);
            // add point
            this.destinationGeoSet.add(geoPoint);
        }
        
        this.updateLines();
    }
    
    public PointSymbol getPointSymbol() {
        return pointSymbol;
    }

    public void setPointSymbol(PointSymbol pointSymbol) {
        this.pointSymbol = pointSymbol;
    }
    
    @Override
    protected String getCursorName() {
        return "setpointarrow";
    }

    public GeoSet getLinesGeoSet() {
        return linesGeoSet;
    }

    public void setLinesGeoSet(GeoSet linesGeoSet) {
        this.linesGeoSet = linesGeoSet;
    }

    public GeoGrid getGrid() {
        return grid;
    }

    public void setGrid(GeoGrid grid) {
        this.grid = grid;
    }

    public float getMinSlopeDegree() {
        return minSlopeDegree;
    }

    public void setMinSlopeDegree(float minSlopeDegree) {
        this.minSlopeDegree = minSlopeDegree;
    }

    public double getSlopeDisplacement() {
        return slopeDisplacement;
    }

    public void setSlopeDisplacement(double slopeDisplacement) {
        this.slopeDisplacement = slopeDisplacement;
    }

    public double getCurveDisplacement() {
        return curveDisplacement;
    }

    public void setCurveDisplacement(double curveDisplacement) {
        this.curveDisplacement = curveDisplacement;
    }

    public double getStoneDistance() {
        return stoneDistance;
    }

    public void setStoneDistance(double stoneDistance) {
        this.stoneDistance = stoneDistance;
    }

    public float getStoneDiameter() {
        return stoneDiameter;
    }

    public void setStoneDiameter(float stoneDiameter) {
        this.stoneDiameter = stoneDiameter;
    }
}
