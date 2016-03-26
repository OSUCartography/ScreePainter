package ika.app;

import com.sanityinc.jargs.CmdLineParser;
import ika.geoexport.GeoSetExporter;
import ika.geoexport.GeospatialPDFExporter;
import ika.geoexport.ShapeExporter;
import ika.geoexport.ShapeGeometryExporter;
import ika.geoexport.VectorGraphicsExporter;
import ika.gui.GeoExportGUI;
import ika.gui.PageFormat;
import ika.utils.FileUtils;
import java.io.File;
import java.io.IOException;

/**
 * Command line version of Scree Painter. This version does not display a GUI.
 *
 * @author Bernhard Jenny, School of Mathematical and Geospatial Sciences, RMIT
 * University, Melbourne
 */
public class CmdLineMain {

    private CmdLineMain() {
    }

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

    public static CommandLineArguments parseCommandLine(String args[]) {
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

        CmdLineParser.Option<Boolean> verbose = parser.addBooleanOption('v', "verbose");
        CmdLineParser.Option<Boolean> help = parser.addBooleanOption('h', "help");
        CmdLineParser.Option<Boolean> version = parser.addBooleanOption("version");

        // Options may have just a long form with no corresponding short form.
        CmdLineParser.Option<String> parameters = parser.addStringOption("parameters");
        CmdLineParser.Option<String> shading = parser.addStringOption("shading");
        CmdLineParser.Option<String> dem = parser.addStringOption("dem");
        CmdLineParser.Option<String> screePolygons = parser.addStringOption("scree_polygons");
        CmdLineParser.Option<String> obstaclesMask = parser.addStringOption("obstacles_mask");
        CmdLineParser.Option<String> largeStonesMask = parser.addStringOption("large_stones_mask");
        CmdLineParser.Option<String> gradationMask = parser.addStringOption("gradation_mask");
        CmdLineParser.Option<String> gullyLines = parser.addStringOption("gully_lines");
        CmdLineParser.Option<String> referenceImage = parser.addStringOption("reference_image");
        CmdLineParser.Option<String> output = parser.addStringOption("output_file");
        CmdLineParser.Option<String> outputFormat = parser.addStringOption("output_format");
        CmdLineParser.Option<Double> west = parser.addDoubleOption("west");
        CmdLineParser.Option<Double> south = parser.addDoubleOption("south");
        CmdLineParser.Option<Double> width = parser.addDoubleOption("width");
        CmdLineParser.Option<Double> height = parser.addDoubleOption("height");
        CmdLineParser.Option<Double> scale = parser.addDoubleOption("scale");

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

        if (parser.getOptionValue(version, false)) {
            System.out.println(ApplicationInfo.getApplicationVersion());
            System.exit(0);
        }

        CommandLineArguments cmd = new CommandLineArguments();
        cmd.verbose = parser.getOptionValue(verbose, false);
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
        if (cmd.verbose) {
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

    public static void runCommandLine(CommandLineArguments commandLineArguments) {
        try {
            ScreeGenerator screeGenerator = new ScreeGenerator();
            CmdLineProgress prog = new CmdLineProgress();
            ScreeDataLoader loader = new ScreeDataLoader(
                    commandLineArguments.dataFilePaths, screeGenerator.screeData);

            // load required data
            loader.loadDEM(prog);
            prog.complete();
            loader.loadShading(prog);
            prog.complete();
            loader.loadObstaclesMask(prog);
            prog.complete();
            loader.loadScreePolygons(prog);
            prog.complete();

            // load optional data
            if (commandLineArguments.dataFilePaths.isLargeStonesFilePathValid()) {
                loader.loadLargeStonesMask(prog);
                prog.complete();
            }
            if (commandLineArguments.dataFilePaths.isGradationMaskFilePathValid()) {
                loader.loadGradationMask(prog);
                prog.complete();
            }
            if (commandLineArguments.dataFilePaths.isGullyLinesFilePath()) {
                loader.loadGullyLines(prog);
                prog.complete();
            }

            // load parameters file
            File f = new File(commandLineArguments.parametersFilePath);
            screeGenerator.p.fromString(new String(FileUtils.getBytesFromFile(f)));

            // generate scree
            ScreeGeneratorManager manager = new ScreeGeneratorManager();
            manager.generateScree(screeGenerator, null, prog, true);
            System.out.format("Generated %,d scree stones.%n", manager.nbrGeneratedScreeStones());

            // export scree
            screeGenerator.screeData.screeStones.setVisible(true);
            GeoSetExporter exporter = GeoExportGUI.getExporterByName(commandLineArguments.outputFormat);
            if (exporter == null) {
                throw new IOException("Unknown format " + commandLineArguments.outputFormat);
            }

            // setup format of map
            PageFormat pageFormat = new PageFormat();
            pageFormat.setPageScale(commandLineArguments.scale);
            pageFormat.setPageLeft(commandLineArguments.west);
            pageFormat.setPageBottom(commandLineArguments.south);
            pageFormat.setPageHeightWorldCoordinates(commandLineArguments.height);
            pageFormat.setPageWidthWorldCoordinates(commandLineArguments.width);

            // configre Esri shapefile exporter
            if (exporter instanceof ShapeExporter) {
                ((ShapeExporter) exporter).setShapeType(ShapeGeometryExporter.POLYGON_SHAPE_TYPE);
            }

            // configure Swiss LV95 coordinate system
            if (exporter instanceof GeospatialPDFExporter) {
                GeospatialPDFExporter geospatialPDFExporter = (GeospatialPDFExporter) exporter;
                geospatialPDFExporter.setWKT(SwissLV95GeospatialPDFExport.wkt);
                float[] corners = SwissLV95GeospatialPDFExport.lonLatCornerPoints(pageFormat);
                if (corners == null) {
                    throw new Exception("Cannot initialize Swiss LV95 Coordinate System");
                }
                geospatialPDFExporter.setLonLatCornerPoints(corners);
            }

            if (exporter instanceof VectorGraphicsExporter) {
                ((VectorGraphicsExporter) exporter).setPageFormat(pageFormat);
            }

            // screeGenerator.screeData.screeStones contains ScreeGenerator.Stone,
            // a class that derives from GeoObject but is not usually supported by
            // exporters. The stones could be converted to GeoPaths using
            // stone.toGeoPath, however, this would multiply the amount of memory
            // required to store the graphics. The exporters have therefore each
            // been hacked to call stone.toGeoPath and then using the standard
            // export routines for GeoPaths.
            exporter.setDocumentName(ApplicationInfo.getApplicationName());
            exporter.setDocumentAuthor(System.getProperty("user.name"));
            exporter.setDocumentSubject("scree");
            exporter.setDocumentKeyWords("");
            GeoExportGUI.export(exporter, screeGenerator.screeData.screeStones,
                    commandLineArguments.outputFilePath, null);

            System.out.format("Saved file to %s%n", commandLineArguments.outputFilePath);

        } catch (Throwable ex) {
            String msg = "Scree could not be generated completely.";
            if (ex instanceof java.lang.OutOfMemoryError) {
                msg += "\nThere is not enough memory available.";
                msg += "\nTry adjusting memory with -Xmx";
            }
            System.err.println(msg);
            
            if (commandLineArguments.verbose) {
                ex.printStackTrace();
            }
        }
    }
}
