package ika.app;

import ika.gui.PageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.data.projection.proj.Proj;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;
import org.openstreetmap.josm.data.projection.proj.SwissObliqueMercator;

/**
 * Utility class to compute longitude and latitude for corner points of a
 * PageFormat. This is for the Swiss CH1903+LV95 coordinate reference system.
 *
 * @author bernie
 */
public class SwissLV95GeospatialPDFExport {

    private static final double LV95_X0 = 2600000; // false easting (in meters)
    private static final double LV95_Y0 = 1200000; // false northing (in meters)
    private static final double LV95_LON0 = 7.439583333333333;   // central meridian
    private static  final double LV95_ELLIPS_A = 6377397.155; // Bessel ellipspoid major axis
    
    /*public static final String wkt = "PROJCS[\"CH1903 / LV03\","
     + "GEOGCS[\"CH1903\","
     + "DATUM[\"D_CH1903\","
     + "SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],"
     + "PRIMEM[\"Greenwich\",0],"
     + "UNIT[\"Degree\",0.017453292519943295]],"
     + "PROJECTION[\"Hotine_Oblique_Mercator_Azimuth_Center\"],"
     + "PARAMETER[\"latitude_of_center\",46.95240555555556],"
     + "PARAMETER[\"longitude_of_center\",7.439583333333333],"
     + "PARAMETER[\"azimuth\",90],"
     + "PARAMETER[\"scale_factor\",1],"
     + "PARAMETER[\"false_easting\",600000],"
     + "PARAMETER[\"false_northing\",200000],"
     + "UNIT[\"Meter\",1]]";*/
    /*public static final String wkt =
            "PROJCS[\"CH1903+ / LV95\","
            + "GEOGCS[\"CH1903+\","
            + "DATUM[\"CH1903\","
            + "SPHEROID[\"Bessel 1841\",6377397.155,299.1528128,AUTHORITY[\"EPSG\",\"7004\"]],TOWGS84[674.374,15.056,405.346,0,0,0,0],AUTHORITY[\"EPSG\",\"6150\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4150\"]],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],"
            + "PROJECTION[\"Hotine_Oblique_Mercator\"],PARAMETER[\"latitude_of_center\",46.95240555555556],PARAMETER[\"longitude_of_center\",7.439583333333333],PARAMETER[\"azimuth\",90],PARAMETER[\"rectified_grid_angle\",90],PARAMETER[\"scale_factor\",1],PARAMETER[\"false_easting\",2600000],PARAMETER[\"false_northing\",1200000],AUTHORITY[\"EPSG\",\"2056\"],"
            + "AXIS[\"Y\",EAST],AXIS[\"X\",NORTH]]";
    */
    public static final String wkt = 
            "PROJCS[\"CH1903+_LV95\","
            + "GEOGCS[\"GCS_CH1903+\","
            + "DATUM[\"D_CH1903+\","
            + "SPHEROID[\"Bessel_1841\",6377397.155,299.1528128156]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],"
            + "PROJECTION[\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"false_easting\",2600000],PARAMETER[\"false_northing\",1200000],PARAMETER[\"latitude_of_center\",46.95240555555556],PARAMETER[\"longitude_of_center\",7.439583333333333],PARAMETER[\"azimuth\",90],PARAMETER[\"scale_factor\",1],"
            + "UNIT[\"Meter\",1]]";
    /**
     *
     * @param x
     * @param y
     * @param proj
     * @return longitude and latitude (in this order)
     */
    private static double[] eastNorth2latlon(double x, double y, Proj proj) {
        double[] latlon_rad = proj.invproject((x - LV95_X0) / LV95_ELLIPS_A, (y - LV95_Y0) / LV95_ELLIPS_A);
        double lat = Math.toDegrees(latlon_rad[0]);
        double lon = Math.toDegrees(latlon_rad[1]) + LV95_LON0;
        return new double[]{lat, lon};
    }

    private static double[] latlon2EastNorth(double lat, double lon, Proj proj) {
        lon -= LV95_LON0;
        lat = Math.toRadians(lat);
        lon = Math.toRadians(lon);

        double[] xy = proj.project(lat, lon);
        xy[0] *= LV95_ELLIPS_A;
        xy[1] *= LV95_ELLIPS_A;
        xy[0] += LV95_X0;
        xy[1] += LV95_Y0;

        return xy;
    }

    public static float[] lonLatCornerPoints(PageFormat pageFormat) {
        try {
            float[] geographicBB = new float[8];

            SwissObliqueMercator proj = new SwissObliqueMercator();
            ProjParameters projParameters = new ProjParameters();
            projParameters.ellps = Ellipsoid.Bessel1841;
            projParameters.lat_0 = 46.95240555555556;

            proj.initialize(projParameters);
            double x, y;
            double[] latlonDeg;

            // bottom left
            x = pageFormat.getPageLeft();
            y = pageFormat.getPageBottom();
            latlonDeg = eastNorth2latlon(x, y, proj);
            geographicBB[0] = (float) latlonDeg[0];
            geographicBB[1] = (float) latlonDeg[1];

            // top left
            x = pageFormat.getPageLeft();
            y = pageFormat.getPageTop();
            latlonDeg = eastNorth2latlon(x, y, proj);
            geographicBB[2] = (float) latlonDeg[0];
            geographicBB[3] = (float) latlonDeg[1];

            // top right
            x = pageFormat.getPageRight();
            y = pageFormat.getPageTop();
            latlonDeg = eastNorth2latlon(x, y, proj);
            geographicBB[4] = (float) latlonDeg[0];
            geographicBB[5] = (float) latlonDeg[1];

            // bottom right
            x = pageFormat.getPageRight();
            y = pageFormat.getPageBottom();
            latlonDeg = eastNorth2latlon(x, y, proj);
            geographicBB[6] = (float) latlonDeg[0];
            geographicBB[7] = (float) latlonDeg[1];

            return geographicBB;
        } catch (ProjectionConfigurationException ex) {
            Logger.getLogger(SwissLV95GeospatialPDFExport.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    /*
     public static void main(String[] args) {
     try {
     SwissObliqueMercator proj = new SwissObliqueMercator();
     ProjParameters projParameters = new ProjParameters();
     projParameters.ellps = Ellipsoid.Bessel1841;
     projParameters.lat_0 = 46.95240555555556;

     proj.initialize(projParameters);
     double[] latlonDeg;

     // bottom left
     double x = 602030.680;
     double y = 191775.030;
     latlonDeg = eastNorth2latlon(x, y, proj);
     double zimmerwaldLon = 7. + 27. / 60 + 58.416328 / 60 / 60;
     double zimmerwaldLat = 46. + 52. / 60 + 42.269284 / 60 / 60;
            
     System.out.println("Zimmerwald: " + zimmerwaldLat);
     System.out.println("Zimmerwald: " + zimmerwaldLon);
     System.out.println(zimmerwaldLat - latlonDeg[0]);
     System.out.println(zimmerwaldLon - latlonDeg[1]);
     System.out.println(latlonDeg[0]);
     System.out.println(latlonDeg[1]);
            
     double[] xy = latlon2EastNorth(latlonDeg[0], latlonDeg[1], proj);
     System.out.println(xy[0]);
     System.out.println(xy[1]);
            
     } catch (ProjectionConfigurationException ex) {
     Logger.getLogger(GeospatialPDFExporter.class.getName()).log(Level.SEVERE, null, ex);
     }
     }
     */
}
