package ika.app;

import java.util.prefs.Preferences;

/**
 * Handles file paths to input data.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ScreeDataFilePaths implements Cloneable{
    public static final String DEF_LARGE_STONES_MASK_PATH = "large stones mask.tif";
    public static final String DEF_GRADATION_MASK_PATH = "gradation mask.tif";
    public static final String DEF_OBSTACLES_PATH = "obstacles mask.tif";
    public static final String DEF_REF_PATH = "reference.tif";
    public static final String DEF_DEM_PATH = "dem.asc";
    public static final String DEF_SHADING_PATH = "shaded relief.tif";
    public static final String DEF_SCREE_PATH = "scree.shp";
    public static final String DEF_GULLY_LINES_PATH = "lines.shp";

    private static String dirPath;
    static {
        // load path to last folder from preferences
        Preferences prefs = Preferences.userNodeForPackage(ScreeDataFilePaths.class);
        dirPath = prefs.get("directory", "");
    }

    public static String getDirPath() {
        return dirPath;
    }

    public static void setDirPath(String directory) {
        dirPath = directory;

        // save selected folder in preferences
        Preferences prefs = Preferences.userNodeForPackage(ScreeDataFilePaths.class);
        prefs.put("directory", dirPath);
    }

    public String screePolygonsFilePath,
            demFilePath,
            shadingFilePath,
            gradationMaskFilePath,
            obstaclesFilePath,
            largeStoneFilePath,
            referenceFilePath,
            gullyLinesFilePath;

    public ScreeDataFilePaths(){
    }

    @Override
    public ScreeDataFilePaths clone() throws CloneNotSupportedException {
        ScreeDataFilePaths clone=(ScreeDataFilePaths)super.clone();
        return clone;
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
        return path == null ? "No file selected, gullies are computed from " +
                "the elevation model. Default file name is: " + defaultPath : path;
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

}