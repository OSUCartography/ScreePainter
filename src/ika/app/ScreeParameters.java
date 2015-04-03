package ika.app;

import ika.gui.GradationGraph;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.StringTokenizer;

public class ScreeParameters {

    /**
     * first line of parameter file
     */
    private static final String FILE_FORMAT_IDENTIFIER = "Scree Painter Format";

    private static final float FILE_FORMAT_VERSION = 1.1f;
    
    /**
     * Scale of the map for conversion from mm in the user interface to m of the
     * input data.
     */
    public double mapScale = 25000;
    /**
     * maximum stone diameter in meter at 25,000 scale
     * mean diameter in swisstopo maps is about 0.017 cm
     */
    public double stoneMaxDiameter = 5.5;
    /**
     * the mean diameter of the smallest stones relative to the maximum diameter
     */
    public double stoneMinDiameterScale = 0.29;
    /**
     * The width of the buffer of white space around each stone to avoid stones
     * touching other stones, relative to the maximum diameter.
     */
    public double stoneMinDistanceFraction = 0.09;
    /**
     * there must be a buffer of white space around each stone to avoid stones
     * touching obstructing elements
     */
    public double stoneMinObstacleDistanceFraction = 0.18;
    /**
     * the radius of stones may randomly vary by this percentage
     */
    public double stoneRadiusVariabilityPerc = 18;
    /**
     * the angular distribution of corners in a stone may vary by this percentage
     */
    public double stoneAngleVariabilityPerc = 28;
    /**
     * scale the radius of stones on the large stones mask by a factor
     * between 1 and LARGE_RADIUS_SCALE
     */
    public double stoneLargeMaxScale = 1.8;
    /**
     * the minimum number of corners in a stone
     */
    public int stoneMinCornerCount = 4;
    /**
     * the maximum number of corners in a stone
     */
    public int stoneMaxCornerCount = 8;
    /**
     * the position of a stone may randomly vary by this distance in meters
     */
    public double stoneMaxPosJitterFraction = 0.36;
    /**
     * gradation curve applied to shading before extracting gully lines
     */
    public GradationGraph.Curve lineGradationCurve = new GradationGraph.Curve();
    /**
     * distance between stones along a fall line.
     */
    public double lineStoneDistFraction = 0.28;
    /**
     * enlarge stones at highest position of gully lines by this factor
     */
    public double lineSizeScaleTop = 1.32;

    /**
     * enlarge stones at lowest position of gully lines by this factor
     */
    public double lineSizeScaleBottom = 2;

    /**
     * minimum distance between a points in gully lines and other points
     */
    public double lineToPointDistFraction = 1.5;

    /**
     * the minimum distance between neighboring fall lines
     */
    public double lineMinDistance = 55;
    /**
     * a fall line must be at least this long to be valid
     */
    public double lineMinLengthApprox = 150;
    /**
     * a fall lines stops when the terrain is flatter than this value
     */
    public float lineMinSlopeDegree = 5;
    /**
     * the plan curvature must be at least this large along gully lines
     */
    public float lineMinCurvature = 0.0010F;
    /**
     * gradation curve applied to the shading image before dithering. This
     * adjusts the size and density of scree stones.
     */
    public GradationGraph.Curve shadingGradationCurve1 = new GradationGraph.Curve();
    /**
     * gradation curve applied to the shading image where the gradation mask is
     * dark.
     */
    public GradationGraph.Curve shadingGradationCurve2 = new GradationGraph.Curve();

    public ScreeParameters() {
    }

