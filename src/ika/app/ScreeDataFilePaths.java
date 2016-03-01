package ika.app;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Handles file paths to input data.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ScreeDataFilePaths implements Cloneable {

    public static final String DEF_LARGE_STONES_MASK_PATH = "large stones mask.tif";
    public static final String DEF_GRADATION_MASK_PATH = "gradation mask.tif";
    public static final String DEF_OBSTACLES_PATH = "obstacles mask.tif";
    public static final String DEF_REF_PATH = "reference.tif";
    public static final String DEF_DEM_PATH = "dem.asc";
    public static final String DEF_SHADING_PATH = "shaded relief.tif";
    public static final String DEF_SCREE_PATH = "scree.shp";
    public static final String DEF_GULLY_LINES_PATH = "lines.shp";

    private static Preferences getPreferences() {
        return Preferences.userNodeForPackage(ScreeDataFilePaths.class);
    }

    private static void writeFilePathToPreferences(String key, String path) {
        if (key != null && path != null) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                getPreferences().put(key, path);
            }
        }
    }

    private static String readFilePathFromPreferences(String key) {
        String path = getPreferences().get(key, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists() == false || file.isFile() == false) {
                path = null;
            }
        }
        return path;
    }

    private static String dirPath;

    static {
        // load path to last folder from preferences
        dirPath = getPreferences().get("directory", "");
    }

    public static String getDirPath() {
        return dirPath;
    }

    public static void setDirPath(String directory) {
        dirPath = directory;

        // save selected folder in preferences
        getPreferences().put("directory", dirPath);
    }

    public String screePolygonsFilePath,
            demFilePath,
            shadingFilePath,
            gradationMaskFilePath,
            obstaclesFilePath,
            largeStoneFilePath,
            referenceFilePath,
            gullyLinesFilePath;

    public ScreeDataFilePaths() {
        screePolygonsFilePath = readFilePathFromPreferences("screePolygonsFilePath");
        demFilePath = readFilePathFromPreferences("demFilePath");
        shadingFilePath = readFilePathFromPreferences("shadingFilePath");
        gradationMaskFilePath = readFilePathFromPreferences("gradationMaskFilePath");
        obstaclesFilePath = readFilePathFromPreferences("obstaclesFilePath");
        largeStoneFilePath = readFilePathFromPreferences("largeStoneFilePath");
        referenceFilePath = readFilePathFromPreferences("referenceFilePath");
        gullyLinesFilePath = readFilePathFromPreferences("gullyLinesFilePath");
    }

    @Override
    public ScreeDataFilePaths clone() throws CloneNotSupportedException {
        ScreeDataFilePaths clone = (ScreeDataFilePaths) super.clone();
        return clone;
    }

    public void writePathsToPreferences() {
        writeFilePathToPreferences("screePolygonsFilePath", screePolygonsFilePath);
        writeFilePathToPreferences("demFilePath", demFilePath);
        writeFilePathToPreferences("shadingFilePath", shadingFilePath);
        writeFilePathToPreferences("gradationMaskFilePath", gradationMaskFilePath);
        writeFilePathToPreferences("obstaclesFilePath", obstaclesFilePath);
        writeFilePathToPreferences("largeStoneFilePath", largeStoneFilePath);
        writeFilePathToPreferences("referenceFilePath", referenceFilePath);
        writeFilePathToPreferences("gullyLinesFilePath", gullyLinesFilePath);
        try {
            getPreferences().flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(ScreeDataFilePaths.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String screePolygonsFilePath() {
        return screePolygonsFilePath == null ? dirPath + DEF_SCREE_PATH : screePolygonsFilePath;
    }

    public String gullyLinesFilePath() {
        return gullyLinesFilePath == null ? dirPath + DEF_GULLY_LINES_PATH : gullyLinesFilePath;
    }

    public String demFilePath() {
        return demFilePath == null ? dirPath + DEF_DEM_PATH : demFilePath;
    }

    public String shadingFilePath() {
        return shadingFilePath == null ? dirPath + DEF_SHADING_PATH : shadingFilePath;
    }

    public String gradationMaskFilePath() {
        return gradationMaskFilePath == null ? dirPath + DEF_GRADATION_MASK_PATH : gradationMaskFilePath;
    }

    public String obstaclesFilePath() {
        return obstaclesFilePath == null ? dirPath + DEF_OBSTACLES_PATH : obstaclesFilePath;
    }

    public String largeStonesFilePath() {
        return largeStoneFilePath == null ? dirPath + DEF_LARGE_STONES_MASK_PATH : largeStoneFilePath;
    }

    public String referenceFilePath() {
        return referenceFilePath == null ? dirPath + DEF_REF_PATH : referenceFilePath;
    }

    private static String filePathOrInfo(String defaultPath, String path) {
        return path == null ? "No file selected. Default file name is: " + defaultPath : path;
    }

    public String screePolygonsFilePathOrName() {
        return filePathOrInfo(DEF_SCREE_PATH, screePolygonsFilePath);
    }

    public String demFilePathOrName() {
        return filePathOrInfo(DEF_DEM_PATH, demFilePath);
    }

    public String shadingFilePathOrName() {
        return filePathOrInfo(DEF_SHADING_PATH, shadingFilePath);
    }

    public String gradationMaskFilePathOrName() {
        return filePathOrInfo(DEF_GRADATION_MASK_PATH, gradationMaskFilePath);
    }

    public String gullyLinesFilePathOrName() {
        String defaultPath = DEF_GULLY_LINES_PATH;
        String path = gullyLinesFilePath;
        return path == null ? "No file selected, gullies are computed from "
                + "the elevation model. Default file name is: " + defaultPath : path;
    }

    public String obstaclesFilePathOrName() {
        return filePathOrInfo(DEF_OBSTACLES_PATH, obstaclesFilePath);
    }

    public String largeStonesFilePathOrName() {
        return filePathOrInfo(DEF_LARGE_STONES_MASK_PATH, largeStoneFilePath);
    }

    public String referenceFilePathOrName() {
        return filePathOrInfo(DEF_REF_PATH, referenceFilePath);
    }

    public boolean isShadingFilePathValid() {
        return isFile(shadingFilePath);
    }

    public boolean isDEMFilePathValid() {
        return isFile(demFilePath);
    }

    public boolean isScreePolygonsFilePathValid() {
        return isFile(screePolygonsFilePath);
    }

    public boolean isObstaclesFilePathValid() {
        return isFile(obstaclesFilePath);
    }

    public boolean isLargeStonesFilePathValid() {
        return isFile(largeStoneFilePath);
    }

    public boolean isGradationMaskFilePathValid() {
        return isFile(gradationMaskFilePath);
    }

    public boolean isGullyLinesFilePath() {
        return isFile(gullyLinesFilePath);
    }

    public boolean isReferenceFilePath() {
        return isFile(referenceFilePath);
    }
    
    public String toCommandLineArguments() {
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        sb.append("--shading ").append("\"").append(shadingFilePath()).append("\"").append(nl);
        sb.append("--dem ").append("\"").append(demFilePath()).append("\"").append(nl);
        sb.append("--scree_polygons ").append("\"").append(screePolygonsFilePath()).append("\"").append(nl);
        sb.append("--obstacles_mask ").append("\"").append(obstaclesFilePath()).append("\"").append(nl);
        sb.append("--large_stones_mask ").append("\"").append(largeStonesFilePath()).append("\"").append(nl);
        sb.append("--gradation_mask ").append("\"").append(gradationMaskFilePath()).append("\"").append(nl);
        sb.append("--gully_lines ").append("\"").append(gullyLinesFilePath()).append("\"").append(nl);
        sb.append("--reference_image ").append("\"").append(referenceFilePath()).append("\"").append(nl);
        return sb.toString();
    }

    private boolean isFile(String filePath) {
        File f = new File(filePath);
        return f.exists() && !f.isDirectory();
    }

}
