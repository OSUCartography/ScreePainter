package ika.geoexport;

import ika.app.ScreeGenerator;
import java.io.*;
import java.util.*;
import java.awt.geom.*;
import ika.geo.*;
import ika.utils.FileUtils;

/**
 * Exporter for the ESRI Ungenerate file format.<br>
 * Ungenerate is a very simple text format for spaghetti data.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class UngenerateExporter extends GeoSetExporter {
    
    public UngenerateExporter(){
    }
    
    public String getFileFormatName(){
        return "Ungenerate";
    }
    
    public String getFileExtension() {
        return "lin";
    }
    
    /**
     * Writes GeoPaths contained in a GeoSet to an Ungenerate file.
     * @param geoSet The GeoSet containing the GeoPaths to export.
     * @param outputStream The output stream to receive the data.
     */
    protected void write(GeoSet geoSet, OutputStream outputStream) 
    throws IOException {
        if (geoSet == null || outputStream == null)
            throw new IllegalArgumentException();
        PrintWriter writer = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(outputStream)));
        this.writeLines(geoSet, writer, 0);
        writer.println("end");
        writer.flush();
    }
    
    /**
     * An internal helper method that writes paths to a PrintWriter.
     */
    private int writeLines(GeoSet geoSet, PrintWriter writer, int id) {
        if (geoSet.isVisible() == false)
            return id;
        
        final int numberOfChildren = geoSet.getNumberOfChildren();
        for (int i = 0; i < numberOfChildren; i++) {
            GeoObject geoObject = geoSet.getGeoObject(i);
            
            // only write visible objects
            if (geoObject.isVisible() == false) {
                continue;
            }

            // Scree Painter hack
            if (geoObject instanceof ScreeGenerator.Stone) {
                geoObject = ((ScreeGenerator.Stone) geoObject).toGeoPath();
            }

            if (geoObject instanceof GeoPath) {
                GeoPath geoPath = (GeoPath)geoObject;
                GeoPathIterator iterator = geoPath.getIterator();
                float lastMoveToX = Float.NaN;
                float lastMoveToY = Float.NaN;
                do {
                    final int type = iterator.getInstruction();
                    switch (type) {
                        case GeoPathModel.CLOSE:
                            if (iterator.atFirstInstruction())
                                continue;
                            writer.print(Float.toString(lastMoveToX));
                            writer.print("\t");
                            writer.println(Float.toString(lastMoveToY));
                            break;
                        case GeoPathModel.MOVETO:
                            if (!iterator.atFirstInstruction())
                                writer.println("end");
                            writer.println(id++);
                            lastMoveToX = iterator.getX();
                            lastMoveToY = iterator.getY();
                            // fall thru
                        case GeoPathModel.LINETO:
                            final float x = iterator.getX();
                            final float y = iterator.getY();
                            writer.print(Float.toString(x));
                            writer.print("\t");
                            writer.println(Float.toString(y));
                            break;
                            
                        default:
                            System.err.println("UngenerateExporter: unsupported path segment");
                    }
                } while (iterator.next());
                
                writer.println("end");
            } else if (geoObject instanceof GeoSet) {
                GeoSet childGeoSet = (GeoSet)geoObject;
                id = writeLines(childGeoSet, writer, id);
            }
        }
        return id;
    }
    
}