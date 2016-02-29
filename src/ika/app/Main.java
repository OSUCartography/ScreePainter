/*
 * Main.java
 *
 * Created on November 1, 2005, 10:19 AM
 *
 */
package ika.app;

import com.sanityinc.jargs.CmdLineParser;
import com.sanityinc.jargs.CmdLineParser.Option;
import ika.gui.*;
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
                "Usage: ScreePainter parameters shading dem scree_polygons obstacles_mask output_file [large_stones_mask] [gradation_mask] [gullyLines] [reference_image]\n"
                + "    parameters: scree parameter file in Scree Painter format 1.1.\n"
                + "    shading: shaded relief file path (format: raster image with world file).\n"
                + "    dem: elevation model file path (format: Esri ASCII grid).\n"
                + "    scree_polygons: scree polygons shapefile file path (Esri shapefile).\n"
                + "    obstacles_mask: obstacles mask file path (format: raster image with world file).\n"
                + "    output_file: output file path."
                + "    large_stones_mask: large stones mask file path (format: raster image with world file). Optional.\n"
                + "    gradation_mask: gradation maks file path (format: raster image with world file). Optional.\n"
                + "    gully_lines: gully lines file path (Esri shapefile). Optional.\n"
                + "    reference_image: reference image file path (format: raster image with world file). Optional.\n"
        );
    }

    private static void testFile(String filePath, String info) {
        if (filePath == null) {
            System.out.println("Missing command line argument '" + info
                    + "': A path to a " + info + " file is required.");
            System.exit(2);
        }
        File f = new File(filePath);
        if (!f.exists() || f.isDirectory()) {
            String str = info + " file does not exist at " + filePath + ".";
            System.out.println(str);
            System.exit(2);
        }
    }

    private static void parseCommandLine(String args[]) {
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
        // Option<String> outputFormat = parser.addStringOption("outformat");

        // parse the user-provided command line arguments, and catch any errors
        // Options may appear on the command line in any order, and may even
        // appear after some or all of the non-option arguments.
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

        // If the options were not specified, the corresponding values will be null.
        String parametersFilePath = parser.getOptionValue(parameters);
        String shadingFilePath = parser.getOptionValue(shading);
        String demFilePath = parser.getOptionValue(dem);
        String screePolygonsFilePath = parser.getOptionValue(screePolygons);
        String obstaclesMaskFilePath = parser.getOptionValue(obstaclesMask);
        String largeStonesMaksFilePath = parser.getOptionValue(largeStonesMask);
        String gradationMaskFilePath = parser.getOptionValue(gradationMask);
        String gullyLinesFilePath = parser.getOptionValue(gullyLines);
        String referenceImageFilePath = parser.getOptionValue(referenceImage);
        String outputFilePath = parser.getOptionValue(output);
        // String outputFileFormat = parser.getOptionValue(outputFormat);

        // verbose info
        if (parser.getOptionValue(verbose, false)) {
            System.out.println("parameters file: " + parametersFilePath);
            System.out.println("shading file: " + shadingFilePath);
            System.out.println("dem file: " + demFilePath);
            System.out.println("scree polygons file: " + screePolygonsFilePath);
            System.out.println("obstacles mask file: " + obstaclesMaskFilePath);
            System.out.println("large stones mask file: " + largeStonesMaksFilePath);
            System.out.println("gradation mask file: " + gradationMaskFilePath);
            System.out.println("gully lines file: " + gullyLinesFilePath);
            System.out.println("reference image file: " + referenceImageFilePath);
            System.out.println("output file: " + outputFilePath);
            // System.out.println("output format: " + outputFileFormat);
        }

        // test if all required parameters have been provided. Exit otherwise. 
        testFile(parametersFilePath, parameters.longForm());
        testFile(shadingFilePath, shading.longForm());
        testFile(demFilePath, dem.longForm());
        testFile(screePolygonsFilePath, screePolygons.longForm());
        testFile(obstaclesMaskFilePath, obstaclesMask.longForm());
    }

    /**
     * main routine for the application.
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        if (args.length > 0) {
            parseCommandLine(args);
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
                MainWindow win = MainWindow.newDocumentWindow();

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
