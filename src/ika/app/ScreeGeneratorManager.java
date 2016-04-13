package ika.app;

import ika.geo.GeoGridShort;
import ika.geo.GeoImage;
import ika.geo.GeoObject;
import ika.geo.GeoPath;
import ika.geo.grid.ImageToGridOperator;
import ika.gui.ProgressIndicator;
import ika.utils.GeometryUtils;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

/**
 * Coordinates the generation of scree. First initializes grid and image data
 * for generating scree. Then initializes the progress indicator. Then uses a
 * ScreeGenerator to fill each polygon with scree. Also updates the progress
 * indicator.
 *
 * @author Bernhard Jenny
 */
public class ScreeGeneratorManager {

    // Counts the number of stones generated.
    private int stonesCounter;

    //  stores the time needed to create scree stones.
    private long milliSecondsToGenerateStones;

    // if true, stones are created. Oherwise gully lines are created.
    private boolean generateScreeStones;

    public ScreeGeneratorManager() {
    }

    /**
     * Generate scree and gully lines for all polygons inside a bounding box.
     * @param screeGenerator used to create scree and gully lines
     * @param screeBB create scree and gully lines inside this optional bounding box. Can be null.
     * @param progress a progress indicator that will be regularly updated.
     * @param generateScreeStones if true, scree is generated, otherwise, only gully lines are generated.
     */
    public void generateScree(ScreeGenerator screeGenerator,
            Rectangle2D screeBB,
            ProgressIndicator progress,
            boolean generateScreeStones)  {

        this.stonesCounter = 0;
        this.generateScreeStones = generateScreeStones;

        try {
            long startTime = System.currentTimeMillis();

            if (progress != null) {
                progress.disableCancel();
                progress.start();
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
            if (progress != null) {
                progress.setMessage("Resampling shaded relief for scree generation...");
            }
            double cellSize = screeGenerator.p.stoneMaxDiameter;
            GeoImage resampledShading = screeGenerator.screeData.shadingImage.getResampledCopy(
                    cellSize,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC,
                    BufferedImage.TYPE_BYTE_GRAY);
            // convert from image to grid
            GeoGridShort tempResampledShadingGrid = new ImageToGridOperator().operateToShort(resampledShading);

            // apply gradation curve
            if (screeGenerator.screeData.shadingGradationMaskImage != null) {
                applyGradationCurves(screeGenerator.p,
                        screeGenerator.screeData.shadingGradationMaskImage,
                        tempResampledShadingGrid);
            } else {
                screeGenerator.p.shadingGradationCurve1.applyToGrid(tempResampledShadingGrid.getGrid());
            }

            GeoGridShort tempShadingGridToDither = tempResampledShadingGrid.clone();

            if (progress != null) {
                progress.setMessage("Resampling shaded relief for gully lines generation...");
            }
            resampledShading = screeGenerator.screeData.shadingImage.getResampledCopy(
                    screeGenerator.getGullyGridCellsize(),
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC,
                    BufferedImage.TYPE_BYTE_GRAY);
            screeGenerator.screeData.lineDensityGrid = new ImageToGridOperator().operateToShort(resampledShading);
            screeGenerator.p.lineGradationCurve.applyToGrid(screeGenerator.screeData.lineDensityGrid.getGrid());
            GeoGridShort tempLinesDensityGridToDither1 = screeGenerator.screeData.lineDensityGrid.clone();
            GeoGridShort tempLinesDensityGridToDither2 = screeGenerator.screeData.lineDensityGrid.clone();

            short[] minMax = tempResampledShadingGrid.getMinMax();
            short minShading = minMax[0];
            short maxShading = minMax[1];

            // prepare progress indicator for scree generation
            if (progress != null) {
                progress.enableCancel();
            }
            if (progress instanceof CmdLineProgress) {
                progress.setMessage("Generating scree");
            }

            // find all polygons that intersect with screeBB
            int nPolygons = screeGenerator.screeData.screePolygons.getNumberOfChildren();
            for (int i = 0; i < nPolygons; i++) {
                GeoObject polygon = screeGenerator.screeData.screePolygons.getGeoObject(i);
                final Rectangle2D bounds = polygon.getBounds2D(GeoObject.UNDEFINED_SCALE);
                if (screeBB != null && !GeometryUtils.rectanglesIntersect(screeBB, bounds)) {
                    continue;
                }

                int nItems = screeGenerator.generateScreeForPolygon(screeBB,
                        (GeoPath) polygon,
                        tempResampledShadingGrid,
                        minShading, maxShading,
                        tempShadingGridToDither,
                        tempLinesDensityGridToDither1,
                        tempLinesDensityGridToDither2,
                        generateScreeStones);
                stonesCounter += nItems;

                updateProgressIndicator(progress, i, nPolygons);
            }

            long endTime = System.currentTimeMillis();
            milliSecondsToGenerateStones = endTime - startTime;
        } finally {
            if (progress != null) {
                progress.complete();
            }
        }
    }

    /**
     * Apply the two gradations curves on tempResampledShadingGrid. The two
     * gradation curves are mixed based on the values stored in
     * screeGenerator.screeData.shadingGradationMaskImage
     * @param p scree generation settings
     * @param maskImage obstacles mask
     * @param tempResampledShadingGrid the shading image to change 
     */
    private void applyGradationCurves(ScreeParameters p, GeoImage maskImage,
            GeoGridShort tempResampledShadingGrid) {
        
        int[] table1 = p.shadingGradationCurve1.makeTable();
        int[] table2 = p.shadingGradationCurve2.makeTable();

        final double west = tempResampledShadingGrid.getWest();
        final double north = tempResampledShadingGrid.getNorth();
        final double cellSize = tempResampledShadingGrid.getCellSize();
        final short[][] grid = tempResampledShadingGrid.getGrid();
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

    /**
     * Updates the progress indicator with the current progress
     * @param progress indicator to update
     * @param currentPolygon the number of polygons filled so far
     * @param totalPolygons the total number of polygons to fill
     * @return if false, abort operation
     */
    private boolean updateProgressIndicator(ProgressIndicator progress,
            int currentPolygon, int totalPolygons) {
        DecimalFormat f = new DecimalFormat("#,###");
        StringBuilder sb = new StringBuilder();
        sb.append("<html>Filling polygon ");
        sb.append(currentPolygon);
        sb.append(" of ");
        sb.append(totalPolygons);
        sb.append(" <br>Total ");
        sb.append(generateScreeStones ? "stones" : "lines");
        sb.append(" generated: ");
        sb.append(f.format(stonesCounter));
        sb.append("</html>");

        if (progress != null) {
            if (progress.progress(100 * (currentPolygon + 1) / totalPolygons) == false) {
                return false;
            }
            if (progress instanceof CmdLineProgress == false) {
                progress.setMessage(sb.toString());
            }
        }

        return true;
    }

    /**
     * Returns a HTML string with statistics about last set of scree generated.
     * @return HTML string
     */
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

    /**
     * Returns the number of scree stones generated so far.
     * @return the number of scree stones generated so far.
     */
    public int nbrGeneratedScreeStones() {
        return stonesCounter;
    }
}
