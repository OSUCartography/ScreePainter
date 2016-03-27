/*
 * PathModel.java
 *
 * Created on March 30, 2007, 11:32 AM
 *
 */

package ika.geo;

import ika.utils.GeometryUtils;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * GeoPathModel holds the geometry model of a GeoPath. It does not handle 
 * symbolization.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public final class GeoPathModel implements Serializable, Cloneable {
    
    private static final long serialVersionUID = 74565327855645505L;
    
    public static final byte NONE = 0; // no instruction defined.
                                       // NONE is not stored in this.instructions[]
    
    public static final byte MOVETO = 1;
    public static final byte LINETO = 2;
    public static final byte CURVETO = 3;       // cubic bezier curve
    public static final byte QUADCURVETO = 4;   // quadratic bezier curve
    public static final byte CLOSE = 5;
    
    /**
     * An array containing all points: x1, y1, x2, y2, etc.
     */
    protected double[] points = new double[0];
    
    /**
     * An array containing MOVETO, LINETO, CURVETO, QUADCURVETO or CLOSE.
     */
    protected byte[] instructions = new byte[0];
    
    /**
     * The bounding box of this path.
     * Rectangle2D is not serializable!
     */
    private transient Rectangle2D bounds = null;
    
    /** Creates a new instance of PathModel */
    public GeoPathModel() {
    }
    
    /**
     * Deserialize this object.
     */
    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException {
        // read the serializable part of this GeoPath.
        stream.defaultReadObject();
        
        // compute bounds of path. this.bounds is not serialized.
        this.updateBounds();
    }
    
    /**
     * Create and return a copy of this path.
     */
    @Override
    public Object clone() {
        GeoPathModel copy = new GeoPathModel();
        
        final int instructionsCount = this.instructions.length;
        final byte[] instructionsCopy = new byte[instructionsCount];
        System.arraycopy(this.instructions, 0, instructionsCopy, 0, instructionsCount);
        copy.instructions = instructionsCopy;
        
        final int coordinatesCount = this.points.length;
        final double[] pointsCopy = new double[coordinatesCount];
        System.arraycopy(this.points, 0, pointsCopy, 0, coordinatesCount);
        copy.points = pointsCopy;
        
        if (this.bounds != null)
            copy.bounds = (Rectangle2D)this.bounds.clone();
    
        return copy;
    }
    
    /**
     * Convert a cubic bezier spline to an array of straight lines.
     * @param path Destination for straight line segments.
     * @param tol The maximum tolerable deviation from the bezier spline.
     */
    public static void bezierToStraightLines( GeoPathModel path,
            double x1, double y1,
            double x2, double y2,
            double x3, double y3,
            double x4, double y4,
            double tol) {
        
        final double	t = 0.5;
        
        if (GeometryUtils.isStraightLine(x1, y1, x2, y2, x3, y3, x4, y4, tol)) {
            // don't add new point if start and end point coincide.
            if (x1 != x4 || y1 != y4)
                path.lineTo(x4, y4);
        } else {   // divide curve by factor of two
            
            final double hx = t * (x2 + x3);
            final double hy = t * (y2 + y3);
            final double l2x = t * (x1 + x2);
            final double l2y = t * (y1 + y2);
            final double l3x = t * (l2x + hx);
            final double l3y = t * (l2y + hy);
            final double r3x = t * (x3 + x4);
            final double r3y = t * (y3 + y4);
            final double r2x = t * (hx + r3x);
            final double r2y = t * (hy + r3y);
            final double lrx = t * (l3x + r2x);
            final double lry = t * (l3y + r2y);
            
            // left part of curve
            bezierToStraightLines(path, x1, y1, l2x, l2y, l3x, l3y, lrx, lry, tol);
            
            // right part of curve
            bezierToStraightLines(path, lrx, lry, r2x, r2y, r3x, r3y, x4, y4, tol);
        }
        
    }
    
    /**
     * Adds the passed drawing instruction to this.instructions. This method does
     * not alter any other variable of this object.
     * IMPORTANT: It is not guaranteed that the passed drawing instruction is 
     * added to this.instructions. 
     * @param instruction The drawing instruction to add.
     */
    private void pushDrawingInstruction(byte instruction) {
        final int instructionsCount = this.instructions.length;
        final byte[] newInstructions = new byte[instructionsCount + 1];
        System.arraycopy(this.instructions, 0, newInstructions, 0, instructionsCount);
        newInstructions[instructionsCount] = instruction;
        this.instructions = newInstructions;
    }
    
    /**
     * Removes the last drawing instruction from this.instructions. This method 
     * does not alter any other variable of this object.
     */
    private void popDrawingInstruction() {
        final int instructionsCount = this.instructions.length;
        if (instructionsCount == 0)
            return;
        final byte[] newInstructions = new byte[instructionsCount - 1];
        System.arraycopy(this.instructions, 0, newInstructions, 0, instructionsCount-1);
        this.instructions = newInstructions;
    }
    
    /**
     * Add a drawing instruction that uses a single point as argument (lineto or
     * moveto).
     * @param x The horizontal coordinate of the point.
     * @param y The horizontal coordinate of the point.
     * @param drawingInstruction The instruction to add.
     */
    private void add(double x, double y, byte drawingInstruction) {
        
        // add point
        final int coordinatesCount = this.points.length;
        final double[] newPoints = new double[coordinatesCount + 2];
        System.arraycopy(this.points, 0, newPoints, 0, coordinatesCount);
        newPoints[coordinatesCount] = x;
        newPoints[coordinatesCount + 1] = y;
        this.points = newPoints;
        
        // update bounding box
        this.includeLastPointInBoundingBox();
        
        // add drawing instruction
        this.pushDrawingInstruction(drawingInstruction);
    }
    
    /**
     * Removes count x-y point pairs from the points array.
     */
    private void removePoints(int count) {
        final int coordinatesCount = this.points.length;
        final double[] newPoints = new double[coordinatesCount - count * 2];
        System.arraycopy(this.points, 0, newPoints, 0, newPoints.length);
        this.points = newPoints;
        
        this.updateBounds();
    }
    
    /**
     * Makes sure the last point added to this path is included in the bounding
     * box. If it is not, the bounding box is altered accordingly.
     */
    private void includeLastPointInBoundingBox() {
        final int coordinatesCount = points.length;
        final double x = this.points[coordinatesCount-2];
        final double y = this.points[coordinatesCount-1];
        if (this.bounds == null) {
            this.bounds = new Rectangle2D.Double(x, y, 0, 0);
            return;
        }
        
        // enlarge the bounds rectangle if the new point lays ouside its 
        // current extension.
        this.bounds.add(x, y);
    }
    
    /**
     *  Compute the bounding box of this path and store it in this.bounds.
     */
    protected void updateBounds () {
        
        final int coordinatesCount = this.points.length;
        if (coordinatesCount == 0) {
            this.bounds = null;
            return;
        }
        
        double minX = points[0];
        double maxX = points[0];
        double minY = points[1];
        double maxY = points[1];
        
        for (int i = 2; i < coordinatesCount; i+=2) {
            final double x = points[i];
            final double y = points[i+1];
            if (x < minX)
                minX = x;
            else if (x > maxX)
                maxX = x;
            if (y < minY)
                minY = y;
            else if (y > maxY)
                maxY = y;
        }
        
        this.bounds = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }
    
    /**
     * Removes the last drawing instruction of the path that was added with 
     * moveto, lineto, etc.
     */
    public void removeLastInstruction() {
        final int instructionsCount = this.instructions.length;
        if (instructionsCount == 0)
            return;
        
        switch (this.instructions[instructionsCount-1]) {
            case CLOSE:
                break;
            case MOVETO:
            case LINETO:
                this.removePoints(1);
                break;
            case QUADCURVETO:
                this.removePoints(2);
                break;
            case CURVETO:
                this.removePoints(3);
                break;
        }
        this.popDrawingInstruction();
    }
    
    /**
     * Removes all currently stored drawing instructions and all points.
     */
    public void reset() {
        this.points = new double[0];
        this.instructions = new byte[0];
        this.bounds = null;
    }
    
    /**
     * Removes all currently stored drawing instructions and points, and 
     * replaces them with the drawing stored in the passed PathIterator.
     * @param pathIterator Contains the new geometry of the path.
     */
    public void reset(PathIterator pathIterator) {
        this.reset();
        this.append(pathIterator);
    }
     
    /**
     * Append a move-to command to the current path. Places the virtual pen at
     * the specified location without drawing any line.
     * @param x The location to move to.
     * @param y The location to move to.
     */
    public void moveTo(double x, double y) {
        final int instructionsCount = this.instructions.length;
        if (instructionsCount > 0 
                && this.instructions[instructionsCount - 1] == MOVETO) {
            this.removeLastInstruction();
        }
        
        this.add(x, y, MOVETO);
    }
    
    /**
     * Append a move-to command to the current path. Places the virtual pen at the
     * specified location without drawing any line.
     * @param xy An array containing the x and the y coordinate.
     */
    public void moveTo(double[] xy) {
        this.moveTo(xy[0], xy[1]);
    }
    
    /**
     * Draws a line from the current location of the pen to the specified location. Before
     * calling lineTo, moveTo must be called. Alternatively, use moveOrLineTo that makes
     * sure moveTo is called before lineTo (or quadTo, resp. curveTo).
     * @param x The end point of the new line.
     * @param y The end point of the new line.
     */
    public void lineTo(double x, double y) {
        this.add(x, y, LINETO);
    }
    
    /**
     * Draws a line from the current location of the pen to the specified location. Before
     * calling lineTo, moveTo must be called. Alternatively, use moveOrLineTo that makes
     * sure moveTo is called before lineTo (or quadTo, resp. curveTo).
     * @param xy An array containing the x and the y coordinate.
     */
    public void lineTo(double[] xy) {
        this.add(xy[0], xy[1], LINETO);
    }
    
    /**
     * Moves the virtual pen to the specified location if this is the first call that
     * changes the geometry. If this is not the first geometry changing call, a straight
     * line is drawn to the specified location.
     * @param x The end point of the new line, or the location to move to.
     * @param y The end point of the new line, or the location to move to.
     */
    public void moveOrLineTo(double x, double y) {
        if (this.points.length == 0)
            this.moveTo(x, y);
        else
            this.add(x, y, LINETO);
    }
    
    /**
     * Appends a quadratic bezier curve.
     * @param x1 The location of the control point that is not on the curve.
     * @param y1 The location of the control point that is not on the curve.
     * @param x2 The location of the end point of the new curve.
     * @param y2 The location of the control point that is not on the curve.
     */
    public void quadTo(double x1, double y1, double x2, double y2) {
        // add two points
        int coordinatesCount = this.points.length;
        final double[] expandedPoints = new double[coordinatesCount + 4];
        System.arraycopy(this.points, 0, expandedPoints, 0, coordinatesCount);
        expandedPoints[coordinatesCount] = x1;
        expandedPoints[++coordinatesCount] = y1;
        expandedPoints[++coordinatesCount] = x2;
        expandedPoints[++coordinatesCount] = y2;
        this.points = expandedPoints;
        
        // update bounding box
        this.includeLastPointInBoundingBox();
        this.includeLastPointInBoundingBox();
        
        // add drawing instruction
        this.pushDrawingInstruction(QUADCURVETO);
    }
    
    /**
     * Appends a cubic bezier curve.
     * @param x1 The location of the first control point that is not on the curve.
     * @param y1 The location of the first control point that is not on the curve.
     * @param x2 The location of the second control point that is not on the curve.
     * @param y2 The location of the second control point that is not on the curve.
     * @param x3 The location of the end point of the new curve.
     * @param y3 The location of the end point of the new curve.
     */
    public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
        
        // add three points
        int coordinatesCount = this.points.length;
        final double[] expandedPoints = new double[coordinatesCount + 6];
        System.arraycopy(this.points, 0, expandedPoints, 0, coordinatesCount);
        expandedPoints[coordinatesCount] = x1;
        expandedPoints[++coordinatesCount] = y1;
        expandedPoints[++coordinatesCount] = x2;
        expandedPoints[++coordinatesCount] = y2;
        expandedPoints[++coordinatesCount] = x3;
        expandedPoints[++coordinatesCount] = y3;
        this.points = expandedPoints;
        
        // update bounding box
        this.includeLastPointInBoundingBox();
        this.includeLastPointInBoundingBox();
        this.includeLastPointInBoundingBox();
        
        // add drawing instruction
        this.pushDrawingInstruction(CURVETO);
    }
    
    /**
     * Closes the path by connecting the last point with the first point using a
     * straight line. 
     */
    public void closePath() {
        if (this.instructions.length > 0)
            this.pushDrawingInstruction(CLOSE);
    }
    
    /**
     * Returns true if any of the sub-paths is closed.
     * @return True if the path is closed.
     */
    public boolean isClosed() {
        final int instructionsCount = this.instructions.length;
        for (int i = 0; i < instructionsCount; i++) {
            if (this.instructions[i] == CLOSE)
                return true;
        }
        return false;
    }
    
    public void append(GeoPathModel pathModel, boolean connect) {
        if (pathModel.getDrawingInstructionCount() == 0)
            return;
        
        final int instructionsCount = pathModel.instructions.length;
        int pt = 0;
        for (int i = 0; i < instructionsCount; i++) {
            switch (pathModel.instructions[i]) {
                case MOVETO:
                    if (i == 0 && connect)
                        this.lineTo(pathModel.points[pt++], pathModel.points[pt++]);
                    else
                        this.moveTo(pathModel.points[pt++], pathModel.points[pt++]);
                    break;
                case LINETO:
                    this.lineTo(pathModel.points[pt++], pathModel.points[pt++]);
                    break;
                case CLOSE:
                    this.closePath();
                    break;
                case QUADCURVETO:
                    this.quadTo(pathModel.points[pt++], pathModel.points[pt++],
                            pathModel.points[pt++], pathModel.points[pt++]);
                    break;
                case CURVETO:
                    this.curveTo(pathModel.points[pt++], pathModel.points[pt++],
                            pathModel.points[pt++], pathModel.points[pt++], 
                            pathModel.points[pt++], pathModel.points[pt++]);
                    break;
            }
        }
    }
    
    public void append(PathIterator pathIterator) {
        double coords[] = new double [6];
        while (!pathIterator.isDone()) {
            switch (pathIterator.currentSegment(coords)) {
                case PathIterator.SEG_CLOSE:
                    this.closePath();
                    break;
                case PathIterator.SEG_LINETO:
                    this.lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_MOVETO:
                    this.moveTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    this.quadTo(coords[0], coords[1],
                            coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    this.curveTo(coords[0], coords[1],
                            coords[2], coords[3],
                            coords[4], coords[5]);
                    break;
                
            }
            pathIterator.next();
        }
        this.updateBounds();
    }
    
    /**
     * Returns true if this GeoPath consists of more than one line or polygon.
     * @return True if this is a compound path.
     */
    public boolean isCompound() {
        final int instructionsCount = this.instructions.length;
        if (instructionsCount == 0)
            return false;
        for (int i = 1; i < instructionsCount; i++) {
            if (this.instructions[i] == MOVETO) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the number of compound sub-paths.
     * @return The number of sub-paths. Returns 0 if this path does not contain
     * any instruction.
     */
    public int getCompoundCount() {
        final int instructionsCount = this.instructions.length;
        if (instructionsCount == 0)
            return 0;
        int compoundCount = 0;
        for (int i = 0; i < instructionsCount; i++) {
            if (this.instructions[i] == MOVETO) {
                ++compoundCount;
            }
        }
        return compoundCount;
    }
    
    /**
     * Returns true if the passed point is inside this path.
     * @param x
     * @param y
     * @return
     */
    public boolean contains(double x, double y) {
        if (bounds.contains(x, y) == false) {
            return false;
        }
        return toPath().contains(x, y);
    }
    
    /**
     * Returns the bounding box of this path.
     * @return The bounding box around all points of this path. May be null if 
     * the path does not contain any drawing instructions. The width and / or
     * height may be 0.
     */
    public Rectangle2D getBounds2D() {
        return bounds;
    }
    
    /**
     * Returns the number of drawing instructions.
     * @return The number of instructions.
     */
    public int getDrawingInstructionCount() {
        return this.instructions.length;
    }
    
    /**
     * Returns the last drawing instruction.
     * @return The drawing command.
     */
    public byte getLastInstruction() {
        if (this.instructions.length == 0)
            return GeoPathModel.NONE;
        return this.instructions[this.instructions.length-1];
    }
    
    public Point2D getLastMoveTo() {
        
        if (this.getPointsCount() < 1)
            return null;
        
        GeoPathIterator iterator = this.getIterator();
        Point2D pt = new Point2D.Double();
        while(iterator.next()) {
            if (iterator.getInstruction() == GeoPathModel.MOVETO) {
                pt.setLocation(iterator.getX(), iterator.getY());
            }
        }
        
        return pt;
    }
    
    public Point2D getStartPoint() {
        if (this.points.length == 0)
            return null;
        final double x = this.points[0];
        final double y = this.points[1];
        return new Point2D.Double(x, y);
    }

    public Point2D getEndPoint() {
        if (this.points.length == 0)
            return null;
        final double x = this.points[this.points.length-2];
        final double y = this.points[this.points.length-1];
        return new Point2D.Double(x, y);
    }

    public int getPointsCount() {
        return this.points.length / 2;
    }
    
    /**
     * Scale this path by a factor relative to a passed origin.
     * @param scale Scale factor.
     * @param cx The x coordinate of the point relative to which the object is scaled.
     * @param cy The y coordinate of the point relative to which the object is scaled.
     */
    public void scale(double scale, double cx, double cy) {
        final int pointsCount = this.points.length / 2;
        if (pointsCount == 0) {
            this.bounds = null;
            return;
        }
        
        for (int i = 0; i < pointsCount; i++) {
            final double x = points[2*i];
            final double y = points[2*i+1];
            points[2*i] = (x - cx) * scale + cx;
            points[2*i+1] = (y - cy) * scale + cy;
        }
        
        this.updateBounds();
    }
    
    /**
     * Apply an affine transformation on this path.
     * @param affineTransform The transformation to apply.
     */
    public void transform(AffineTransform affineTransform) {
        affineTransform.transform(points, 0, points, 0, points.length / 2);
        this.updateBounds();
    }
    
    /**
     * Returns the signed area enclosed by a GeoPath. The last point and the first 
     * point are connected.
     * Does not work with bezier curves and polygons with islands and holes !!! ???
     */
    public double getSignedArea() {
        double area = 0.;
        
        // convert this Path if it's (partially) built of bezier curves
        // GeoPath path = onlyStraightLines() ? this : convertToStraightLines();
        // this.bezierToStraightLines()
        
        final int nbrPoints = this.getPointsCount();
        if (nbrPoints < 3)
            return 0;
        
        GeoPathIterator pIter = this.getIterator();
        double x0 = pIter.getX();
        double y0 = pIter.getY();
        double x1 = x0;
        double y1 = y0;
        
        while (pIter.next() && pIter.getInstruction() == LINETO) {
            final double x2 = pIter.getX();
            final double y2 = pIter.getY();
            area += x1 * y2 - x2 * y1;
            x1 = x2;
            y1 = y2;
        }
        area += x1 * y0 - x0 * y1;
        return area * 0.5;
    }
    
    /**
     * Returns the area enclosed by a GeoPath. The last point and the first 
     * point are connected.
     * Does not work with bezier curves !!! ???
     */
    public double getArea() {
        return Math.abs(getSignedArea());
    }
    
    public Path2D.Double toPath() {
        Path2D.Double path = new Path2D.Double();
        final int instructionsCount = instructions.length;
        int ptID = 0;
        for (int i = 0; i < instructionsCount; i++) {
            switch (instructions[i]) {
                case MOVETO:
                    path.moveTo(points[ptID++], points[ptID++]);
                    break;
                case LINETO:
                    path.lineTo(points[ptID++], points[ptID++]);
                    break;
                case CLOSE:
                    path.closePath();
                    break;
                case QUADCURVETO:
                    path.quadTo(points[ptID++], points[ptID++],
                            points[ptID++], points[ptID++]);
                    break;
                case CURVETO:
                    path.curveTo(points[ptID++], points[ptID++],
                            points[ptID++], points[ptID++], 
                            points[ptID++], points[ptID++]);
                    break;
            }
        }
        return path;
    }
    
    PathIterator toPathIterator(AffineTransform affineTransform) {
        return toPath().getPathIterator(affineTransform);
    }
    
    PathIterator toPathIterator(AffineTransform affineTransform, double flatness) {
        return toPath().getPathIterator(affineTransform, flatness);
    }
    
    @Override
    public String toString() {
        
        StringBuffer str = new StringBuffer();
        final int instructionsCount = this.instructions.length;
        int ptID = 0;
        for (int i = 0; i < instructionsCount; i++) {
            switch (this.instructions[i]) {
                case MOVETO:
                    str.append("moveto " + this.points[ptID++] + " " +  
                            this.points[ptID++] + "\n");
                    break;
                case LINETO:
                    str.append("lineto " + this.points[ptID++] + " " +  
                            this.points[ptID++] + "\n");
                    break;
                case CLOSE:
                    str.append("close\n");
                    break;
                case QUADCURVETO:
                    str.append("quad " + this.points[ptID++] + " " +  
                            this.points[ptID++] + " " +
                            this.points[ptID++] + " " +  
                            this.points[ptID++] + "\n");
                    break;
                case CURVETO:
                    str.append("cubic " + this.points[ptID++] + " " +  
                            this.points[ptID++] + " " +
                            this.points[ptID++] + " " +  
                            this.points[ptID++] + " " + 
                            this.points[ptID++] + " " +  
                            this.points[ptID++] + "\n");
                    break;
            }
        }
        return str.toString();
    }
    
    /**
     * Returns an iterator for this path. It is the caller's responsibility
     * to make sure that the GeoPathModel is not changed while a GeoPathIterator
     * is used.
     */
    public GeoPathIterator getIterator() {
        return new GeoPathIterator(this);
    }

}

