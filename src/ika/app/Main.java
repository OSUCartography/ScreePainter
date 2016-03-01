/*
 * Main.java
 *
 * Created on November 1, 2005, 10:19 AM
 *
 */
package ika.app;

import com.sanityinc.jargs.CmdLineParser;
import com.sanityinc.jargs.CmdLineParser.Option;
import ika.geoexport.GeospatialPDFExporter;
import ika.gui.*;
import ika.utils.FileUtils;
import ika.utils.IconUtils;
import java.io.File;
import javax.swing.*;

/**
 * Main entry point.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Main {

    private static void printUsage() {
        System.out.println(
                "Usage: ScreePainter parameters shading dem scree_polygons obstacles_mask output_file west south width height scale [large_stones_mask] [gradation_mask] [gullyLines] [reference_image]\n"
                + "    parameters: scree parameter file in Scree Painter format 1.1.\n"
                + "    shading: shaded relief file path (format: raster image with world file).\n"
                + "    dem: elevation model file path (format: Esri ASCII grid).\n"
                + "    scree_polygons: scree polygons shapefile file path (Esri shapefile).\n"
                + "    obstacles_mask: obstacles mask file path (format: raster image with world file).\n"
                + "    output_file: output file path.\n"
                + "    output_format: output file format (\"Geospatial PDF (Swiss CH1903+ / LV95)\", Illustrator, PDF, Shape, SVG, TerraGoGeoPDF)\n"
                + "    west: western border of map sheet in ground coordinates\n"
                + "    south: shourthern border of map sheet in ground coordinates\n"
                + "    width: width of map sheet in ground coordinates\n"
                + "    height: height of map sheet in ground coordinates\n"
                + "    scale: scale of map (such as 50000 for 1:50,000)\n"
                + "    large_stones_mask: large stones mask file path (format: raster image with world file). Optional.\n"
                + "    gradation_mask: gradation mask file path (format: raster image with world file). Optional.\n"
                + "    gully_lines: gully lines file path (Esri shapefile). Optional.\n"
                + "    reference_image: reference image file path (format: raster image with world file). Optional.\n"
        );
    }

    private static void exitIfFileDoesNotExist(String filePath, String info) {
        if (filePath == null) {
            System.err.println("Missing command line argument '" + info
                    + "': A path to a " + info + " file is required.");
            System.exit(2);
        }
        File f = new File(filePath);
        if (!f.exists() || f.isDirectory()) {
            String str = info + " file does not exist at " + filePath + ".";
            System.err.println(str);
            System.exit(2);
        }
    }

    private static CommandLineArguments parseCommandLine(String args[]) {
        /*
        This uses JArgs, a GNU getopt-style command-line argument parser
        http://jargs.sourceforge.net
        Example use: https://github.com/purcell/jargs/blob/master/src/examples/java/com/sanityinc/jargs/examples/OptionTest.java
        
        Also see POSIX argument conventions
        http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html
         */

        // First, you must create a CmdLineParser, and add to it the
        // appropriate Options.
        CmdLineParser parser = new CmdLineParser();

        Option<Boolean> verbose = parser.addBooleanOption('v', "verbose");
        Option<Boolean> help = parser.addBooleanOption('h', "help");

        // Options may have just a long form with no corresponding short form.
        Option<String> parameters = parser.addStringOption("parameters");
        Option<String> shading = parser.addStringOption("shading");
        Option<String> dem = parser.addStringOption("dem");
        Option<String> screePolygons = parser.addStringOption("scree_polygons");
        Option<String> obstaclesMask = parser.addStringOption("obstacles_mask");
        Option<String> largeStonesMask = parser.addStringOption("large_stones_mask");
        Option<String> gradationMask = parser.addStringOption("gradation_mask");
        Option<String> gullyLines = parser.addStringOption("gully_lines");
        Option<String> referenceImage = parser.addStringOption("reference_image");
        Option<String> output = parser.addStringOption("output_file");
        Option<String> outputFormat = parser.addStringOption("output_format");
        Option<Double> west = parser.addDoubleOption("west");
        Option<Double> south = parser.addDoubleOption("south");
        Option<Double> width = parser.addDoubleOption("width");
        Option<Double> height = parser.addDoubleOption("height");
        Option<Double> scale = parser.addDoubleOption("scale");

        // parse the user-provided command line arguments, and catch any errors
        // Options may appear on the command line in any order
        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        if (parser.getOptionValue(help, false)) {
            printUsage();
            System.exit(0);
        }

        CommandLineArguments cmd = new CommandLineArguments();
        cmd.parametersFilePath = parser.getOptionValue(parameters);
        cmd.dataFilePaths.shadingFilePath = parser.getOptionValue(shading);
        cmd.dataFilePaths.demFilePath = parser.getOptionValue(dem);
        cmd.dataFilePaths.screePolygonsFilePath = parser.getOptionValue(screePolygons);
        cmd.dataFilePaths.obstaclesFilePath = parser.getOptionValue(obstaclesMask);
        cmd.dataFilePaths.largeStoneFilePath = parser.getOptionValue(largeStonesMask);
        cmd.dataFilePaths.gradationMaskFilePath = parser.getOptionValue(gradationMask);
        cmd.dataFilePaths.gullyLinesFilePath = parser.getOptionValue(gullyLines);
        cmd.dataFilePaths.referenceFilePath = parser.getOptionValue(referenceImage);
        cmd.outputFilePath = parser.getOptionValue(output);
        cmd.outputFormat = parser.getOptionValue(outputFormat);
        cmd.west = parser.getOptionValue(west);
        cmd.south = parser.getOptionValue(south);
        cmd.width = parser.getOptionValue(width);
        cmd.height = parser.getOptionValue(height);
        cmd.scale = parser.getOptionValue(scale);

        if (cmd.outputFormat == null) {
            cmd.outputFormat = new GeospatialPDFExporter().getFileFormatName();
        }

        // verbose info
        if (parser.getOptionValue(verbose, false)) {
            System.out.println("parameters file: " + cmd.parametersFilePath);
            System.out.println("shading file: " + cmd.dataFilePaths.shadingFilePath);
            System.out.println("dem file: " + cmd.dataFilePaths.demFilePath);
            System.out.println("scree polygons file: " + cmd.dataFilePaths.screePolygonsFilePath);
            System.out.println("obstacles mask file: " + cmd.dataFilePaths.obstaclesFilePath);
            System.out.println("large stones mask file: " + cmd.dataFilePaths.largeStoneFilePath);
            System.out.println("gradation mask file: " + cmd.dataFilePaths.gradationMaskFilePath);
            System.out.println("gully lines file: " + cmd.dataFilePaths.gullyLinesFilePath);
            System.out.println("reference image file: " + cmd.dataFilePaths.referenceFilePath);
            System.out.println("output file: " + cmd.outputFilePath);
            System.out.println("output format: " + cmd.outputFormat);
            System.out.println("west: " + cmd.west);
            System.out.println("south: " + cmd.south);
            System.out.println("width: " + cmd.width);
            System.out.println("height: " + cmd.height);
            System.out.println("scale: " + cmd.scale);
        }

        // test if all required parameters have been provided. Exit otherwise. 
        exitIfFileDoesNotExist(cmd.parametersFilePath, parameters.longForm());
        exitIfFileDoesNotExist(cmd.dataFilePaths.shadingFilePath, shading.longForm());
        exitIfFileDoesNotExist(cmd.dataFilePaths.demFilePath, dem.longForm());
        exitIfFileDoesNotExist(cmd.dataFilePaths.screePolygonsFilePath, screePolygons.longForm());
        exitIfFileDoesNotExist(cmd.dataFilePaths.obstaclesFilePath, obstaclesMask.longForm());

        // test if destination directory is valid
        String outputDirectory = FileUtils.getParentDirectoryPath(cmd.outputFilePath);
        File dir = new File(outputDirectory);
        if (dir.isDirectory() == false) {
            System.err.println("Output directory does not exist: " + dir);
            System.exit(2);
        }

        // test if numerical values are valid
        if (cmd.width == null || cmd.width <= 0) {
            System.err.println("width must be a positive value: " + cmd.width);
            System.exit(2);
        }
        if (cmd.height == null || cmd.height <= 0) {
            System.err.println("height must be a positive value: " + cmd.height);
            System.exit(2);
        }
        if (cmd.scale == null || cmd.scale <= 0) {
            System.err.println("scale must be a positive value: " + cmd.scale);
            System.exit(2);
        }

        return cmd;
    }

    /**
     * main routine for the application.
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        final CommandLineArguments commandLineArguments;
        if (args.length > 0) {
            commandLineArguments = parseCommandLine(args);
        } else {
            commandLineArguments = null;
        }

        // on Mac OS X: take the menu bar out of the window and put it on top
        // of the main screen.
        if (ika.utils.Sys.isMacOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Scree Painter");
        }

        // set icon for JOptionPane dialogs
        java.util.Properties props
                = ika.utils.PropertiesLoader.loadProperties("ika.app.Application");
        IconUtils.setOptionPaneIcons(props.getProperty("ApplicationIcon"));

        // Replace title of progress monitor dialog by empty string.
        UIManager.put("ProgressMonitor.progressText", "");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                // show std out in own window
                // new ika.utils.StdErrOutWindows(null, "Console Output", "Error Output");
                // create a temporary invisible BaseMainWindow, extract its
                // menu bar and pass it to the MacWindowsManager.
                if (ika.utils.Sys.isMacOSX()) {
                    MacWindowsManager.init(MainWindow.getMenuBarClone());
                }

                // create a new empty window
                MainWindow win = MainWindow.newDocumentWindow(commandLineArguments);

                // user canceled data selection dialog.
                if (win == null) {
                    System.exit(0);
                }

                /*
                // initialize output and error stream for display in a window
                String appName = ika.app.ApplicationInfo.getApplicationName();
                String outTitle = appName + " - Standard Output";
                String errTitle = appName + " - Error Messages";
                new ika.utils.StdErrOutWindows(null, outTitle, errTitle);
                 */
            }
        });

    }
}
