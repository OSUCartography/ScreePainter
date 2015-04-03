/*
 * AdobeCurveReader.java
 *
 * Created on July 27, 2007, 11:02 AM
 *
 */
package ika.app;

import ika.gui.GradationGraph;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class AdobeCurveReader {

    private GradationGraph.Curve curves[];

    /** Creates a new instance of AdobeCurveReader */
    public AdobeCurveReader() {
    }

    public void readACV(DataInput in) throws IOException {

        final int MAX_CURVES_COUNT = 17;
        final int MAX_POINTS_COUNT = 19;

        int version = in.readShort();
        //if (version != 1) // CS2 and CS3 write undocumented 4 instead of 1
        //    throw new IOException("Not a ACV file.");

        int curvesCount = in.readShort();
        if (curvesCount < 1) {
            throw new IOException("File does not contain any curve");
        }
        if (curvesCount > MAX_CURVES_COUNT) {
            throw new IOException("File contains too many curves");
        }
        this.curves = new GradationGraph.Curve[curvesCount];

        for (int curveID = 0; curveID < curvesCount; curveID++) {
            int pointsCount = in.readShort();
            if (pointsCount > MAX_POINTS_COUNT) {
                throw new IOException("Curve contains too many points");
            }
            this.curves[curveID] = new GradationGraph.Curve();

            for (int ptID = 0; ptID < pointsCount; ptID++) {
                // first number is output value, second number is input value
                float y = in.readShort() / 255f;
                float x = in.readShort() / 255f;
                this.curves[curveID].addKnot(x, y);
            }
        }
    }

    public void readACV(URL acvURL) throws IOException {
        DataInputStream din = null;
        try {
            din = new DataInputStream(acvURL.openStream());
            this.readACV(din);
        } finally {
            if (din != null) {
                din.close();
            }
        }
    }

    public void readACV(String filePath) throws IOException {
        DataInputStream din = null;
        try {
            din = new DataInputStream(new FileInputStream(filePath));
            this.readACV(din);
        } finally {
            if (din != null) {
                din.close();
            }
        }
    }

    public int getCurvesCount() {
        return this.curves == null ? 0 : this.curves.length;
    }

    public GradationGraph.Curve getCurve(int id) {
        return this.curves == null ? null : this.curves[id];
    }

    public static void main(String[] args) {
        try {
            String filePath = ika.utils.FileUtils.askFile(null, "Adobe Photoshop Curves File", true);
            AdobeCurveReader acr = new AdobeCurveReader();
            acr.readACV(filePath);
            System.out.println("Number of curves: " + acr.getCurvesCount());
            for (int i = 0; i < acr.getCurvesCount(); i++) {
                System.out.println(acr.getCurve(i));
            }
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