    /**
     * returns a string containing all parameters that can be saved to a file
     * and parsed by fromString().
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DecimalFormat f = new DecimalFormat("##0.####");
        DecimalFormatSymbols dfs = f.getDecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        f.setDecimalFormatSymbols(dfs);
        String lineSep = System.getProperty("line.separator");
        sb.append(FILE_FORMAT_IDENTIFIER);
        sb.append(" ");
        sb.append(FILE_FORMAT_VERSION);
        sb.append(lineSep);
        sb.append("Stones: Minimum Distance Fraction");
        sb.append(lineSep);
        sb.append(f.format(stoneMinDistanceFraction));
        sb.append(lineSep);
        sb.append("Stones: Minimum Distance Fraction to Obstructing Elements");
        sb.append(lineSep);
        sb.append(f.format(stoneMinObstacleDistanceFraction));
        sb.append(lineSep);
        sb.append("Stones: Maximum Diameter");
        sb.append(lineSep);
        sb.append(f.format(stoneMaxDiameter));
        sb.append(lineSep);
        sb.append("Stones: Relative Minimum Diameter");
        sb.append(lineSep);
        sb.append(f.format(stoneMinDiameterScale));
        sb.append(lineSep);
        sb.append("Stones: Radius Variability");
        sb.append(lineSep);
        sb.append(f.format(stoneRadiusVariabilityPerc));
        sb.append(lineSep);
        sb.append("Stones: Angular Variability");
        sb.append(lineSep);
        sb.append(f.format(stoneAngleVariabilityPerc));
        sb.append(lineSep);
        sb.append("Stones: Maximum Scale for Large Stones");
        sb.append(lineSep);
        sb.append(f.format(stoneLargeMaxScale));
        sb.append(lineSep);
        sb.append("Stones: Minimum Corners");
        sb.append(lineSep);
        sb.append(f.format(stoneMinCornerCount));
        sb.append(lineSep);
        sb.append("Stones: Maximum Corners");
        sb.append(lineSep);
        sb.append(f.format(stoneMaxCornerCount));
        sb.append(lineSep);
        sb.append("Stones: Maximum Position Jitter Fraction");
        sb.append(lineSep);
        sb.append(f.format(stoneMaxPosJitterFraction));
        sb.append(lineSep);
        sb.append("Stones: Gradation 1");
        sb.append(lineSep);
        sb.append(shadingGradationCurve1.toString());
        sb.append(lineSep);
        sb.append("Stones: Gradation 2");
        sb.append(lineSep);
        sb.append(shadingGradationCurve2.toString());
        sb.append(lineSep);
        sb.append("Lines: Gradation");
        sb.append(lineSep);
        sb.append(lineGradationCurve.toString());
        sb.append(lineSep);
        sb.append("Lines: Distance Fraction between Stones on Lines");
        sb.append(lineSep);
        sb.append(f.format(lineStoneDistFraction));
        sb.append(lineSep);
        sb.append("Lines: Top Stone Size Scale");
        sb.append(lineSep);
        sb.append(f.format(lineSizeScaleTop));
        sb.append(lineSep);
        sb.append("Lines: Bottom Stone Size Scale");
        sb.append(lineSep);
        sb.append(f.format(lineSizeScaleBottom));
        sb.append(lineSep);
        sb.append("Lines: Distance between Stones on Lines and other Stones");
        sb.append(lineSep);
        sb.append(f.format(lineToPointDistFraction));
        sb.append(lineSep);
        sb.append("Lines: Minimum Distance between Lines");
        sb.append(lineSep);
        sb.append(f.format(lineMinDistance));
        sb.append(lineSep);
        sb.append("Lines: Minimum Slope [Degree]");
        sb.append(lineSep);
        sb.append(f.format(lineMinSlopeDegree));
        sb.append(lineSep);
        sb.append("Lines: Minimum Length of Lines");
        sb.append(lineSep);
        sb.append(f.format(lineMinLengthApprox));
        sb.append(lineSep);
        sb.append("Lines: Minimum Curvature");
        sb.append(lineSep);
        sb.append(f.format(lineMinCurvature));
        sb.append(lineSep);
        return sb.toString();
    }

    /**
     * parses a string formatted by toString() and replaces all parameters
     * @param string The string to parse
     * @throws java.io.IOException Thrown when the string cannot be parsed.
     */
    public void fromString(String string) throws IOException {
        // make sure we have Flex Projector Format 1.0
        if (!string.startsWith(FILE_FORMAT_IDENTIFIER)) {
            throw new IOException("Not a Scree Painter File");
        }

        // ignore file format identifier
        string = string.substring(FILE_FORMAT_IDENTIFIER.length());
        StringTokenizer tokenizer = new StringTokenizer(string, "\n\r");

        float version = Float.parseFloat(tokenizer.nextToken());

        // overread "Stones: Minimum Distance Fraction"
        tokenizer.nextToken();
        this.stoneMinDistanceFraction = Double.parseDouble(tokenizer.nextToken());
        // overread "Stones: Minimum Distance Fraction to Obstructing Elements"
        tokenizer.nextToken();
        this.stoneMinObstacleDistanceFraction = Double.parseDouble(tokenizer.nextToken());
        // overread "Stones: Maximum Diameter"
        tokenizer.nextToken();
        this.stoneMaxDiameter = Double.parseDouble(tokenizer.nextToken());
        // overread Stones: Relative Minimum Diameter"
        tokenizer.nextToken();
        this.stoneMinDiameterScale = Double.parseDouble(tokenizer.nextToken());
        // overread "Stones: Radius Variability"
        tokenizer.nextToken();
        this.stoneRadiusVariabilityPerc = Double.parseDouble(tokenizer.nextToken());
        // overread "Stones: Angular Variability"
        tokenizer.nextToken();
        this.stoneAngleVariabilityPerc = Double.parseDouble(tokenizer.nextToken());
        // overread "Stones: Maximum Scale for Large Stones"
        tokenizer.nextToken();
        this.stoneLargeMaxScale = Double.parseDouble(tokenizer.nextToken());
        // overread "Stones: Minimum Corners"
        tokenizer.nextToken();
        this.stoneMinCornerCount = Integer.parseInt(tokenizer.nextToken());
        // overread "Stones: Maximum Corners"
        tokenizer.nextToken();
        this.stoneMaxCornerCount = Integer.parseInt(tokenizer.nextToken());
        // overread "Stones: Maximum Position Jitter Fraction"
        tokenizer.nextToken();
        this.stoneMaxPosJitterFraction = Double.parseDouble(tokenizer.nextToken());
        // overread "Stones: Gradation 1"
        tokenizer.nextToken();
        this.shadingGradationCurve1 = new GradationGraph.Curve(tokenizer.nextToken());
        // overread "Stones: Gradation 2"
        tokenizer.nextToken();
        this.shadingGradationCurve2 = new GradationGraph.Curve(tokenizer.nextToken());
        // overread "Lines: Gradation"
        tokenizer.nextToken();
        this.lineGradationCurve = new GradationGraph.Curve(tokenizer.nextToken());
        // overread "Lines: Distance Fraction between Stones on Lines"
        tokenizer.nextToken();
        this.lineStoneDistFraction = Double.parseDouble(tokenizer.nextToken());
        // overread "Lines: Top Stone Size Scale"
        tokenizer.nextToken();
        this.lineSizeScaleTop = Double.parseDouble(tokenizer.nextToken());

        if (version >= 1.1f) {
            // overread "Lines: Bottom Stone Size Scale"
            tokenizer.nextToken();
            this.lineSizeScaleBottom = Double.parseDouble(tokenizer.nextToken());

            // overread "Lines: Distance between Stones on Lines and other Stones"
            tokenizer.nextToken();
            this.lineToPointDistFraction = Double.parseDouble(tokenizer.nextToken());
        }
        
        // overread "Lines: Minimum Distance between Stones"
        tokenizer.nextToken();
        this.lineMinDistance = Double.parseDouble(tokenizer.nextToken());
        // overread "Lines: Minimum Slope [Degree]"
        tokenizer.nextToken();
        this.lineMinSlopeDegree = Float.parseFloat(tokenizer.nextToken());
        // overread "Lines: Minimum Length of Lines"
        tokenizer.nextToken();
        this.lineMinLengthApprox = Double.parseDouble(tokenizer.nextToken());
        // overread "Lines: Minimum Curvature"
        tokenizer.nextToken();
        this.lineMinCurvature = Float.parseFloat(tokenizer.nextToken());
    }
}
