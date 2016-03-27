package ika.geo;

import java.awt.geom.*;
import java.awt.*;
import java.io.*;
import ika.utils.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * GeoPath - a class that models vector data. It can treat straight lines and
 * bezier curves.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class GeoPath extends GeoObject implements Serializable, Cloneable {

    static public GeoPath newRect(Rectangle2D bounds) {
        GeoPath geoPath = new GeoPath();
        geoPath.rectangle(bounds);
        return geoPath;
    }

    static public GeoPath newRect(float west, float south,
            float width, float height) {
        GeoPath geoPath = new GeoPath();
        geoPath.moveTo(west, south);
        geoPath.lineTo(west + width, south);
        geoPath.lineTo(west + width, south + height);
        geoPath.lineTo(west, south + height);
        geoPath.closePath();
        return geoPath;
    }

    static public GeoPath newCircle(float cx, float cy, float r) {
        GeoPath geoPath = new GeoPath();
        geoPath.circle(cx, cy, r);
        return geoPath;
    }

    static public GeoPath newSquare(float cx, float cy, float d) {
        GeoPath geoPath = new GeoPath();
        geoPath.square(cx, cy, d);
        return geoPath;
    }

    /**
     * Build a bezier GeoPath that approximates an arc of an ellipsoid.
     *
     * @param fromAngle counted in clock wise direction from north.
     * @param arcAngle counted in clock wise direction from north.
     */
    public static GeoPath newArc(double cx, double cy, double rx, double ry,
            double fromAngle, double arcAngle) {

        // arcAngle must be larger than kMinAngle.
        final double kMinAngle = 0.0001;

        // a segment of a path may not be larger than pi/4
        final double kMaxSegmentAngle = Math.PI / 4;

        double phi;		// half angle of current segment
        double remainingArc;	// rest of arc to convert into segments
        double arcSign;	// -1. or +1. Indicates direction of arcAngle.
        GeoPath path = null;	// new path.

        // arcAngle must have certain size.
        if (Math.abs(arcAngle) < kMinAngle) {
            return null;
        }

        // arcAngle may not be larger than 2*PI
        if (Math.abs(arcAngle) > Math.PI * 2) {
            arcAngle = Math.PI * 2;
        }

        arcSign = (arcAngle > 0.) ? 1. : -1.;
        remainingArc = arcAngle;
        path = new GeoPath();

        // split the arc and construct each segment
        final int nbrOfSegments = (int) Math.ceil(Math.abs(arcAngle / kMaxSegmentAngle));
        for (int i = 0; i < nbrOfSegments; i++) {
            if (Math.abs(remainingArc) > kMaxSegmentAngle) {
                phi = arcSign * kMaxSegmentAngle * 0.5;
                remainingArc -= arcSign * kMaxSegmentAngle;
            } else {
                phi = 0.5 * remainingArc;
            }
            final double cosPhi = Math.cos(phi);
            final double sinPhi = Math.sin(phi);
            final float c1x = (float) ((4. - cosPhi) / 3.);
            final float c1y = (float) ((1. - cosPhi) * (cosPhi - 3.) / (3. * sinPhi));

            // arc around x axis with radius = 1 at center x = 0 / y = 0.
            if (i == 0) {
                path.moveTo((float) cosPhi, (float) sinPhi);
            }
            path.curveTo(c1x, -c1y, c1x, c1y, (float) cosPhi, -(float) sinPhi);

            // rotate arc against the direction of arcAngle to add next segment at end of the path.
            if (i < nbrOfSegments - 2) {
                path.rotate(arcSign * kMaxSegmentAngle);
            } else if (i == nbrOfSegments - 2) // vorletztes Segment (letztes Segment muss nicht gedreht werden).
            {
                path.rotate(arcSign * (0.5 * kMaxSegmentAngle + Math.abs(0.5 * remainingArc)));
            }
        }

        // rotate finished arc
        double finalRot = -arcSign * (nbrOfSegments - 1) * kMaxSegmentAngle - 0.5 * arcSign * remainingArc - fromAngle + Math.PI * 0.5;
        path.rotate(finalRot);

        // scale from 1 to desired radius in x and y direction.
        path.scale(rx, ry);

        // center on cx, cy.
        path.move(cx, cy);

        return path;
    }

    private static final long serialVersionUID = 7350986432785586245L;

    /**
     * The geometry of this GeoPath.
     */
    private GeoPathModel path;

    /**
     * A VectorSymbol that stores the graphic attributes of this GeoPath.
     */
    private VectorSymbol symbol;

    /**
     * Creates a new instance of GeoPath
     */
    public GeoPath() {
        this.path = new GeoPathModel();
        this.symbol = new VectorSymbol();
    }

    protected GeoPath(GeoPath geoPath) {
        this.path = geoPath.path;
        this.symbol = geoPath.symbol;
    }

    @Override
    public GeoPath clone() {
        try {
            GeoPath geoPath = (GeoPath) super.clone();

            // make deep clone of the VectorSymbol and the path
            geoPath.symbol = (VectorSymbol) this.symbol.clone();
            geoPath.path = (GeoPathModel) this.path.clone();

            return geoPath;
        } catch (Exception exc) {
            return null;
        }
    }

    /**
     * Append a move-to command to the current path. Places the virtual pen at
     * the specified location without drawing any line.
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param x The location to move to.
     * @param y The location to move to.
     */
    public void moveTo(float x, float y) {
        path.moveTo(x, y);
    }

    /**
     * Append a move-to command to the current path. Places the virtual pen at
     * the specified location without drawing any line.
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param xy An array containing the x and the y coordinate.
     */
    public void moveTo(float[] xy) {
        path.moveTo(xy[0], xy[1]);
    }

    /**
     * Append a move-to command to the current path. Places the virtual pen at
     * the specified location without drawing any line.
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param point A point containing the x and the y coordinate.
     */
    public void moveTo(Point2D point) {
        final float x = (float) point.getX();
        final float y = (float) point.getY();
        path.moveTo(x, y);
    }

    /**
     * Draws a line from the current location of the pen to the specified
     * location. Before calling lineTo, moveTo must be called. Alternatively,
     * use moveOrLineTo that makes sure moveTo is called before lineTo (or
     * quadTo, resp. curveTo).
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param x The end point of the new line segment.
     * @param y The end point of the new line segment.
     */
    public void lineTo(float x, float y) {
        path.lineTo(x, y);
    }

    /**
     * Draws a line from the current location of the pen to the specified
     * location. Before calling lineTo, moveTo must be called. Alternatively,
     * use moveOrLineTo that makes sure moveTo is called before lineTo (or
     * quadTo, resp. curveTo).
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param xy An array containing the x and the y coordinate.
     */
    public void lineTo(float[] xy) {
        path.lineTo(xy[0], xy[1]);
    }

    /**
     * Draws a line from the current location of the pen to the specified
     * location. Before calling lineTo, moveTo must be called. Alternatively,
     * use moveOrLineTo that makes sure moveTo is called before lineTo (or
     * quadTo, resp. curveTo).
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param point A point containing the x and the y coordinate.
     */
    public void lineTo(Point2D point) {
        final float x = (float) point.getX();
        final float y = (float) point.getY();
        path.lineTo(x, y);
    }

    /**
     * Moves the virtual pen to the specified location if this is the first call
     * that changes the geometry. If this is not the first geometry changing
     * call, a straight line is drawn to the specified location.
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param x The end point of the new line segment, or the location to move
     * to.
     * @param y The end point of the new line segment, or the location to move
     * to.
     */
    public void moveOrLineTo(float x, float y) {
        if (this.hasOneOrMorePoints()) {
            path.lineTo(x, y);
        } else {
            path.moveTo(x, y);
        }
    }

    /**
     * Appends a quadratic bezier curve to this GeoPath.
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param x1 The location of the control point that is not on the curve.
     * @param y1 The location of the control point that is not on the curve.
     * @param x2 The location of the end point of the new curve segment.
     * @param y2 The location of the control point that is not on the curve.
     */
    public void quadTo(float x1, float y1, float x2, float y2) {
        path.quadTo(x1, y1, x2, y2);
    }

    /**
     * Appends a cubic bezier curve to this GeoPath.
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param x1 The location of the first control point that is not on the
     * curve.
     * @param y1 The location of the first control point that is not on the
     * curve.
     * @param x2 The location of the second control point that is not on the
     * curve.
     * @param y2 The location of the second control point that is not on the
     * curve.
     * @param x3 The location of the end point of the new curve segment.
     * @param y3 The location of the end point of the new curve segment.
     */
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        path.curveTo(x1, y1, x2, y2, x3, y3);
    }

    /**
     * Appends a cubic bezier curve to this GeoPath.
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     *
     * @param ctrl1 The location of the first control point that is not on the
     * curve.
     * @param ctrl2 The location of the second control point that is not on the
     * curve.
     * @param end The location of the end point of the new curve segment.
     */
    public void curveTo(Point2D ctrl1, Point2D ctrl2, Point2D end) {
        final float ctrl1x = (float) ctrl1.getX();
        final float ctrl1y = (float) ctrl1.getY();
        final float ctrl2x = (float) ctrl2.getX();
        final float ctrl2y = (float) ctrl2.getY();
        final float endx = (float) end.getX();
        final float endy = (float) end.getY();

        path.curveTo(ctrl1x, ctrl1y, ctrl2x, ctrl2y, endx, endy);
    }

    /**
     * Closes the path by connecting the last point with the first point using a
     * straight line.
     * <B>Important: A call to this method does not generate a MapEvent!</B>
     */
    public void closePath() {
        path.closePath();
    }

    /**
     * Returns true if any of the possible sub-paths is closed.
     *
     * @return True if the path is closed.
     */
    public boolean isClosed() {
        PathIterator pi = this.toPathIterator(null);
        double[] coords = new double[6];
        while (pi.isDone() == false) {
            if (pi.currentSegment(coords) == PathIterator.SEG_CLOSE) {
                return true;
            }
            pi.next();
        }
        return false;
    }

    /**
     * Returns true if this GeoPath consists of more than one line or polygon.
     *
     * @return True if this is a compound path.
     */
    public boolean isCompound() {
        return this.path.isCompound();
    }

    /**
     * Returns the number of compound sub-paths.
     *
     * @return The number of sub-paths. Returns 0 if this path does not contain
     * any instruction.
     */
    public int getCompoundCount() {
        return this.path.getCompoundCount();
    }

    /**
     * Constructs a path from a series of points that will be connected by
     * straight lines.
     *
     * @param points The points to connect.
     */
    public void straightLines(Point2D[] points) {
        MapEventTrigger trigger = new MapEventTrigger(this);
        try {
            path.reset();
            if (points.length >= 1) {
                path.moveTo((float) points[0].getX(), (float) points[0].getY());
                for (int i = 1; i < points.length; i++) {
                    path.lineTo((float) points[i].getX(), (float) points[i].getY());
                }
            }
        } finally {
            trigger.inform();
        }
    }

    /**
     * Constructs a path from a series of points that will be connected by
     * straight lines.
     *
     * @param points The points to connect.
     * @param firstPoint The id of the first point in the array.
     * @nbrPoints The number of point to use.
     */
    public void straightLines(double[][] points, int firstPoint, int nbrPoints) {
        MapEventTrigger trigger = new MapEventTrigger(this);
        try {
            path.reset();
            if (points.length >= 1) {
                final int lastPoint = firstPoint + nbrPoints;
                path.moveTo((float) points[firstPoint][0], (float) points[firstPoint][1]);
                for (int i = firstPoint + 1; i < lastPoint; i++) {
                    path.lineTo((float) points[i][0], (float) points[i][1]);
                }
            }
        } finally {
            trigger.inform();
        }
    }

    /**
     * Constructs a path from a series of points that will be connected by
     * straight lines.
     *
     * @param param points The points to connect.
     */
    public void straightLines(double[] points) {
        MapEventTrigger trigger = new MapEventTrigger(this);
        try {
            path.reset();
            if (points.length >= 1) {
                path.moveTo((float) points[0], (float) points[1]);
                for (int i = 1; i < points.length / 2; i++) {
                    path.lineTo((float) points[i * 2], (float) points[i * 2 + 1]);
                }
            }
        } finally {
            trigger.inform();
        }
    }

    /**
     * Constructs a bezier control point for two straight lines that meet in a
     * point. The control point lies in backward direction from point 1 towards
     * point 0.
     */
    private void bezierPoint(
            double p0x, double p0y,
            double p1x, double p1y,
            double p2x, double p2y,
            float[] controlPoint,
            double smoothness) {

        final double F = 0.39;

        // length of the line connecting the previous point P0 with the current
        // point P1.
        final double length = GeometryUtils.length(p1x, p1y, p2x, p2y);

        // unary vector from P1 to P0.
        double dx1 = p0x - p1x;
        double dy1 = p0y - p1y;
        final double l1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        dx1 /= l1;
        dy1 /= l1;
        if (Double.isNaN(dx1) || Double.isNaN(dy1)
                || Double.isInfinite(dx1) || Double.isInfinite(dy1)) {
            controlPoint[0] = (float) p0x;
            controlPoint[1] = (float) p1y;
            return;
        }

        // unary vector from P2 to P1.
        double dx2 = p1x - p2x;
        double dy2 = p1y - p2y;
        final double l2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
        dx2 /= l2;
        dy2 /= l2;

        // direction of tangent where bezier control point lies on.
        double tx = dx1 + dx2;
        double ty = dy1 + dy2;
        final double l = Math.sqrt(tx * tx + ty * ty);
        tx /= l;
        ty /= l;

        // first control point
        controlPoint[0] = (float) (p1x - length * F * smoothness * tx);
        controlPoint[1] = (float) (p1y - length * F * smoothness * ty);
    }

    /**
     *
     */
    public void smooth(double smoothness, double[][] points,
            int firstPoint, int nbrPoints) {

        MapEventTrigger trigger = new MapEventTrigger(this);
        try {
            if (smoothness <= 0. || MathUtils.numbersAreClose(0., smoothness)) {
                straightLines(points, firstPoint, nbrPoints);
                return;
            }

            final double F = 0.39;
            final int lastPoint = firstPoint + nbrPoints;

            if (points[0].length < 2) {
                throw new IllegalArgumentException();
            }

            path.reset();

            final boolean closePath = MathUtils.numbersAreClose(
                    points[firstPoint][0], points[lastPoint - 1][0])
                    && MathUtils.numbersAreClose(
                            points[firstPoint][1], points[lastPoint - 1][1]);

            double prevX = points[firstPoint][0];
            double prevY = points[firstPoint][1];

            float[] ctrlP1 = new float[2];
            float[] ctrlP2 = new float[2];

            // move to first point
            path.moveTo((float) points[firstPoint][0], (float) points[firstPoint][1]);

            for (int i = firstPoint + 1; i < lastPoint - 1; i++) {

                // previous point P0
                final double x0 = points[i - 1][0];
                final double y0 = points[i - 1][1];

                // current point P1
                final double x1 = points[i][0];
                final double y1 = points[i][1];

                // next point P2
                final double x2 = points[i + 1][0];
                final double y2 = points[i + 1][1];

                bezierPoint(prevX, prevY, x0, y0, x1, y1, ctrlP1, smoothness);
                bezierPoint(x2, y2, x1, y1, x0, y0, ctrlP2, smoothness);

                // add a bezier line segment to the path
                path.curveTo(ctrlP1[0], ctrlP1[1], ctrlP2[0], ctrlP2[1], (float) x1, (float) y1);
                prevX = x0;
                prevY = y0;
            }

            final double x0 = points[lastPoint - 1][0];
            final double y0 = points[lastPoint - 1][1];
            bezierPoint(x0, y0, x0, y0, prevX, prevY, ctrlP1, smoothness);
            path.curveTo(ctrlP1[0], ctrlP1[1], (float) x0, (float) y0, (float) x0, (float) y0);
        } finally {
            trigger.inform();
        }
    }

    /**
     * Creates a circle. Replaces the current geometry.
     *
     * @param cx The horizontal coordinate of the center.
     * @param cy The vertical coordinate of the center.
     * @param r The radius of the circle.
     */
    public void circle(float cx, float cy, float r) {
        // Build a Bezier path that approximates a full circle.
        // Based on an web-article by G. Adam Stanislav:
        // "Drawing a circle with Bezier Curves"
        if (r <= 0.f) {
            return; // throw new IllegalArgumentException();
        }
        MapEventTrigger trigger = new MapEventTrigger(this);
        try {
            this.reset();

            final float kappa = (float) ((Math.sqrt(2.) - 1.) * 4. / 3.);
            final float l = r * kappa;

            // move to top center
            this.moveTo(cx, cy + r);
            // I. quadrant
            this.curveTo(cx + l, cy + r, cx + r, cy + l, cx + r, cy);
            // II. quadrant
            this.curveTo(cx + r, cy - l, cx + l, cy - r, cx, cy - r);
            // III. quadrant
            this.curveTo(cx - l, cy - r, cx - r, cy - l, cx - r, cy);
            // IV. quadrant
            this.curveTo(cx - r, cy + l, cx - l, cy + r, cx, cy + r);

            this.closePath();
        } finally {
            trigger.inform();
        }
    }

    /**
     * Creates a square. Replaces the current geometry.
     *
     * @param cx The horizontal coordinate of the center.
     * @param cy The vertical coordinate of the center.
     * @param r The length of one side of the square.
     */
    public void square(float cx, float cy, float d) {
        if (d <= 0.f) {
            throw new IllegalArgumentException();
        }

        MapEventTrigger trigger = new MapEventTrigger(this);
        try {
            this.reset();

            float d_2 = d / 2f;
            this.moveTo(cx - d_2, cy - d_2);
            this.lineTo(cx + d_2, cy - d_2);
            this.lineTo(cx + d_2, cy + d_2);
            this.lineTo(cx - d_2, cy + d_2);
            this.closePath();
        } finally {
            trigger.inform();
        }
    }

    /**
     * Creates a rectangle. Replaces the current geometry.
     *
     * @param rect The geometry describing the rectangle.
     */
    public void rectangle(Rectangle2D rect) {
        if (rect == null) {
            throw new IllegalArgumentException();
        }

        MapEventTrigger trigger = new MapEventTrigger(this);
        try {
            this.reset();

            final float xMin = (float) rect.getMinX();
            final float xMax = (float) rect.getMaxX();
            final float yMin = (float) rect.getMinY();
            final float yMax = (float) rect.getMaxY();
            this.moveTo(xMin, yMin);
            this.lineTo(xMax, yMin);
            this.lineTo(xMax, yMax);
            this.lineTo(xMin, yMax);
            this.closePath();
        } finally {
            trigger.inform();
        }
    }

    public void reset() {
        path.reset();
        MapEventTrigger.inform(this);
    }

    public void setPathModel(GeoPathModel path) {
        this.path = path;
        MapEventTrigger.inform(this);
    }

    /**
     * Removes the last point of the path that was added with moveto, lineto,
     * etc.
     */
    public void removeLastPoint() {
        this.path.removeLastInstruction();
        MapEventTrigger.inform(this);
    }

    /**
     * Appends the geometry contained in a GeoPath to this GeoPath.
     *
     * @param geoPath The GeoPath to append.
     * @param connect If true, the currently existing geometry is connected with
     * the new geometry.
     */
    public void append(GeoPath geoPath, boolean connect) {
        if (geoPath != null) {
            path.append(geoPath.path, connect);
            MapEventTrigger.inform(this);
        }
    }

    /**
     * Appends the geometry contained by a Shape object to this GeoPath.
     *
     * @param s The Shape to append.
     * @param connect If true, the currently existing geometry is connected with
     * the new geometry.
     */
    public void append(java.awt.Shape s, boolean connect) {
        GeoPathModel pm = new GeoPathModel();
        pm.reset(s.getPathIterator(null));
        path.append(pm, connect);
        MapEventTrigger.inform(this);
    }

    private class PathSegment {

        public float[] coords;
        int id;
    }

    /**
     * Inverts the order of points in a line.
     * <B>Only for straight open lines!</B>
     */
    public void invertDirection() {
        MapEventTrigger trigger = new MapEventTrigger(this);
        try {
            PathIterator pathIterator = this.toPathIterator(null);
            if (pathIterator == null) {
                return;
            }

            java.util.Vector segments = new java.util.Vector();

            while (!pathIterator.isDone()) {
                PathSegment ps = new PathSegment();
                ps.coords = new float[6];
                ps.id = pathIterator.currentSegment(ps.coords);
                segments.add(ps);
                pathIterator.next();
            }

            if (segments.size() == 0) {
                return;
            }

            this.path.reset();

            PathSegment ps = (PathSegment) (segments.get(segments.size() - 1));
            this.path.moveTo(ps.coords[0], ps.coords[1]);

            boolean firstMove = false;
            for (int i = segments.size() - 2; i > 0; --i) {
                ps = (PathSegment) (segments.get(i));
                switch (ps.id) {
                    case PathIterator.SEG_MOVETO:
                        this.path.moveTo(ps.coords[0], ps.coords[1]);
                        break;

                    case PathIterator.SEG_LINETO:
                        this.path.lineTo(ps.coords[0], ps.coords[1]);
                        break;

                    /*
                 case PathIterator.SEG_QUADTO:
                    this.path.quadTo(ps.coords[0], ps.coords[1], ps.coords[2], ps.coords[3]);
                    break;
                 
                case PathIterator.SEG_CUBICTO:
                    this.path.curveTo(ps.coords[0], ps.coords[1], ps.coords[2], ps.coords[3],
                            ps.coords[4], ps.coords[5]);
                    break;
                 
                 
                case PathIterator.SEG_CLOSE:
                    this.path.closePath();
                    break;
                     */
                }
            }
            // treat initial moveto
            ps = (PathSegment) (segments.get(0));
            this.path.lineTo(ps.coords[0], ps.coords[1]);
        } finally {
            trigger.inform();
        }
    }

    /**
     * Returns true if this GeoPath contains at least one point.
     *
     * @return True if number of points > 0, false otherwise.
     */
    public boolean hasOneOrMorePoints() {
        return path.getDrawingInstructionCount() > 0;
    }

    public int getPointsCount() {
        return path.getPointsCount();
    }

    /**
     * Returns the number of drawing instructions that build this GeoPath.
     *
     * @return The number of instructions.
     */
    public int getDrawingInstructionCount() {
        return path.getDrawingInstructionCount();
    }

    public byte getLastDrawingInstruction() {
        return path.getLastInstruction();
    }

    public Point2D getStartPoint() {
        return path.getStartPoint();
    }

    public Point2D getEndPoint() {
        return path.getEndPoint();
    }

    /**
     * Returns a reference on the vector symbol that stores the graphic
     * attributes used to draw this GeoPath.
     *
     * @return The VectorSymbol used to draw this GeoPath.
     */
    public VectorSymbol getVectorSymbol() {
        return symbol;
    }

    /**
     * Set the VectorSymbol that stores the graphic attributes used to draw this
     * GeoPath. The VectorSymbol is not copied, but simply a reference to it is
     * retained.
     *
     * @param symbol The new VectorSymbol.
     */
    public void setVectorSymbol(VectorSymbol symbol) {
        this.symbol = symbol;
        MapEventTrigger.inform(this);
    }

    /**
     * Returns a PathIterator that can be used to draw this GeoPath or iterate
     * over its geometry.
     *
     * @param affineTransform An AffineTransform to apply before the
     * PathIterator is returned.
     * @return The PathIterator.
     */
    public PathIterator toPathIterator(AffineTransform affineTransform) {
        return path.toPathIterator(affineTransform);
    }

    public Path2D.Double toPath() {
        return path.toPath();
    }
    
    /**
     * Returns a flattened PathIterator that can be used to draw this GeoPath or
     * iterate over its geometry. A flattened PathIterator does not contain any
     * quatratic or cubic bezier curve segments, but only straight lines.
     *
     * @param affineTransform An AffineTransform to apply before the
     * PathIterator is returned.
     * @param flatness The maximum deviation of the flattend geometry from the
     * original bezier geometry.
     * @return The PathIterator.
     */
    public PathIterator toPathIterator(AffineTransform affineTransform,
            double flatness) {
        return path.toPathIterator(affineTransform, flatness);
    }

    @Override
    public void drawNormalState(RenderParams rp) {

        final Graphics2D g2d = rp.g2d;
        final double scale = rp.scale;
        final Path2D p = path.toPath();

        // fill
        if (this.symbol != null && this.symbol.isFilled()) {
            g2d.setColor(this.symbol.getFillColor());
            g2d.fill(p);
        }

        // stroke
        if (this.symbol != null) {
            // apply the symbol attached to this path
            if (this.symbol.isStroked()) {
                g2d.setStroke(this.symbol.getStroke(scale));
                g2d.setColor(this.symbol.getStrokeColor());
                g2d.draw(p); // stroke it
            }
        } else {
            // there is no VectorSymbol present, use a default stroke.
            g2d.setStroke(new BasicStroke(0));
            g2d.setColor(Color.BLACK);
            g2d.draw(p); // stroke it
        }

    }

    public void drawSelectedState(RenderParams rp) {
        if (!this.isSelected()) {
            return;
        }
        // only stroke, no fill
        rp.g2d.draw(path.toPath());

    }

    public boolean isPointOnSymbol(java.awt.geom.Point2D point, double tolDist,
            double scale) {

        if (point == null) {
            return false;
        }

        /* First test if point is inside the bounding box.
         The rectangle has to be enlarged by tolDist, otherwise contains()
         returns false for a straight horizontal or vertical line. */
        Rectangle2D bounds = this.getBounds2D(scale);
        if (bounds == null) {
            return false;
        }
        bounds = (Rectangle2D) bounds.clone();

        GeometryUtils.enlargeRectangle(bounds, tolDist);
        if (bounds.contains(point) == false) {
            return false;
        }

        // if path is filled, test if point is inside path
        if (this.symbol.isFilled() && path.contains(point.getX(), point.getY())) {
            return true;
        }

        // test if distance to line is smaller than tolDist
        // create new path with straight lines only
        PathIterator pi = this.path.toPathIterator(null, tolDist / 2.);
        double x1 = 0;
        double y1 = 0;
        double lastMoveToX = 0;
        double lastMoveToY = 0;
        double[] coords = new double[6];
        int segmentType;
        while (pi.isDone() == false) {
            segmentType = pi.currentSegment(coords);
            switch (segmentType) {
                case PathIterator.SEG_CLOSE:
                    // SEG_CLOSE does not return any point.
                    coords[0] = lastMoveToX;
                    coords[1] = lastMoveToY;
                // fall thru, no break here

                case PathIterator.SEG_LINETO:
                    double d = Line2D.ptSegDistSq(x1, y1, coords[0], coords[1],
                            point.getX(), point.getY());
                    if (d < tolDist * tolDist) {
                        return true;
                    }
                    x1 = coords[0];
                    y1 = coords[1];
                    break;

                case PathIterator.SEG_MOVETO:
                    lastMoveToX = x1 = coords[0];
                    lastMoveToY = y1 = coords[1];
                    break;
            }
            pi.next();
        }
        return false;
    }

    public boolean contains(double x, double y) {
        return path.contains(x, y);
    }

    public boolean isIntersectedByRectangle(Rectangle2D rect, double scale) {

        // Test if the passed rectangle and the bounding box of this object
        // intersect.
        // Don't use Rectangle2D.intersects, but use 
        // GeometryUtils.rectanglesIntersect, which can handle rectangles with
        // an heigt or a width of 0.
        final Rectangle2D bounds = this.getBounds2D(scale);
        if (GeometryUtils.rectanglesIntersect(rect, bounds) == false) {
            return false;
        }

        // transform curved bezier segments to straight line segments.
        // tolerance for conversion is 0.5 pixel converted to world coordinates.
        final double tolDist = 0.5 / scale;

        // loop over all straight line segments of this path
        PathIterator pi = this.path.toPathIterator(null, tolDist);
        double lx1 = 0;
        double ly1 = 0;
        double lx2, ly2;
        double lastMoveToX = 0;
        double lastMoveToY = 0;
        double[] coords = new double[6];
        int segmentType;
        while (pi.isDone() == false) {
            segmentType = pi.currentSegment(coords);
            lx2 = coords[0];
            ly2 = coords[1];
            switch (segmentType) {
                case PathIterator.SEG_CLOSE:
                    lx2 = lastMoveToX;
                    ly2 = lastMoveToY;
                // fall through, no break here
                case PathIterator.SEG_LINETO:
                    // test if rect and the line segment intersect.
                    if (GeometryUtils.lineIntersectsRectangle(lx1, ly1, lx2, ly2, rect)) {
                        return true;
                    }
                    lx1 = lx2;
                    ly1 = ly2;
                    break;

                case PathIterator.SEG_MOVETO:
                    lastMoveToX = lx1 = lx2;
                    lastMoveToY = ly1 = ly2;
                    break;
            }
            pi.next();
        }
        return false;
    }

    public Rectangle2D getBounds2D(double scale) {
        return (path != null) ? path.getBounds2D() : null;
    }

    /**
     * Scale this path by a factor relative to a passed origin.
     *
     * @param scale Scale factor.
     * @param cx The x coordinate of the point relativ to which the object is
     * scaled.
     * @param cy The y coordinate of the point relativ to which the object is
     * scaled.
     */
    @Override
    public void scale(double scale, double cx, double cy) {
        this.path.scale(scale, cx, cy);
    }

    public void transform(AffineTransform affineTransform) {
        this.path.transform(affineTransform);
        MapEventTrigger.inform(this);
    }

    /**
     * Returns an iterator for this path. It is the caller's responsibility to
     * make sure that this path is not changed while a GeoPathIterator is used.
     */
    public GeoPathIterator getIterator() {
        return this.path.getIterator();
    }

    @Override
    public String toString() {

        StringBuffer str = new StringBuffer();
        PathIterator pi = path.toPathIterator(null);
        float[] coord = new float[6];
        while (pi.isDone() == false) {
            switch (pi.currentSegment(coord)) {
                case PathIterator.SEG_MOVETO:
                    str.append("moveto " + coord[0] + " " + coord[1] + "\n");
                    break;

                case PathIterator.SEG_LINETO:
                    str.append("lineto " + coord[0] + " " + coord[1] + "\n");
                    break;

                case PathIterator.SEG_QUADTO:
                    str.append("quad " + coord[0] + " " + coord[1]
                            + "\n\t" + coord[2] + " " + coord[3] + "\n");
                    break;

                case PathIterator.SEG_CUBICTO:
                    str.append("cubic " + coord[0] + " " + coord[1]
                            + "\n\t" + coord[2] + " " + coord[3]
                            + "\n\t" + coord[4] + " " + coord[5] + "\n");
                    break;

                case PathIterator.SEG_CLOSE:
                    str.append("close\n");
                    break;

            }
            pi.next();
        }

        return super.toString() + " \n" + str.toString() + this.symbol.toString();
    }

    public double getArea() {
        return this.path.getArea();
    }

    public ArrayList<Point2D> toBeads(double d,
            double jitterAlongLine, double jitterVertical) {

        Random random = new Random(0);

        if (d <= 0) {
            throw new IllegalArgumentException();
        }

        ArrayList<Point2D> xy = new ArrayList<Point2D>();

        GeoPathIterator iterator = path.getIterator();
        double startX = iterator.getX();
        double startY = iterator.getY();

        // add start point
        xy.add(new Point2D.Double(startX, startY));

        double lastMoveToX = startX;
        double lastMoveToY = startY;

        double length = 0;
        while (iterator.next()) {
            double endX = 0;
            double endY = 0;
            final int inst = iterator.getInstruction();
            switch (inst) {

                case GeoPathModel.CLOSE:
                    endX = lastMoveToX;
                    endY = lastMoveToY;
                    break;

                case GeoPathModel.MOVETO:
                    startX = lastMoveToX = iterator.getX();
                    startY = lastMoveToY = iterator.getY();
                    continue;

                default:
                    endX = iterator.getX();
                    endY = iterator.getY();
                    break;

            }

            // normalized direction dx and dy
            double dx = endX - startX;
            double dy = endY - startY;
            final double l = Math.hypot(dx, dy);
            dx /= l;
            dy /= l;

            double rest = length;
            length += l;
            while (length >= d) {
                // compute new point
                length -= d;
                startX += dx * (d - rest);
                startY += dy * (d - rest);
                rest = 0;
                Point2D.Double pt = new Point2D.Double(startX, startY);
                this.jitter(pt, dx, dy, jitterAlongLine, jitterVertical, random);
                xy.add(pt);
            }
            startX = endX;
            startY = endY;
        }

        return xy;
    }

    private void jitter(Point2D.Double pt, double ndx, double ndy,
            double maxJitterAlongLine, double maxJitterVertical, Random random) {

        final double jitterAlongLine = maxJitterAlongLine * (random.nextDouble() - 0.5);
        final double jitterVertical = maxJitterVertical * (random.nextDouble() - 0.5);
        final double dx = ndx * jitterAlongLine - ndy * jitterVertical;
        final double dy = ndy * jitterAlongLine + ndx * jitterVertical;
        pt.x += dx;
        pt.y += dy;

    }

}
