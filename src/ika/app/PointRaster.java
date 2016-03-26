package ika.app;

/**
 *
 * @author Bernhard Jenny, School of Mathematical and Geospatial Sciences, RMIT
 * University, Melbourne
 */
public interface PointRaster {

    void addCircle(double x, double y, double rad);

    boolean isCircleOverlaying(double x, double y, double rad);
    
}
