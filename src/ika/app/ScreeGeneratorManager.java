package ika.app;

import ika.geo.GeoGridShort;
import ika.geo.GeoImage;
import ika.geo.GeoObject;
import ika.geo.GeoPath;
import ika.geo.grid.ImageToGridOperator;
import ika.gui.ProgressIndicator;
import ika.gui.SwingWorkerWithProgressIndicator;
import ika.utils.GeometryUtils;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Distributes the generation of scree to multiple threads.
 *
 * @author jenny
 */
public class ScreeGeneratorManager /*implements Runnable*/ {

    /**
     * The id of the next polygon to fill with scree
     */
    private int polygonID;
    /**
     * The polygons to fill with scree. This can be a selection of all GeoSets:
     * only polygons in the update area are filled with scree.
     */
    private ArrayList<GeoPath> tempPolygonsToFill;
    /**
     * Only generate scree stones inside this box.
     */
    private Rectangle2D screeBB;
    /**
     * progress indicator for the generation process.
     */
    private ProgressIndicator progress;
    /**
     * ScreeGenerator fills a polygon with scree stones.
     */
    private ScreeGenerator screeGenerator;

    /**
     * Counts the number of stones generated.
     */
    private int stonesCounter;

    private long milliSecondsToGenerateStones = 0;

    private boolean generateScreeStones;

    /**
     * A copy of the shaded resampled to the stone resolution. The values of
     * this grid will change during the generation of scree stones.
     */
    private GeoGridShort tempResampledShadingGrid;
    private float minShading, maxShading;

    private GeoGridShort tempShadingGridToDither;
    private GeoGridShort tempLinesDensityGridToDither1, tempLinesDensityGridToDither2;

    public ScreeGeneratorManager() {
    }

    public String getHTMLReportForLastGeneration() {
        DecimalFormat format = new DecimalFormat("#,##0.#");
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("Number of ");
        sb.append(this.generateScreeStones ? "stones" : "lines");
        sb.append(" generated: ");
        sb.append(format.format(this.stonesCounter));
        sb.append("<br>");
        sb.append("Time required: ");
        sb.append(format.format(milliSecondsToGenerateStones / 1000d));
        sb.append(" seconds");
        sb.append("</html>");
        return sb.toString();
    }

    public void generateScree(ScreeGenerator screeGenerator,
            Rectangle2D screeBB,
            ProgressIndicator progress,
            boolean generateScreeStones) throws InterruptedException {

        this.polygonID = 0;
        this.screeGenerator = screeGenerator;
        this.screeBB = screeBB;
        this.progress = progress;
        this.stonesCounter = 0;
        this.generateScreeStones = generateScreeStones;

        try {
            long startTime = System.currentTimeMillis();

            if (progress != null) {
                progress.disableCancel();
                progress.start();
            }

            // find all polygons that intersect with screeBB
            tempPolygonsToFill = new ArrayList<>();
            int nPolygons = screeGenerator.screeData.screePolygons.getNumberOfChildren();
            for (int i = 0; i < nPolygons; i++) {
                GeoObject polygon = screeGenerator.screeData.screePolygons.getGeoObject(i);
                final Rectangle2D bounds = polygon.getBounds2D(GeoObject.UNDEFINED_SCALE);
                if (screeBB != null && !GeometryUtils.rectanglesIntersect(screeBB, bounds)) {
                    continue;
                }
                tempPolygonsToFill.add((GeoPath) polygon);
            }

            // remove existing scree lines
            if (!screeGenerator.screeData.fixedScreeLines) {
                screeGenerator.screeData.gullyLines.removeAllGeoObjects();
            }

            // remove existing scree dots
            screeGenerator.screeData.screeStones.removeAllGeoObjects();

            // Resample shading to resolution of scree stones, used for placing
            // stones with a Floyd-Steinberg diffuse dithering algorithm.
            // The values in tempResampledShadingGrid will be changed when dithering.
            double cellSize = screeGenerator.p.stoneMaxDiameter;
            GeoImage resampledShading = screeGenerator.screeData.shadingImage.getResampledCopy(
                    cellSize,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC,
                    BufferedImage.TYPE_BYTE_GRAY);
            // convert from image to grid
            tempResampledShadingGrid = new ImageToGridOperator().operateToShort(resampledShading);

            // apply gradation curve
            if (screeGenerator.screeData.shadingGradationMaskImage != null) {
                applyGradationCurves(screeGenerator.p);
            } else {
                screeGenerator.p.shadingGradationCurve1.applyToGrid(tempResampledShadingGrid.getGrid());
            }
            short[] minMax = tempResampledShadingGrid.getMinMax();
            minShading = minMax[0];
            maxShading = minMax[1];

            tempShadingGridToDither = tempResampledShadingGrid.clone();

            resampledShading = screeGenerator.screeData.shadingImage.getResampledCopy(
                    screeGenerator.getGullyGridCellsize(),
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC,
                    BufferedImage.TYPE_BYTE_GRAY);
            screeGenerator.screeData.lineDensityGrid = new ImageToGridOperator().operateToShort(resampledShading);
            screeGenerator.p.lineGradationCurve.applyToGrid(screeGenerator.screeData.lineDensityGrid.getGrid());
            tempLinesDensityGridToDither1 = screeGenerator.screeData.lineDensityGrid.clone();
            tempLinesDensityGridToDither2 = screeGenerator.screeData.lineDensityGrid.clone();

            if (progress != null) {
                progress.enableCancel();
            }

            /*
            int nThreads = Runtime.getRuntime().availableProcessors();
            ArrayList<Thread> threads = new ArrayList(nThreads);

            // interruption exception handler for all threads
            Thread.UncaughtExceptionHandler h = (Thread th, Throwable ex) -> {
                threads.stream().forEach((t) -> {
                    if (t != th) {
                        t.interrupt();
                    }
                });
            };

            for (int i = 0; i < nThreads; i++) {
                Thread t = new Thread(this);
                t.setName("Scree Generator" + " " + i);
                threads.add(t);
                t.setUncaughtExceptionHandler(h);
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }*/
            run();

            // release memory, which is for example needed to export the scree
            tempPolygonsToFill = null;
            tempResampledShadingGrid = null;
            tempShadingGridToDither = null;
            tempLinesDensityGridToDither1 = null;
            tempLinesDensityGridToDither2 = null;

            long endTime = System.currentTimeMillis();
            milliSecondsToGenerateStones = endTime - startTime;

        } finally {
            if (progress != null) {
                progress.complete();
            }
        }

    }

