package ika.app;

import ika.geo.GeoGrid;
import ika.geo.GeoGridShort;
import ika.geo.GeoImage;
import ika.geo.GeoSet;

public class ScreeData {

    public static final String REF_IMAGE_NAME = "refmap";
    public static final String OBSTACLES_IMAGE_NAME = "obstacles";
    public static final String LARGE_STONE_IMAGE_NAME = "largeStonesMask";
    public static final String SHADING_IMAGE_NAME = "shading";
    public static final String GRADATION_MASK_IMAGE_NAME = "gradationMask";
    public static final String LINES_NAME = "lines";
    public static final String POLYGONS_NAME = "polygons";

    /**
     * polygons to fill with scree. This GeoSet is added to the map and receives
     * the generated scree dots.
     */
    public final GeoSet screePolygons;
    /**
     * A digital elevation model
     */
    public GeoGrid dem;
    /**
     * The shaded relief steering the density of stones as grayscale image.
     */
    public GeoImage shadingImage;
    /**
     * Use second gradation curve for shading where values are not 0 in this
     * dem.
     */
    public GeoImage shadingGradationMaskImage;
    /**
     * An image containing obstacles (non-white area is an obstacle).
     */
    public GeoImage obstaclesMaskImage;
    /**
     * mask image with areas where larger stones are placed. stones are enlarged
     * on non-white areas
     */
    public GeoImage largeStoneMaskImage;
    /**
     * An image that is displayed as a reference. Not used for the computation
     * of scree.
     */
    public GeoImage referenceImage;
    /**
     * density of gully lines
     */
    public GeoGridShort lineDensityGrid;
    /**
     * curvature derived from the DEM
     */
    public GeoGrid curvatureGrid;
    /**
     * generated scree dots
     */
    public final GeoSet screeStones;
    /**
     * gully lines either extracted from the DEM or imported from a file.
     */
    public final GeoSet gullyLines;
    /**
     * flag that is true when gully lines have been imported from a file and
     * must not be extracted from the DEM.
     */
    public boolean fixedScreeLines = false;

    public ScreeData() {
        screePolygons = new GeoSet();
        screePolygons.setVisible(false);
        screeStones = new GeoSet();
        gullyLines = new GeoSet();
        gullyLines.setVisible(false);
    }

    public boolean hasDEM() {
        return dem != null;
    }

    public boolean hasShading() {
        return shadingImage != null;
    }

    public boolean hasShadingGradationMask() {
        return shadingGradationMaskImage != null;
    }

    public boolean hasObstaclesMask() {
        return obstaclesMaskImage != null;
    }

    public boolean hasLargeStoneMask() {
        return largeStoneMaskImage != null;
    }

    public boolean hasLineDensityGrid() {
        return lineDensityGrid != null;
    }

    public boolean hasCurvatureGrid() {
        return curvatureGrid != null;
    }

    public boolean hasReferenceImage() {
        return referenceImage != null;
    }

    public boolean hasGullyLines() {
        return gullyLines.getNumberOfChildren() > 0;
    }

    public boolean hasScreePolygons() {
        return screePolygons.getNumberOfChildren() > 0;
    }

    public boolean hasScreeStones() {
        return screeStones.getNumberOfChildren() > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.dem.toStringWithStatistics());
        sb.append(this.shadingImage.toString());
        sb.append(this.obstaclesMaskImage.toString());
        sb.append(this.largeStoneMaskImage.toString());
        sb.append(this.curvatureGrid.toStringWithStatistics());
        sb.append(this.screePolygons.toString());
        return sb.toString();
    }
}
