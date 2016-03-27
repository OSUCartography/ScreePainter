package ika.app;

import ika.geo.*;
import ika.geo.grid.GridFalllineOperator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.util.*;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ScreeGenerator {

    public ScreeData screeData = new ScreeData();
    public ScreeParameters p = new ScreeParameters();

    /**
     * Polygons are converted to raster to accelerate frequent point-in-polygon
     * tests. The resolution of the raster to for this test is
     * POINT_IN_POLOGYON_TOLERANCE times higher than the resolution of the grid
     * to dither.
     */
    private final static double POINT_IN_POLOGYON_TOLERANCE = 10d;

    /**
     * A stone with a position, radius and the corners.
     */
    public final class Stone extends GeoObject {

        /**
         * horizontal center
         */
        public double x;
        /**
         * vertical center
         */
        public double y;
        /**
         * mean radius of stone (actual size can be larger)
         */
        public double r;
        /**
         * geometry of stone
         */
        public Path2D.Double path;
        /**
         * cached bounding box for accelerating drawing
         */
        private Rectangle2D bounds;

        public Stone(double x, double y, double r) {
            this.x = x;
            this.y = y;
            setR(r);
            setSelectable(false);
        }

        /**
         * Converts this stone to a GeoPath object
         *
         * @return
         */
        public GeoPath toGeoPath() {
            if (path == null) {
                return null;
            }
            GeoPath geoPath = new GeoPath();
            geoPath.append(path, false);
            geoPath.setVectorSymbol(STONE_SYMBOL);
            return geoPath;
        }

        /**
         * Set the geometry of the stone
         *
         * @param corners x1, y1, x2, y2, etc.
         */
        public void setCorners(double[] corners) {
            // update path for drawing
            path = new Path2D.Double();
            path.moveTo(corners[0], corners[1]);
            final int cornerscount = corners.length / 2;
            for (int i = 1; i < cornerscount; i++) {
                path.lineTo(corners[i * 2], corners[i * 2 + 1]);
            }
        }

        /**
         * set mean radius of stone
         *
         * @param r
         */
        public void setR(double r) {
            this.r = r;
            final double d = 2 * r;
            this.bounds = new Rectangle2D.Double(x - r, y - r, d, d);
        }

        /**
         * returns a bounding box
         *
         * @param scale
         * @return
         */
        @Override
        public Rectangle2D getBounds2D(double scale) {
            return bounds;
        }

        /**
         * draw this object in a map
         *
         * @param rp
         */
        @Override
        public void drawNormalState(RenderParams rp) {
            final Graphics2D g2d = rp.g2d;
            final double scale = rp.scale;
            g2d.setColor(Color.BLACK);

            // if the stone is smaller than 1 pixel, a rectangle is drawn to
            // accelerate drawing. Otherwise the path geometry is drawn.
            if (scale * r > 0.5) {
                g2d.fill(path);
            } else {
                g2d.fill(bounds);
            }
        }

        @Override
        public void drawSelectedState(RenderParams rp) {
        }

        @Override
        public boolean isPointOnSymbol(Point2D point, double tolDist, double scale) {
            return false;
        }

        @Override
        public boolean isIntersectedByRectangle(Rectangle2D rect, double scale) {
            return rect.intersects(bounds);
        }

        @Override
        public void transform(AffineTransform affineTransform) {
        }
    }
    public static final VectorSymbol GULLIES_VECTOR_SYMBOL;

    static {
        GULLIES_VECTOR_SYMBOL = new VectorSymbol(null, Color.DARK_GRAY, 2);
        GULLIES_VECTOR_SYMBOL.setScaleInvariant(true);
    }
    public static final VectorSymbol SCREE_POLYGON_VECTOR_SYMBOL;

    static {
        SCREE_POLYGON_VECTOR_SYMBOL = new VectorSymbol(null, Color.RED, 1);
        SCREE_POLYGON_VECTOR_SYMBOL.setScaleInvariant(true);
        SCREE_POLYGON_VECTOR_SYMBOL.setFilled(false);
        SCREE_POLYGON_VECTOR_SYMBOL.setStroked(true);
    }
    public static final VectorSymbol STONE_SYMBOL = new VectorSymbol(Color.BLACK, null, 0);
    /**
     * A PointRaster object is used to find dots that are too close to each
     * other. The cell size of this raster grid is p.stoneMaxDiameter /
     * REL_POINT_RASTER_RESOLUTION;
     */
    private static final int REL_POINT_RASTER_RESOLUTION = 20;
    /**
     * Search resolution is d = screeGenerator.p.lineMinDistance /
     * REL_GULLIES_SEARCH_RESOLUTION. The shading is resampled to cell size d
     * and then dithered to find start points for searching gully lines. d is
     * also the relative cell size of a grid used to identify gully lines that
     * are getting too close to each other.
     */
    protected static double REL_GULLIES_SEARCH_RESOLUTION = 3;

    // Floyd Steinberg dithering constants
    private static final float A = 7f / 16f;
    private static final float B = 3f / 16f;
    private static final float C = 5f / 16f;
    private static final float D = 1f / 16f;

    public ScreeGenerator() {
    }

    /**
     * Fills a single polygon with scree.
     *
     * @param screeBB a clipping rectangle: only generate stones inside this
     * rectangle. Can be null.
     * @param screePolygon the polygon to fill with scree stones
     * @param shadingGrid grid to control the radius of scree dots.
     * @param minShading the smallest value in shadingGrid
     * @param maxShading the largest value in shadingGrid
     * @param tempShadingGridToDither apply Floyd-Steinberg dithering to this
     * grid. This grid will be changed by this method
     * @param tempLinesDensityGridToDither1
     * @param tempLinesDensityGridToDither2
     * @param generateScreeStones if true polygons are filled with scree stones,
     * otherwise only gully lines are created.
     * @return The number of generated stones or lines if screeStones is null.
     */
    public int generateScree(Rectangle2D screeBB,
            GeoPath screePolygon,
            GeoGridShort shadingGrid,
            float minShading, float maxShading,
            GeoGridShort tempShadingGridToDither,
            GeoGridShort tempLinesDensityGridToDither1,
            GeoGridShort tempLinesDensityGridToDither2,
            boolean generateScreeStones) {

        // make sure the bounding box intersects with the polygon to fill
        if (screeBB != null && !screeBB.intersects(screePolygon.getBounds2D(GeoObject.UNDEFINED_SCALE))) {
            return 0;
        }

        // convert the polygon to fill to a FastContainsGeoPath
        FastContainsGeoPath fastContainsGeoPath = new FastContainsGeoPath(screePolygon);
        fastContainsGeoPath.initContainsTest(shadingGrid.getCellSize() / POINT_IN_POLOGYON_TOLERANCE);

        // extract gully lines for the polygon
        ArrayList<GeoPath> screeLines;
        if (screeData.fixedScreeLines) {
            screeLines = getScreeLinesForPolygon(screeData.gullyLines, fastContainsGeoPath);
        } else {
            screeLines = generateGullyLines(fastContainsGeoPath,
                    tempLinesDensityGridToDither1,
                    tempLinesDensityGridToDither2);
        }

        // add gully lines to the GeoSet
        if (!screeData.fixedScreeLines) {
            for (GeoPath line : screeLines) {
                line.setVectorSymbol(GULLIES_VECTOR_SYMBOL);
                line.setSelectable(false);
                synchronized (screeData.gullyLines) {
                    screeData.gullyLines.add(line);
                }
            }
        }

        // fill the polygon with stones
        if (generateScreeStones) {
            GeoSet stones = fillScreePolygonWithStonesAndLines(
                    fastContainsGeoPath, screeLines, shadingGrid,
                    minShading, maxShading,
                    tempShadingGridToDither, screeBB);

            synchronized (screeData.screeStones) {
                screeData.screeStones.add(stones);
            }
            return stones.getNumberOfChildren();
        }

        return screeLines.size();
    }

    /**
     * Finds gully lines inside a polygon.
     *
     * @param polygon The polygon to fill with gully lines
     * @param binGrid A grid image containing other rasterized gully lines.
     * @return An array with the found gully lines.
     */
    private ArrayList<GridFalllineOperator.WeightedGeoPath> findGullyLines(
            GeoPath polygon,
            GeoBinaryGrid binGrid,
            GeoGridShort linesDensityToDither) {

        // fall lines are stored in this array
        ArrayList<GridFalllineOperator.WeightedGeoPath> lines;
        lines = new ArrayList<GridFalllineOperator.WeightedGeoPath>();

        // initialize search operator to find fall lines
        GridFalllineOperator fallLineOp = new GridFalllineOperator();
        fallLineOp.setMinSlopeDegree(p.lineMinSlopeDegree);
        fallLineOp.setSearchMethod(GridFalllineOperator.SearchMethod.UP_THEN_DOWN);

        // generate seed points, i.e. points where we start searching for fall lines
        ArrayList<Point2D> seedPoints;
        seedPoints = ScreeGenerator.diffuseDithering(polygon, linesDensityToDither);

        // search a fall line for each seed point
        for (Point2D seedPt : seedPoints) {
            final double x = seedPt.getX();
            final double y = seedPt.getY();

            fallLineOp.setStart(x, y);
            GridFalllineOperator.WeightedGeoPath line;
            line = fallLineOp.operate(screeData.dem, binGrid, polygon,
                    screeData.curvatureGrid, p.lineMinCurvature, null);

            // compute the mean curvature along the line by dividing the total
            // curvature by the length of the line.
            if (line != null) {
                //line.w /= line.getPointsCount();
            }
            if (isFallLineLongEnough(line)) {
                lines.add(line);
            }
        }

        return lines;

    }

    /**
     * Order an array of WeightedGeoPath by decreasing weight.
     *
     * @param lines The paths to sort.
     */
    private static void sort(ArrayList<GridFalllineOperator.WeightedGeoPath> lines) {

        Comparator<GridFalllineOperator.WeightedGeoPath> lineComparator;
        lineComparator = new Comparator<GridFalllineOperator.WeightedGeoPath>() {

            @Override
            public int compare(GridFalllineOperator.WeightedGeoPath o1,
                    GridFalllineOperator.WeightedGeoPath o2) {
                return o1.w < o2.w ? -1
                        : (o1.w > o2.w ? 1 : 0);
            }
        };

        Collections.sort(lines, Collections.reverseOrder(lineComparator));
    }

    /**
     *
     * @param allScreeLines
     * @param fastContainsGeoPath
     * @return
     */
    private ArrayList<GeoPath> getScreeLinesForPolygon(GeoSet allScreeLines,
            FastContainsGeoPath polygon) {

        while (allScreeLines.getNumberOfChildren() == 1 && allScreeLines.getGeoObject(0) instanceof GeoSet) {
            allScreeLines = (GeoSet) allScreeLines.getGeoObject(0);
        }

        ArrayList<GeoPath> screeLines = new ArrayList<GeoPath>();
        int nLines = allScreeLines.getNumberOfChildren();
        for (int i = 0; i < nLines; i++) {
            GeoPath line = (GeoPath) allScreeLines.getGeoObject(i);
            GeoPathIterator iter = line.getIterator();
            while (iter.next()) {
                final double x = iter.getX();
                final double y = iter.getY();
                if (polygon.contains(x, y)) {
                    screeLines.add(line);
                    break;
                }
            }
        }
        return screeLines;
    }

    public double getGullyGridCellsize() {
        // p.lineMinDistance can be 0
        double d = p.lineMinDistance / ScreeGenerator.REL_GULLIES_SEARCH_RESOLUTION;
        return Math.max(d, p.stoneMaxDiameter);
    }

    /**
     * Extract gully lines for a polygon.
     *
     * @param polygonToFill The polygon that delimits the scree area to fill.
     * @return The found lines.
     */
    private ArrayList<GeoPath> generateGullyLines(GeoPath polygonToFill,
            GeoGridShort tempLinesDensityGridToDither1,
            GeoGridShort tempLinesDensityGridToDither2) {

        // this array contains the gully lines that will be symbolized with stones
        ArrayList<GeoPath> lines = new ArrayList<GeoPath>();

        // create two grids with references to the fall lines inside the polygon
        Rectangle2D polyBounds = polygonToFill.getBounds2D(GeoObject.UNDEFINED_SCALE);
        double gridCellSize = getGullyGridCellsize();
        int gridCols = (int) (polyBounds.getWidth() / gridCellSize);
        int gridRows = (int) (polyBounds.getHeight() / gridCellSize);
        if (gridCols == 0 || gridRows == 0) {
            return lines;
        }

        GeoBinaryGrid doubleWidthBinGrid = new GeoBinaryGrid(gridCols,
                gridRows, polyBounds.getMinX(), polyBounds.getMaxY(),
                gridCellSize);
        GeoBinaryGrid singleWidthBinGrid = new GeoBinaryGrid(gridCols,
                gridRows, polyBounds.getMinX(), polyBounds.getMaxY(),
                gridCellSize);

        VectorSymbol doubleWidthSymbol = new VectorSymbol(null, Color.BLACK, (float) p.lineMinDistance * 2);
        VectorSymbol singleWidthSymbol = new VectorSymbol(null, Color.BLACK, (float) p.lineMinDistance);

        // find full length lines
        ArrayList<GridFalllineOperator.WeightedGeoPath> weightedLines
                = this.findGullyLines(polygonToFill,
                        null,
                        tempLinesDensityGridToDither1);

        // sort lines by curvature weight
        ScreeGenerator.sort(weightedLines);

        // add lines with biggest curvature weight
        for (GeoPath line : weightedLines) {
            line.setVectorSymbol(singleWidthSymbol);
            if (!singleWidthBinGrid.isAddingCausingOverlay(line, true)) {

                // also add the line to the grid with doubled line width
                line.setVectorSymbol(doubleWidthSymbol);
                doubleWidthBinGrid.rasterize(line);
                lines.add(line);
            }
        }

        // find cut lines. When finding lines no stroke width is used, i.e.
        // when detecting conflicts between the new line and existing objects in
        // the reference dem, the new lines is dimensionless. Therefore, the
        // reference dem with doubled line widths needs to be used here to
        // compensate for the dimensionless new line.
        weightedLines = this.findGullyLines(polygonToFill,
                doubleWidthBinGrid,
                tempLinesDensityGridToDither2);

        // sort cut lines by curvature weight
        ScreeGenerator.sort(weightedLines);

        // add cut lines with biggest curvature weight
        for (GeoPath line : weightedLines) {
            line.setVectorSymbol(singleWidthSymbol);
            if (!singleWidthBinGrid.isAddingCausingOverlay(line, true)) {
                lines.add(line);
            }
        }

        return lines;
    }

    public ArrayList<Point2D> toBeads(GeoPath line,
            double d,
            double jitterAlongLine,
            double jitterVertical) {

        Random random = new Random(0);

        if (d <= 0) {
            throw new IllegalArgumentException();
        }

        ArrayList<Point2D> xy = new ArrayList<Point2D>();

        GeoPathIterator iterator = line.getIterator();
        double startX = iterator.getX();
        double startY = iterator.getY();

        // add start point
        xy.add(new Point2D.Double(startX, startY));

        double lastMoveToX = startX;
        double lastMoveToY = startY;

        double length = 0;
        while (iterator.next()) {
            double endX = 0;
            double endY = 0;
            final int inst = iterator.getInstruction();
            switch (inst) {

                case GeoPathModel.CLOSE:
                    endX = lastMoveToX;
                    endY = lastMoveToY;
                    break;

                case GeoPathModel.MOVETO:
                    startX = lastMoveToX = iterator.getX();
                    startY = lastMoveToY = iterator.getY();
                    continue;

                default:
                    endX = iterator.getX();
                    endY = iterator.getY();
                    break;

            }

            // normalized direction dx and dy
            double dx = endX - startX;
            double dy = endY - startY;
            final double l = Math.hypot(dx, dy);
            dx /= l;
            dy /= l;

            double rest = length;
            length += l;
            while (length >= d) {
                // compute new point
                length -= d;
                startX += dx * (d - rest);
                startY += dy * (d - rest);
                rest = 0;
                Point2D.Double pt = new Point2D.Double(startX, startY);
                jitterPoint(pt, dx, dy, jitterAlongLine, jitterVertical, random);
                xy.add(pt);
            }
            startX = endX;
            startY = endY;
        }

        return xy;
    }

    private static void jitterPoint(Point2D.Double pt, double ndx, double ndy,
            double maxJitterAlongLine, double maxJitterVertical, Random random) {

        final double jitterAlongLine = maxJitterAlongLine * (random.nextDouble() - 0.5);
        final double jitterVertical = maxJitterVertical * (random.nextDouble() - 0.5);
        final double dx = ndx * jitterAlongLine - ndy * jitterVertical;
        final double dy = ndy * jitterAlongLine + ndx * jitterVertical;
        pt.x += dx;
        pt.y += dy;

    }

    /**
     * Fills a polygon with scree.
     *
     * @param polygonToFill GeoPath to fill with scree.
     * @param gullyLines Place stones along these lines.
     * @param shadingGrid grid to control the radius of scree dots.
     * @param screeBB
     * @return A set of stones for the passed polygonToFill
     */
    private GeoSet fillScreePolygonWithStonesAndLines(
            GeoPath polygonToFill,
            ArrayList<GeoPath> screeLines,
            GeoGridShort shadingGrid,
            float minShading, float maxShading,
            GeoGridShort tempShadingGridToDither,
            Rectangle2D screeBB) {

        Random random = new Random(0);

        final double minObstacleDist = p.stoneMinObstacleDistanceFraction * p.stoneMaxDiameter;
        final double stoneMaxR = p.stoneMaxDiameter / 2;

        double pointRasterCellSize = p.stoneMaxDiameter / REL_POINT_RASTER_RESOLUTION;
        Rectangle2D bb = polygonToFill.getBounds2D(GeoObject.UNDEFINED_SCALE);
        PointRaster pointRaster = new BitSetPointRaster(bb, pointRasterCellSize);

        // array to store all generated stones
        ArrayList<Stone> stones = new ArrayList<Stone>();

        // place stones along the gully lines        
        final double minStoneDistOnLine = p.lineStoneDistFraction * p.stoneMaxDiameter;
        final double jitterDist = p.stoneMaxDiameter * p.stoneMaxPosJitterFraction;

        final double t = p.lineSizeScaleTop;
        final double b = p.lineSizeScaleBottom;
        final double meanScale = (t + b) / 2;
        for (GeoPath line : screeLines) {

            // estimate the modulated mean radius of the points in the gully line
            GeoPathIterator iter = line.getIterator();
            double meanR = 0;
            do {
                double x = iter.getX();
                double y = iter.getY();
                meanR += modulatedStoneRadius(x, y, stoneMaxR,
                        shadingGrid, minShading, maxShading);
            } while (iter.next());
            meanR /= line.getPointsCount();

            // compute a series of points along the gully line
            final double d = meanR * 2 * meanScale + minStoneDistOnLine / 2;
            ArrayList<Point2D> pts = toBeads(line, d, jitterDist, jitterDist);

            // store each point on the gully line
            ArrayList<Stone> lineStones = new ArrayList<Stone>();
            int pointsCount = pts.size();
            for (int i = 0; i < pointsCount; i++) {
                Point2D pt = pts.get(i);

                final double x = pt.getX();
                final double y = pt.getY();
                if (screeBB != null && !screeBB.contains(pt)) {
                    continue;
                }

                final double rScale = (b - t) / pointsCount * i + t;
                final double r = modulatedStoneRadius(x, y, stoneMaxR,
                        shadingGrid, minShading, maxShading) * rScale;

                // make sure the new stone is not too close to other map elements
                if (isStoneOnObstacle(x, y, r + minObstacleDist)) {
                    continue;
                }

                // make sure the new stone is not too close to other points
                if (pointRaster.isCircleOverlaying(x, y, r + minStoneDistOnLine)) {
                    continue;
                }

                // create and store stone
                Stone stone = new Stone(x, y, r);
                stones.add(stone);
                lineStones.add(stone);
                pointRaster.addCircle(x, y, r);
            }

            final double linePointDist = p.lineToPointDistFraction * p.stoneMaxDiameter;
            for (Stone st : lineStones) {
                pointRaster.addCircle(st.x, st.y, st.r + linePointDist);
            }
        }

        // fill the polygon with randomly placed stones using Floyd-Steinberg
        // error diffusion dithering.
        ditherFillPolygon(polygonToFill, stones, tempShadingGridToDither,
                screeBB, pointRaster, shadingGrid, minShading, maxShading);

        // generate stones
        GeoSet stonesGeoSet = new GeoSet();
        for (Stone stone : stones) {
            generateStone(stone, random);
            stonesGeoSet.add(stone);
        }

        return stonesGeoSet;
    }

    /**
     * Tests whether a stone with a given radius conflicts with an obstacle,
     * i.e. the stone touches a cell that is not white in the
     * obstaclesMaskImage. The stone is treated as a regular circle.
     *
     * @param x Center of the stone.
     * @param y Center of the stone.
     * @param radius Radius of the stone in world coordinates [m].
     * @return True if the stone touches a cell in obstaclesMaskImage that is
     * not white.
     */
    private boolean isStoneOnObstacle(double x, double y, double radius) {

        // This uses normalized dem coordinates that are relative to the
        // top-left corner: y is downwards, one cell has a length of 1.
        // Variables in the normalized dem coordinates are marked with "_".
        // compute the cell size of the dem in normalized dem coordinates
        final double inverseCellSize_ = 1. / this.screeData.obstaclesMaskImage.getCellSize();

        // compute the stone position in normalized dem coordinates
        final double x_ = (x - screeData.obstaclesMaskImage.getWest()) * inverseCellSize_;
        final double y_ = (screeData.obstaclesMaskImage.getNorth() - y) * inverseCellSize_;

        // compute the radius in normalized dem coordinates
        final double radius_ = radius * inverseCellSize_;

        // compute the square of the radius in normalized dem coordinates
        final double radiusSqr_ = radius_ * radius_;

        // compute the stone position in columns and rows
        final int col = (int) x_;
        final int row = (int) y_;

        // compute the size of the scan area
        final int cells = (int) Math.ceil(radius_);

        // make sure we don't sample outside of the obstacles image
        final int w = screeData.obstaclesMaskImage.getCols();
        final int h = screeData.obstaclesMaskImage.getRows();
        if (col - cells < 0 || row - cells < 0 || col + cells >= w || row + cells >= h) {
            return false;
        }

        Raster obstaclesRaster = screeData.obstaclesMaskImage.getBufferedImage().getRaster();

        // loop over the scan area
        for (int r = row - cells; r <= row + cells; r++) {
            for (int c = col - cells; c <= col + cells; c++) {

                // compute the distance between the cell and the stone center
                // in normalized dem coordinates
                final double dx_ = c - x_;
                final double dy_ = r - y_;
                if ((dx_ * dx_ + dy_ * dy_) > radiusSqr_) {
                    continue;
                }

                final int gray = obstaclesRaster.getSample(c, r, 0);
                if (gray < 255) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the a pseudorandom value from a bell-shaped distribution with
     * mean {@code 0.0}. The standard deviation is not {@code 1.0}, but values
     * are clamped to the range between 0 and 1 (all positivie).
     *
     * @param random A random number generator.
     * @return A pseudorandom value between 0 and 1 with a bell distribution.
     */
    public static double clampedGaussian(Random random) {
        final double max = 3;
        double v;
        do {
            v = random.nextGaussian();
        } while (v > max || v < -max);
        v /= max;

        return Math.min(Math.abs(v), 1d);

    }

    /**
     * Fills a polygon with randomly placed stones using Floyd-Steinberg error
     * diffusion dithering.
     *
     * @param screePolygon The polygon to fill with stones.
     * @param stones Store new stones in this array.
     * @param shadingGrid grid to control the radius of scree dots.
     */
    private void ditherFillPolygon(GeoPath screePolygon,
            ArrayList<Stone> stones,
            GeoGridShort ditherGrid,
            Rectangle2D screeBB,
            PointRaster pointRaster,
            GeoGridShort shadingGrid,
            float minShading,
            float maxShading) {

        final int MAX_DITHER_TRIES = 20;

        Random random = new Random(0);

        final double maxStoneRadius = p.stoneMaxDiameter / 2;
        final double minStoneDist = p.stoneMinDistanceFraction * p.stoneMaxDiameter;
        final double minObstacleDist = p.stoneMinObstacleDistanceFraction * p.stoneMaxDiameter;
        final double jitterDist = p.stoneMaxDiameter * p.stoneMaxPosJitterFraction;

        Rectangle2D bounds = screePolygon.getBounds2D(GeoObject.UNDEFINED_SCALE);

        // stones are arranged in a dem with a cell size of p.stoneDist
        final int nRows = (int) (bounds.getHeight() / p.stoneMaxDiameter) + 2;
        final int nCols = (int) (bounds.getWidth() / p.stoneMaxDiameter) + 2;

        // generate stones in a regular raster covering the scree polygon
        double y = bounds.getMaxY();
        for (int row = 0; row < nRows; row++) {
            // traverse the image in zig-zag order. Even rows from left to right
            // and odd rows from right to left.
            int col = row % 2 == 0 ? 0 : nCols - 1;
            final int inc = row % 2 == 0 ? 1 : -1;
            for (; row % 2 == 0 ? col < nCols : col >= 0; col += inc) {
                final double x = bounds.getMinX() + col * p.stoneMaxDiameter;
                // make sure the stone is inside the polygon
                if (!screePolygon.contains(x, y)) {
                    continue;
                }

                // make sure the stone is inside the area of interest
                if (screeBB != null && !screeBB.contains(x, y)) {
                    continue;
                }

                // try a few times to place a stone such that it does not conflict
                // with any other stone or any obstacle.
                for (int i = 0; i < MAX_DITHER_TRIES; i++) {

                    double stoneX = x + random.nextGaussian() * jitterDist;
                    double stoneY = y + random.nextGaussian() * jitterDist;

                    // test whether the new stone would overlay any obstacle
                    // this test is not taking the variation of the stone radius into acount
                    if (isStoneOnObstacle(stoneX, stoneY, maxStoneRadius + minObstacleDist)) {
                        continue;
                    }

                    // make sure the jittered stone center is still inside the polygon
                    if (!screePolygon.contains(stoneX, stoneY)) {
                        continue;
                    }

                    final int shadeCol = (int) ((x - ditherGrid.getWest()) / ditherGrid.getCellSize());
                    final int shadeRow = (int) ((ditherGrid.getNorth() - y) / ditherGrid.getCellSize());

                    // test whether the new stone is on the grid to dither
                    final boolean onDitherGrid = (shadeCol > 0
                            && shadeRow > 0
                            && shadeCol < ditherGrid.getCols() - 1
                            && shadeRow < ditherGrid.getRows() - 1);
                    if (!onDitherGrid) {
                        continue;
                    }

                    float shade = ditherGrid.getValue(shadeCol, shadeRow);
                    final float dif;
                    if (shade < 128) {

                        // enlarge stones if they are placed on the largeStoneMask
                        final boolean largeStone;
                        if (screeData.largeStoneMaskImage == null) {
                            largeStone = false;
                        } else {
                            // large stone mask image is in grayscale
                            int gray = screeData.largeStoneMaskImage.getNearestGrayNeighbor(stoneX, stoneY);
                            // if outside of mask, gray is -1
                            largeStone = gray >= 0 && gray < 255;
                        }
                        final double rScale;
                        if (largeStone) {
                            // map random value [0..1] to [1..largeStoneMaxScale]
                            rScale = 1d + clampedGaussian(random) * (p.stoneLargeMaxScale - 1d);
                        } else {
                            rScale = 1.;
                        }

                        double r = maxStoneRadius * rScale;
                        // adjust the radius of the stone to the brightness of the shading
                        r = this.modulatedStoneRadius(stoneX, stoneY, r, shadingGrid, minShading, maxShading);
                        if (isStoneOnObstacle(stoneX, stoneY, r + minObstacleDist)
                                || pointRaster.isCircleOverlaying(stoneX, stoneY, r + minStoneDist)) {
                            continue;
                        }
                        stones.add(new Stone(stoneX, stoneY, r));
                        pointRaster.addCircle(stoneX, stoneY, r);
                        dif = largeStone ? (float) (shade * rScale * rScale) : shade;

                    } else {
                        dif = shade - 255;
                    }

                    // Floyd Steinberg error diffusion dithering
                    // right
                    float v = ditherGrid.getValue(shadeCol + inc, shadeRow);
                    ditherGrid.setValue((short) (v + dif * A), shadeCol + inc, shadeRow);

                    // left bottom
                    v = ditherGrid.getValue(shadeCol - inc, shadeRow + 1);
                    ditherGrid.setValue((short) (v + dif * B), shadeCol - inc, shadeRow + 1);

                    // center bottom
                    v = ditherGrid.getValue(shadeCol, shadeRow + 1);
                    ditherGrid.setValue((short) (v + dif * C), shadeCol, shadeRow + 1);

                    // right bottom
                    v = ditherGrid.getValue(shadeCol + inc, shadeRow + 1);
                    ditherGrid.setValue((short) (v + dif * D), shadeCol + inc, shadeRow + 1);

                    /*
                    // Atkinson dithering
                    // Atkinson dithering doesn't diffuse the entire quantization
                    // error, but only three quarters. It tends to preserve detail
                    // well, but very light and dark areas may appear blown out.
                    // This results in polygons not being tightly filled with stones.

                     // right
                    final float dif_8 = dif / 8f;
                    float v = ditherGrid.getValue(shadeCol + inc, shadeRow);
                    ditherGrid.setValue((short)(v + dif_8), shadeCol + inc, shadeRow);

                    // right of right
                    if (shadeCol + inc * 2 >= 0 && shadeCol + inc * 2 < ditherGrid.getCols()) {
                        v = ditherGrid.getValue(shadeCol + inc * 2, shadeRow);
                        ditherGrid.setValue((short)(v + dif_8), shadeCol + inc * 2, shadeRow);
                    }
                    
                    // bottom left
                    v = ditherGrid.getValue(shadeCol - inc, shadeRow + 1);
                    ditherGrid.setValue((short)(v + dif_8), shadeCol - inc, shadeRow + 1);

                    // bottom
                    v = ditherGrid.getValue(shadeCol, shadeRow + 1);
                    ditherGrid.setValue((short)(v + dif_8), shadeCol, shadeRow + 1);

                    // bottom right
                    v = ditherGrid.getValue(shadeCol + inc, shadeRow + 1);
                    ditherGrid.setValue((short)(v + dif_8), shadeCol + inc, shadeRow + 1);

                    // bottom bottom
                    if (shadeRow + 2 < ditherGrid.getRows()) {
                        v = ditherGrid.getValue(shadeCol, shadeRow + 2);
                        ditherGrid.setValue((short)(v + dif_8), shadeCol, shadeRow + 2);
                    }
                     */
                    break;
                }
            }
            y -= p.stoneMaxDiameter;
        }
    }

    private static ArrayList<Point2D> diffuseDithering(GeoPath screeOutline, GeoGridShort shading) {

        final double dist = shading.getCellSize();
        ArrayList<Point2D> points = new ArrayList<Point2D>();
        Rectangle2D bounds = screeOutline.getBounds2D(GeoObject.UNDEFINED_SCALE);

        // stones are arranged in a dem
        int nRows = (int) (bounds.getHeight() / dist) + 1;
        int nCols = (int) (bounds.getWidth() / dist) + 1;

        // generate stones in a regular raster covering the scree outline polygon
        double y = bounds.getMaxY();
        for (int row = 0; row < nRows; row++) {
            double x = bounds.getMinX();

            // traverse the image in zig-zag order. Even rows from left to right
            // and odd rows from right to left.
            int col = row % 2 == 0 ? 0 : nCols - 1;
            final int inc = row % 2 == 0 ? 1 : -1;
            for (; row % 2 == 0 ? col < nCols : col >= 0; col += inc) {

                final int shadeCol = (int) ((x - shading.getWest()) / dist);
                final int shadeRow = (int) ((shading.getNorth() - y) / dist);
                if (shadeCol > 0 && shadeRow > 0 && shadeCol < shading.getCols() - 1 && shadeRow < shading.getRows() - 1) {

                    float shade = shading.getValue(shadeCol, shadeRow);
                    final float dif;
                    if (shade < 128) {
                        points.add(new Point2D.Double(x, y));
                        dif = shade;
                    } else {
                        dif = shade - 255;
                    }

                    // Floyd Steinberg error diffusion dithering
                    // right
                    float v = shading.getValue(shadeCol + inc, shadeRow);
                    shading.setValue((short) (v + dif * A), shadeCol + inc, shadeRow);

                    // left bottom
                    v = shading.getValue(shadeCol - inc, shadeRow + 1);
                    shading.setValue((short) (v + dif * B), shadeCol - inc, shadeRow + 1);

                    // center bottom
                    v = shading.getValue(shadeCol, shadeRow + 1);
                    shading.setValue((short) (v + dif * C), shadeCol, shadeRow + 1);

                    // right bottom
                    v = shading.getValue(shadeCol + inc, shadeRow + 1);
                    shading.setValue((short) (v + dif * D), shadeCol + inc, shadeRow + 1);
                }

                x += dist;
            }
            y -= dist;
        }
        return points;
    }

    private double modulatedStoneRadius(double x, double y, double r,
            GeoGridShort shadingGrid, float minShading, float maxShading) {

        final double maxStoneRadius = p.stoneMaxDiameter / 2d;
        final double minStoneRadius = p.stoneMinDiameterScale * p.stoneMaxDiameter / 2d;
        final double scaledRadDif = (maxStoneRadius - minStoneRadius) / (maxShading - minShading);

        // scale stone radius by shading value
        short v = shadingGrid.getNearestNeighbor(x, y);
        if (v > 255) {
            v = 255;
        } else if (v < 0) {
            v = 0;
        }
        return r - (v - minShading) * scaledRadDif;

    }

    private GeoObject generateStone(Stone stone, Random random) {

        if (Double.isNaN(stone.x) || Double.isNaN(stone.y) || Double.isNaN(stone.r)) {
            return null;
        }

        double r = stone.r;
        double radiusVariance = p.stoneRadiusVariabilityPerc / 100. * random.nextDouble();
        // map to -maxRadiusVariancePercentage .. +maxRadiusVariancePercentage
        radiusVariance = -p.stoneRadiusVariabilityPerc / 100 + 2. * radiusVariance;
        r *= 1 + radiusVariance;
        stone.setR(r);

        // the number of corners of the stone
        int nbrCorners = (int) Math.round(p.stoneMinCornerCount + (p.stoneMaxCornerCount - p.stoneMinCornerCount) * random.nextDouble());
        double corners[] = new double[nbrCorners * 2];

        // the angle between two neighboring corners measured from the center
        final double angleIncrement = Math.PI * 2 / nbrCorners;
        double currAngle = Math.PI * random.nextDouble();

        for (int i = 0; i < nbrCorners; i++) {
            double angleVariance = p.stoneAngleVariabilityPerc / 100. * random.nextDouble();
            double angle = currAngle + angleIncrement * angleVariance;
            currAngle += angleIncrement;
            double cornerX = r * Math.cos(angle);
            double cornerY = r * Math.sin(angle);
            corners[i * 2] = cornerX + stone.x;
            corners[i * 2 + 1] = cornerY + stone.y;
        }
        stone.setCorners(corners); // this will update the path for drawing
        return stone;
    }

    private boolean isFallLineLongEnough(GeoPath line) {
        return line != null && line.getPointsCount() >= p.lineMinLengthApprox / screeData.dem.getCellSize();
    }
}