    /**
     * apply the two gradations curves on tempResampledShadingGrid. The two
     * gradation curves are mixed based on the values stored in
     * screeGenerator.screeData.shadingGradationMaskImage
     *
     * @param p
     */
    private void applyGradationCurves(ScreeParameters p) {
        int[] table1 = p.shadingGradationCurve1.makeTable();
        int[] table2 = p.shadingGradationCurve2.makeTable();

        final double west = tempResampledShadingGrid.getWest();
        final double north = tempResampledShadingGrid.getNorth();
        final double cellSize = tempResampledShadingGrid.getCellSize();
        final short[][] grid = tempResampledShadingGrid.getGrid();
        GeoImage maskImage = screeGenerator.screeData.shadingGradationMaskImage;
        for (int r = 0; r < grid.length; r++) {
            final short[] row = grid[r];
            final double y = north - cellSize * r;
            for (int c = 0; c < row.length; c++) {
                final float v = row[c];
                final double x = west + cellSize * c;
                final int gray = maskImage.getNearestGrayNeighbor(x, y);
                final double w;
                if (gray < 0) {
                    w = 1;  // point outside of mask
                } else {
                    w = gray / 255d;
                }
                if (v <= 0) {
                    row[c] = (short) (w * table1[0] + (1 - w) * table2[0]);
                } else if (v >= 255) {
                    row[c] = (short) (w * table1[255] + (1 - w) * table2[255]);
                } else {
                    row[c] = (short) (w * table1[(int) v] + (1 - w) * table2[(int) v]);
                }

            }
        }
    }

    private /*synchronized*/ GeoPath getNextPolygon() {
        if (polygonID >= tempPolygonsToFill.size()) {
            return null;
        }
        return tempPolygonsToFill.get(polygonID++);
    }

    private /*synchronized*/ int increaseCounter(int newStones) {
        stonesCounter += newStones;
        return stonesCounter;
    }

    //@Override
    public void run() {

        final int nPolygons = tempPolygonsToFill.size();
        final double cellSize = tempResampledShadingGrid.getCellSize() / 2;
        final DecimalFormat f = new DecimalFormat("#,###");

        StringBuilder sb = new StringBuilder();
        sb.append(" of ");
        sb.append(nPolygons);
        sb.append(" <br>Total ");
        sb.append(generateScreeStones ? "stones" : "lines");
        sb.append(" generated: ");
        final String msgPart = sb.toString();
        GeoPath polygon;
        while ((polygon = getNextPolygon()) != null) {

            int nItems = screeGenerator.generateScree(screeBB,
                    polygon,
                    cellSize,
                    tempResampledShadingGrid,
                    minShading, maxShading,
                    tempShadingGridToDither,
                    tempLinesDensityGridToDither1,
                    tempLinesDensityGridToDither2,
                    generateScreeStones);

            //synchronized (progress) {
            // update progress
            sb.delete(0, sb.length());
            sb.append("<html>Filling polygon ");
            sb.append(polygonID);
            sb.append(msgPart);
            sb.append(f.format(increaseCounter(nItems)));
            sb.append("</html>");

            if (progress != null) {
                if (progress.progress(100 * polygonID / nPolygons) == false) {
                    return;
                }
                progress.setMessage(sb.toString());
            }
        }
    }
}
