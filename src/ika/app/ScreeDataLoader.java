package ika.app;

import ika.geo.GeoImage;
import ika.geo.GeoObject;
import ika.geo.GeoSet;
import ika.geo.grid.GridPlanCurvatureOperator;
import ika.geoimport.ESRIASCIIGridReader;
import ika.geoimport.GeoImporter;
import ika.geoimport.ImageImporter;
import ika.gui.ProgressIndicator;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Bernhard Jenny, School of Mathematical and Geospatial Sciences, RMIT
 * University, Melbourne
 */
public class ScreeDataLoader {
    
    private final ScreeDataFilePaths screeInputData;
    private final ScreeData screeData;
    
    public ScreeDataLoader(ScreeDataFilePaths screeInputData, ScreeData screeData) {
        this.screeInputData = screeInputData;
        this.screeData = screeData;
    }
    
    private static GeoImage loadImage(String filePath,
            String name,
            ProgressIndicator progressIndicator) throws IOException {
        ImageImporter importer = new ImageImporter();
        importer.setOptimizeForDisplay(false);
        importer.setProgressIndicator(progressIndicator, false);
        GeoImage geoImage = (GeoImage) importer.read(filePath);
        if (geoImage != null) {
            geoImage.setName(name);
            geoImage.setVisible(false);
            geoImage.setSelectable(false);
        }
        return geoImage;
    }

    public void loadDEM(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading elevation model...");
        prog.enableCancel();
        screeData.dem = ESRIASCIIGridReader.read(screeInputData.demFilePath());
        screeData.curvatureGrid = new GridPlanCurvatureOperator().operate(screeData.dem);
    }

    public void loadShading(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading shaded relief...");
        prog.enableCancel();
        screeData.shadingImage = loadImage(screeInputData.shadingFilePath(),
                ScreeData.SHADING_IMAGE_NAME, prog);
        if (screeData.shadingImage != null) {
            screeData.shadingImage.convertToGrayscale();
        }
    }

    public void loadLargeStonesMask(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading mask for large stones...");
        prog.enableCancel();
        screeData.largeStoneMaskImage = loadImage(screeInputData.largeStonesFilePath(),
                ScreeData.LARGE_STONE_IMAGE_NAME, prog);
        if (screeData.largeStoneMaskImage != null) {
            screeData.largeStoneMaskImage.convertToGrayscale();
        }
    }

    public void loadGradationMask(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading mask for alternative gradation...");
        prog.enableCancel();
        screeData.shadingGradationMaskImage = loadImage(screeInputData.gradationMaskFilePath(),
                ScreeData.GRADATION_MASK_IMAGE_NAME, prog);
        if (screeData.shadingGradationMaskImage != null) {
            screeData.shadingGradationMaskImage.convertToGrayscale();
        }
    }

    public void loadObstaclesMask(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading obstacles mask image...");
        prog.enableCancel();
        screeData.obstaclesMaskImage = loadImage(screeInputData.obstaclesFilePath(),
                ScreeData.OBSTACLES_IMAGE_NAME, prog);
        if (screeData.obstaclesMaskImage != null) {
            // this image will be drawn frequently, and it could be in color,
            // so optimize it for the display hardware.
            screeData.obstaclesMaskImage.optimizeForDisplay();
        }
    }

    public void loadReferenceImage(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading reference image...");
        prog.enableCancel();
        screeData.referenceImage = loadImage(screeInputData.referenceFilePath(),
                ScreeData.REF_IMAGE_NAME, prog);
        if (screeData.referenceImage != null) {
            // this image will be drawn frequently, and it could be in color,
            // so optimize it for the display hardware.
            screeData.referenceImage.optimizeForDisplay();
        }
    }

    public void loadScreePolygons(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading scree polygons...");
        prog.enableCancel();
        String filePath = screeInputData.screePolygonsFilePath();
        GeoImporter importer = GeoImporter.findGeoImporter(filePath);

        GeoObject geoObject = importer.read(filePath);
        final GeoSet newScreePolygons;
        if (geoObject instanceof GeoSet) {
            newScreePolygons = (GeoSet) geoObject;
        } else {
            // single paths are not returned in a GeoSet
            newScreePolygons = new GeoSet();
            newScreePolygons.add(geoObject);
        }
        screeData.screePolygons.replaceGeoObjects(newScreePolygons);
        screeData.screePolygons.setVectorSymbol(ScreeGenerator.SCREE_POLYGON_VECTOR_SYMBOL);
        screeData.screePolygons.setVisible(false);
        screeData.screePolygons.setSelectable(false);
        screeData.screePolygons.setName(ScreeData.POLYGONS_NAME);
    }

    public void loadGullyLines(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading gully lines...");
        prog.enableCancel();
        String filePath = screeInputData.gullyLinesFilePath();
        GeoImporter importer = GeoImporter.findGeoImporter(filePath);
        GeoObject geoObject = importer.read(filePath);
        final GeoSet newGullyLines;
        if (geoObject instanceof GeoSet) {
            newGullyLines = (GeoSet) geoObject;
        } else {
            // single paths are not returned in a GeoSet
            newGullyLines = new GeoSet();
            newGullyLines.add(geoObject);
        }
        screeData.gullyLines.replaceGeoObjects(newGullyLines);
        screeData.gullyLines.setVectorSymbol(ScreeGenerator.GULLIES_VECTOR_SYMBOL);
        screeData.gullyLines.setVisible(false);
        screeData.gullyLines.setSelectable(false);
        screeData.gullyLines.setName(ScreeData.LINES_NAME);
        screeData.fixedScreeLines = true;
    }

    private boolean fileExists(String path) {
        if (path == null) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isFile();
    }
}
