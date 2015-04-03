package ika.geoimport;

import java.io.*;
import java.util.*;
import ika.geo.*;
import ika.gui.ProgressIndicator;
import ika.utils.StringUtils;

public class ESRIASCIIGridReader {

    /** Read a Grid from a file in ESRI ASCII format.
     * @param fileName The path to the file to be read.
     * @return The read grid.
     */
    public static GeoGrid read(String filePath) throws java.io.IOException {
        return ESRIASCIIGridReader.read(filePath, null);
    }

    /** Read a Grid from a file in ESRI ASCII format.
     * @param fileName The path to the file to be read.
     * @param progress A WorkerProgress to inform about the progress.
     * @return The read grid.
     */
    public static GeoGrid read(String filePath, ProgressIndicator progressIndicator)
            throws java.io.IOException {

        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file.getAbsolutePath());
        GeoGrid grid = ESRIASCIIGridReader.read(fis, progressIndicator);
        if (progressIndicator != null && progressIndicator.isAborted()) {
            return null;
        }
        String name = file.getName();
        if (!"".equals(name)) {
            grid.setName(name);
        }
        return grid;

    }

    /**
     * Read a Grid from a file in ESRI ASCII format.
     * @return The read grid.
     */
    public static GeoGrid read(InputStream input, ProgressIndicator progressIndicator)
            throws IOException {

        // initialize the progress monitor at the beginning
        if (progressIndicator != null) {
            progressIndicator.start();
        }

        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader reader = new BufferedReader(isr);
        try {
            String line, str;
            StringTokenizer tokenizer;
            int cols = 0;
            int rows = 0;
            double west = Double.NaN;
            double south = Double.NaN;
            double cellSize = Double.NaN;
            float noDataValue = Float.NaN;

            while (true) {
                line = reader.readLine();
                tokenizer = new StringTokenizer(line, " \t");
                str = tokenizer.nextToken().trim().toLowerCase();
                if (str.equals("ncols")) {
                    cols = Integer.parseInt(tokenizer.nextToken());
                    if (cols <= 0) {
                        throw new IOException();
                    }
                } else if (str.equals("nrows")) {
                    rows = Integer.parseInt(tokenizer.nextToken());
                    if (rows <= 0) {
                        throw new IOException();
                    }
                } else if (str.equals("xllcenter") || str.equals("xllcorner")) {
                    west = Double.parseDouble(tokenizer.nextToken());
                } else if (str.equals("yllcenter") || str.equals("yllcorner")) {
                    south = Double.parseDouble(tokenizer.nextToken());
                } else if (str.equals("cellsize")) {
                    cellSize = Double.parseDouble(tokenizer.nextToken());
                    if (cellSize <= 0) {
                        throw new IOException();
                    }
                } else if (str.startsWith("nodata")) {
                    noDataValue = Float.parseFloat(tokenizer.nextToken());
                } else {
                    // make sure the line starts with a number
                    if (!StringUtils.isDouble(str)) {
                        throw new IOException();
                    }

                    // finished reading the header
                    break;
                }
            }

            // test if valid values have been found
            if (cols <= 0 || rows <= 0 || cellSize <= 0 || Double.isNaN(cellSize)) {
                throw new IOException();
            }

            GeoGrid grid = new GeoGrid(cols, rows, cellSize);

            //orientation = true for horizontal grids
            grid.setWest(west);
            grid.setNorth(south + (rows - 1) * cellSize);

            // read values of grid. Rows are stored from top to bottom in the file.
            for (int row = 0; row < rows; row++) {

                if (progressIndicator != null) {
                    if (!progressIndicator.progress((int) ((double) (row + 1) / rows * 100))) {
                        return null;
                    }
                }

                tokenizer = new StringTokenizer(line, " ");

                for (int col = 0; col < cols; col++) {
                    final float v = Float.parseFloat(tokenizer.nextToken());
                    if (v == noDataValue) {
                        grid.setValue(Float.NaN, col, row);
                    } else {
                        grid.setValue(v, col, row);
                    }
                }
                if (row < rows - 1) {
                    line = reader.readLine();
                }
            }

            return grid;

        } finally {
            try {
                reader.close();
            } catch (Exception exc) {
            }
        }

    }
}