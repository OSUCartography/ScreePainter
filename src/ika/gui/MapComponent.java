/*
 * MapComponent.java
 *
 * Created on 5. Februar 2005, 16:43
 */
package ika.gui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import ika.geo.*;
import ika.map.tools.*;
import ika.utils.*;
import java.util.ArrayList;

/**
 * An interactive JComponent for drawing and working with map data.<br>
 * This MapComponent contains a GeoSet that is used for all manipulations.
 *
 * @beaninfo attribute: isContainer false
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class MapComponent extends javax.swing.JComponent {

    /**
     * The root contains the backgroundGeoSet, the mainGeoSet and the
     * foregroundGeoSet in this order. It is a wrapper containing all GeoObjects
     * of the map. It is not accessible from other classes.
     */
    private GeoTreeRoot root = null;

    /**
     * The current scale to display the GeoSet. Maps between world coordinates
     * of the GeoObjects and the internal coordinate space of this MapComponent.
     */
    private double scale = 1.;

    /**
     * The amount by which the scale factor changes on a simple zoom-in or
     * zoom-out command without specifying the exact new scale factor.
     */
    private static final double ZOOM_STEP = 1. / 3.;

    private static final double MIN_SCALE = 0.0000001;

    /**
     * The MapEventHandler is responsible for treating all key and mouse events
     * for this MapComponent. The whole functionality of MapEventHandler could
     * have been integrated into MapComponent. By separting the two, the
     * MapComponent is much easier to understand, program and extend.
     */
    private MapEventHandler mapEventHandler = null;

    /**
     * A BufferedImage that is used as double buffer for drawing. Swing is using
     * its own double buffer mechanism. However, with this private double
     * buffer, MapTools that need quick drawing don't have to wait until the
     * complete map is redrawn, but can just draw this doubleBuffer, and then
     * draw their own stuff.
     */
    private BufferedImage doubleBuffer = null;

    /**
     * Keep track of the top left coordinate of the visible area in world
     * coordinates.
     */
    private Point2D.Double topLeft = new Point2D.Double(0, 1000);

    /**
     * Rendering settings for all raster images in this map.
     */
    private Object imageRenderingHint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;

    /**
     * A formatter that can be used to display coordinates of this map.
     */
    private CoordinateFormatter coordinateFormatter
            = new CoordinateFormatter("###,##0.00", "###,##0", 1);

    /**
     * MapDropTarget is a helper object that handles drag and drop of data on
     * this component.
     */
    private MapDropTarget mapDropTarget;

    /**
     * infoString is drawn in this component when no visible GeoObject is
     * present.
     */
    private String infoString = "Drag your map data here.";

    private RenderParamsProvider renderParamsProvider;

    /**
     * Event handlers that are called when the scale changes.
     */
    private ArrayList scaleChangeHandlers = new ArrayList(1);

    /**
     * MapUndoManager is responsible for applying and providing undo/redo data
     * snapshots. Can be null, in which case the child data of GeoTreeRoot is
     * serialized and deserialized. See undo() and redo() of this class.
     */
    private MapUndoManager mapUndoManager = null;

    /**
     * An affine transformation that is applied to selected objects when drawing
     * them. This allows MapTools to interactively edit the selected objects
     * without the need to copy them and transform their geoemetry. Usually,
     * transformForSelectedObjects is null, which means that no transformation
     * has to be applied on selected objects.
     */
    private AffineTransform transformForSelectedObjects = null;

    /**
     * Creates a new instance of MapComponent
     */
    public MapComponent() {
        this.root = new GeoTreeRoot();
        this.mapEventHandler = new MapEventHandler(this);

        // add a drag and drop handler. This allows for files and data being 
        // dropped on this MapComponent.
        this.setMapDropTarget(new MapDropTarget(this));
    }

    /**
     * Adds a GeoObject to the map. Deselects all previously selected GeoObjects
     * if deselectExisting is true.
     *
     * @param geoObject A GeoObject that will be added to the map.
     * @param deselectExisting If true, all GeoObjects previously contained in
     * this GeoSet are deselected before the passed GeoObject is attached.
     */
    public void addGeoObject(GeoObject geoObject, boolean deselectExisting) {
        if (geoObject == null) {
            return;
        }

        if (deselectExisting) {
            this.root.getMainGeoSet().deselectAndAdd(geoObject);
        } else {
            this.root.getMainGeoSet().add(geoObject);
        }

        if (!isObjectVisibleOnMap(geoObject)) {
            this.showAll();
        }
    }

    public  void deselectAllAndAddChildren(GeoSet geoSet) {
        this.root.getMainGeoSet().deselectAndAddChildren(geoSet);
    }

    /**
     * Removes all objects from the mainGeoSet. Also removes GeoObjects that are
     * not currently selected, or are not selectable at all.
     */
    public void removeAllGeoObjects() {
        this.root.getMainGeoSet().removeAllGeoObjects();
    }

    /**
     * Removes all GeoObjects from the mainGeoSet that are currently selected.
     */
    public boolean removeSelectedGeoObjects() {
        return this.root.getMainGeoSet().removeSelectedGeoObjects();
    }

    /**
     * Selects all objects contained in mainGeoSet.
     */
    public void selectAllGeoObjects() {
        this.root.getMainGeoSet().setSelected(true);
    }

    public boolean selectByPoint(Point2D point, boolean extendSelection,
            double pixelTolerance) {
        final double worldCoordTol = pixelTolerance / scale;
        final boolean selectionChanged = this.root.selectByPoint(
                point, scale, extendSelection, worldCoordTol);
        return selectionChanged;
    }

    public  boolean selectByRectangle(Rectangle2D rect,
            boolean extendSelection) {
        return this.root.selectByRectangle(rect, scale, extendSelection);
    }

    public  boolean hasSelectedGeoObjects() {
        return this.root.getMainGeoSet().hasSelectedGeoObjects();
    }

    public  boolean hasVisibleGeoObjects() {
        return this.root.getMainGeoSet().hasVisibleGeoObjects();
    }

    /**
     * Deselects all GeoObjects contained in the map.
     */
    public void deselectAllGeoObjects() {
        this.root.setSelected(false);
    }

    public  boolean moveSelectedGeoObjects(double dx, double dy) {
        return this.root.moveSelected(dx, dy);
    }

    public  boolean cloneAndMoveSelectedGeoObjects(double dx, double dy) {
        return this.root.cloneAndMoveSelected(dx, dy);
    }

    public  void scaleSelectedGeoObjects(double sx, double sy) {
        this.root.scaleSelected(sx, sy);
    }

    public  void transformSelectedGeoObjects(AffineTransform trans) {
        this.root.transformSelected(trans);
    }

    public  void deformSelectedGeoObjects(Rectangle2D newBounds) {
        this.root.deformSelected(newBounds);
    }

    public Rectangle2D getBoundingBoxOfSelectedGeoObjects() {
        return this.root.getBounds2D(this.getScaleFactor(), true, true);
    }

    /**
     * Centers the map display on the passed point.
     *
     * @param center The new center of the visible area in world coordinates.
     */
    public void centerOnPoint(Point2D.Double center) {
        this.centerOnPoint(center.getX(), center.getY());
    }

    /**
     * Centers the map display on the passed point.
     *
     * @param cx The new center of the visible area in world coordinates.
     * @param cy The new center of the visible area in world coordinates.
     */
    public void centerOnPoint(double cx, double cy) {
        final double x = cx - this.getVisibleWidth() / 2.;
        final double y = cy + this.getVisibleHeight() / 2.;
        topLeft.setLocation(x, y);
        this.repaint();
    }

    /**
     * Shifts the currently displayed area of the map in horizontal and vertical
     * direction.
     *
     * @param dx Offset in horizontal direction in world coordinates.
     * @param dy Offset in vertical direction in world coordinates.
     */
    public void offsetVisibleArea(double dx, double dy) {
        topLeft.x += dx;
        topLeft.y += dy;
        this.repaint();
    }

    /**
     * Zooms into the map. The currently visible center of the map is
     * maintained.
     */
    public void zoomIn() {
        zoom(1. + ZOOM_STEP);
    }

    /**
     * Zooms into the map, and centers on the passed point.
     *
     ** @param center The new center of the visible area in world coordinates.
     */
    public void zoomIn(Point2D.Double center) {
        zoomOnPoint(1. + ZOOM_STEP, center);
    }

    /**
     * Zooms out of the map. The currently visible center is retained.
     */
    public void zoomOut() {
        zoom(1. / (1. + ZOOM_STEP));
    }

    /**
     * Zooms out and centers the visible area on the passed point.
     *
     * @param center The new center of the visible area in world coordinates.
     */
    public void zoomOut(Point2D.Double center) {
        zoomOnPoint(1. / (1. + ZOOM_STEP), center);
    }

    /**
     * Zooms out and centers the visible area on the passed point.
     *
     * @param zoomFactor The new zoom factor.
     */
    public void zoom(double zoomFactor) {
        this.setScaleFactor(scale * zoomFactor);
    }

    /**
     * Changes the current scale by a specified factor and centers the currently
     * visible area on a passed point.
     *
     * @param zoomFactor The new zoom factor.
     * @param pt The new center of the visible area in world coordinates.
     */
    public void zoomOnPoint(double zoomFactor, Point2D.Double pt) {
        this.setScaleFactor(this.scale * zoomFactor);
        this.topLeft.x = pt.x - this.getVisibleWidth() / 2;
        this.topLeft.y = pt.y + this.getVisibleHeight() / 2;
        this.repaint();
    }

    /**
     * Zooms on passed rectangle. Makes sure the area contained in the rectangle
     * becomes entirely visible.
     *
     * @param rect The area that will be at least visible.
     */
    public void zoomOnRectangle(Rectangle2D rect) {
        if (rect == null) {
            return;
        }

        centerOnPoint(rect.getCenterX(), rect.getCenterY());
        final double horScale = getVisibleWidth() / rect.getWidth() * scale;
        final double verScale = getVisibleHeight() / rect.getHeight() * scale;
        final double newScale = Math.min(horScale, verScale);
        this.setScaleFactor(newScale);
    }

    /**
     * Changes the scale factor and the center of the currently visible area so
     * that all visible GeoObjects contained in the map will be shown.
     */
    public void showAll() {

        // a white border on each side of the map data expressed in percentage
        // of the map size.
        final double BORDER_PERCENTAGE = 2;

        Rectangle2D box = root.getBounds2D(
                GeoObject.UNDEFINED_SCALE, true, false);
        if (box == null || !ika.utils.GeometryUtils.isRectangleValid(box)) {
            return;
        }

        Insets insets = this.getInsets();
        final double width = this.getWidth() - insets.left - insets.right;
        final double height = this.getHeight() - insets.top - insets.bottom;
        final double borderScale = 1 / (1 + 2 * BORDER_PERCENTAGE / 100);
        if (box.getWidth() == 0 && box.getHeight() == 0) {
            this.scale = MIN_SCALE;
            box.setFrame(box.getMinX() - 1, box.getMinY() - 1, 2, 2);
        } else if (box.getWidth() == 0) {
            this.setScaleFactor(height / box.getHeight() * borderScale);
        } else if (box.getHeight() == 0) {
            this.setScaleFactor(width / box.getWidth() * borderScale);
        } else {
            final double horScale = width / box.getWidth();
            final double verScale = height / box.getHeight();
            this.setScaleFactor(Math.min(horScale, verScale) * borderScale);
        }
        this.centerOnPoint(box.getCenterX(), box.getCenterY());
    }

    /**
     * Returns the current scale factor used to display the map. This is the
     * scale factor between screen pixels and ground units.
     *
     * @return The current scale factor.
     */
    public double getScaleFactor() {
        return this.scale;
    }

    /**
     * Returns the current scale used to display the map. If the map is
     * displayed at 1:10,000, the returned value is 10,000.
     *
     * @return The current scale.
     */
    public double getScaleNumber() {

        GraphicsConfiguration gc = this.getGraphicsConfiguration();
        if (gc == null) {
            return 1. / this.getScaleFactor();
        }
        AffineTransform trans = gc.getNormalizingTransform();
        final double dpiScale = trans.getScaleX();
        final double mapScale = this.getScaleFactor();
        return (72. * dpiScale * 100. / 2.54) / mapScale;

    }

    /**
     * Changes the scale factor used to display the map.
     *
     * @param scale The new scale factor.
     */
    public void setScaleFactor(double scale) {

        if (scale < MIN_SCALE) {
            scale = MIN_SCALE;
        }

        final Rectangle2D.Double visibleRect = getVisibleArea();
        final double cx = visibleRect.getCenterX();
        final double cy = visibleRect.getCenterY();
        double dx = cx - topLeft.getX();
        double dy = cy - topLeft.getY();
        dx *= this.scale / scale;
        dy *= this.scale / scale;
        topLeft.setLocation(cx - dx, cy - dy);
        this.scale = scale;
        this.repaint();

        this.fireScaleChangeEvent();
    }

    /**
     * Returns whether a GeoObject is currently displayed in the map.
     *
     * @param geoObject The GeoObject to test.
     * @return true if the passed GeoObject is entirely visible in the map,
     * false otherwise.
     */
    public boolean isObjectVisibleOnMap(GeoObject geoObject) {
        if (geoObject == null) {
            throw new IllegalArgumentException();
        }

        final Rectangle2D geoBounds = geoObject.getBounds2D(this.scale);
        final Rectangle2D.Double visArea = this.getVisibleArea();

        // make sure null objects are not passed here!
        if (geoBounds != null && visArea != null) {
            return (visArea.contains(geoBounds));
        }

        // return true if geoBounds is null. This is the case for empty GeoSets
        // Empty GeoSets are considered to be always visible.
        return true;
    }

    /**
     * Returns whether currently all GeoObjects contained in the map are
     * displayed in the map.
     *
     * @return True if all GeoObjects of the map are currently visible.
     */
    public boolean isAllVisible() {
        final Rectangle2D geoBounds = this.root.getBounds2D(this.scale);
        final Rectangle2D.Double visArea = this.getVisibleArea();
        return (visArea.contains(geoBounds));
    }

    /**
     * Returns the extension of the visible area in world coordinates (the
     * coordinate system used by the GeoObjects).
     *
     * @return The currently visible area in world coordinates.
     */
    public Rectangle2D.Double getVisibleArea() {
        double width = this.getVisibleWidth();
        double height = this.getVisibleHeight();
        Rectangle2D.Double visibleRect = new Rectangle2D.Double();
        visibleRect.setFrame(topLeft.getX(), topLeft.getY() - height, width, height);
        return visibleRect;
    }

    /**
     * Returns the width of the currently visible area in world coordinates (the
     * coordinate system used by the GeoObjects).
     *
     * @return The width of the currently visible area in world coordinates.
     */
    public double getVisibleWidth() {
        Insets insets = this.getInsets();
        final double width = this.getWidth() - insets.left - insets.right;
        return width / scale;
    }

    /**
     * Returns the height of the currently visible area in world coordinates
     * (the coordinate system used by the GeoObjects).
     *
     * @return The height of the currently visible area in world coordinates.
     */
    public double getVisibleHeight() {
        Insets insets = this.getInsets();
        final double height = this.getHeight() - insets.top - insets.bottom;
        return height / scale;
    }

    /**
     * Returns the GeoSet that is currently used.
     *
     * @return The GeoSet that is used. Can be null!
     */
    public GeoSet getGeoSet() {
        return this.root.getMainGeoSet();
    }

    /**
     * Returns a GeoSet that can be used by importers to add GeoObjects to the
     * map, and by exporters to write the map to some external file format.
     *
     * @return A GeoSet to store imported data or extract data to export.
     */
    public GeoSet getImportExportGeoSet() {
        return this.root.getMainGeoSet();
    }

    /**
     * Returns the GeoSet that contains data that is displayed above all other
     * data. The foreground GeoSet is not exported and is usually only used
     * temporarily.
     */
    public GeoSet getForegroundGeoSet() {
        return this.root.getForegroundGeoSet();
    }

    /**
     * Set the GeoSet that is displayed. Objects derived from GeoSet can be
     * passed here, which gives applications a chance to do custom computations
     * e.g. for drawing and storing their data in a private model that is
     * derived from GeoSet.
     */
    public void setGeoSet(GeoSet geoSet) {
        this.root.setMainGeoSet(geoSet);
    }

    /**
     * Returns the visually top-most object contained in this GeoSet that is
     * under a passed point.
     *
     * @param point The point for hit detection.
     * @param pixelTolDist The tolerance to use for hit detection in pixel
     * coordinates.
     * @return Returns the GeoObject if any, null otherwise.
     */
    public  GeoObject getObjectAtPosition(Point2D point,
            double pixelTolDist,
            boolean onlySelectable,
            boolean onlyVisible) {
        final double worldTolDist = pixelTolDist / this.getScaleFactor();
        return this.root.getObjectAtPosition(point,
                worldTolDist, this.getScaleFactor(), onlySelectable, onlyVisible);
    }

    /**
     * Converts from the coordinate system of this MapComponent to the world
     * coordinate system used by the GeoObjects.
     *
     * @param pt The point to convert. Will not be changed.
     * @return The convert point in world coordinates.
     */
    protected final Point2D.Double userToWorldSpace(Point pt) {
        // compensate for border around the map
        final Insets insets = this.getInsets();

        final double x = (pt.getX() - insets.left) / scale + topLeft.getX();
        final double y = -(pt.getY() - insets.top) / scale + topLeft.getY();
        return new Point2D.Double(x, y);
    }

    /**
     * Converts from the world coordinate system used by the GeoObjects to the
     * coordinate system of this MapComponent.
     *
     * @param pt The point to convert. The converted coordinates will also be
     * stored in pt.
     */
    protected final void worldToUserSpace(Point2D.Double pt) {
        final double x = (pt.getX() - topLeft.getX()) * scale;
        final double y = (topLeft.getY() - pt.getY()) * scale;

        // compensate for border around the map
        final Insets insets = this.getInsets();
        pt.setLocation(x + insets.left, y + insets.top);
    }

    /**
     * Change the affine transformation of a graphics destination for drawing of
     * world coordinates, i.e. coordinates of the map objects.
     *
     * @param g2d The graphics destination to change.
     */
    public void setupGraphics2DForDrawingInWorldCoordinates(Graphics2D g2d) {
        // Transformation:
        // x_ = (x-west)*scale;
        // y_ = (north-y)*scale = (y-north)*(-scale);
        g2d.scale(scale, -scale);
        g2d.translate(-topLeft.getX(), -topLeft.getY());
    }

    public void shiftGraphics2DByBorderWidth(Graphics2D g2d) {
        // compensate for the borders
        final Insets insets = this.getInsets();
        g2d.translate(insets.left, insets.top);
    }

    private RenderParams getRenderParams(Graphics2D g2d) {

        // compute the visible area
        final Insets insets = getInsets();
        final int currentWidth = getWidth() - insets.left - insets.right;
        final int currentHeight = getHeight() - insets.top - insets.bottom;
        double width = currentWidth / this.scale;
        double height = currentHeight / this.scale;

        // create default rendering parameters
        RenderParams rp = rp = new RenderParams(g2d, this.scale,
                this.topLeft.x, this.topLeft.y - height,
                width, height, true, this.transformForSelectedObjects);

        // ask registered RenderParamsProvider for customized parameters
        if (this.renderParamsProvider != null) {
            rp = this.renderParamsProvider.getRenderParams(rp);
        }

        return rp;
    }

    /**
     * Draw the non-selected map objects.
     *
     * @param g2d The destination for drawing.
     * @rp The rendering parameters.
     */
    private void drawNormalState(Graphics2D g2d, RenderParams rp) {

        // enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        // enable high quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        // enable bicubic interpolation of images
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                this.imageRenderingHint);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        // set default appearance of vector elements
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(Color.black);

        // draw the normal state of the objects
        this.root.drawNormalState(rp);

    }

    /**
     * Draw the selected map objects.
     *
     * @param g2d The destination for drawing.
     * @rp The rendering parameters.
     */
    private void drawSelectedState(Graphics2D g2d, RenderParams rp) {

        // rendering should be as fast as possible without anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);

        // apply transformation for selected objects
        if (this.transformForSelectedObjects != null) {
            g2d.transform(this.transformForSelectedObjects);
        }

        // Use a line width of 0 for a thin selected line. This is
        // ok, see java bug #4114921 and #4093921.
        g2d.setStroke(new BasicStroke(0));

        // color
        final Color higlightColor = ika.utils.ColorUtils.getHighlightColor();
        g2d.setColor(higlightColor);

        // draw selected objects
        this.root.drawSelectedState(rp);

    }

    /**
     * Paints this map into a Graphics2D.
     *
     * @param g2d The drawing destination. May be associated with a buffered
     * image or a component. If associated with a component, the insets must be
     * compensated for.
     * @param onlyDrawSelected If true, only selected GeoObjects will be drawn.
     */
    public void paintMap(Graphics2D g2d, boolean onlyDrawSelected) {

        this.setupGraphics2DForDrawingInWorldCoordinates(g2d);
        RenderParams rp = this.getRenderParams(g2d);

        if (!onlyDrawSelected) {
            this.drawNormalState(g2d, rp);
        }
        this.drawSelectedState(g2d, rp);

    }

    /**
     * Utility method that returns the bounding box of the area that needs to be
     * redrawn.
     *
     * @param g The graphics object that needs to be redrawn.
     * @return The bounding box of the area that must be redrawn.
     */
    private Rectangle getDirtyArea(Graphics g) {
        Dimension size = this.getSize();
        Rectangle drawingArea = new Rectangle(0, 0, size.width, size.height);
        // The clipping area is the part of the component that needs to be
        // repainted, which might be smaller than the entire component area.
        return g.getClipBounds(drawingArea);
    }

    /**
     * Returns the internal Double Buffer used for drawing the map. This is
     * useful for MapTools that temporarily draw over the map.
     *
     * @return A reference to the double buffer with the content remaining from
     * the last repaint.
     */
    public BufferedImage getDoubleBuffer() {
        return doubleBuffer;
    }

    /**
     * Draws this.infoString if the map at the currently visible map center.
     */
    private void paintInfoString(Graphics2D g2d) {

        if (this.infoString == null || this.infoString.length() == 0) {
            return;
        }

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setColor(java.awt.Color.gray);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 22));
        final Insets insets = getInsets();
        float centerX = (getWidth() - insets.left - insets.right) / 2f + insets.left;
        float centerY = (getHeight() - insets.top - insets.bottom) / 2f + insets.top;
        ika.utils.CenteredStringRenderer.drawCentered(g2d, this.infoString,
                centerX, centerY, ika.utils.CenteredStringRenderer.NOFLIP);

    }

    /**
     * Overwrite paintComponent of JComponent to do custom drawing.
     *
     * @param g The destination to draw to.
     */
    @Override
    protected void paintComponent(Graphics g) {

        //ika.utils.NanoTimer timer = new ika.utils.NanoTimer();
        // final long startTime = timer.nanoTime();
        /* From a sun java tutorial:
         *Make sure that when the paintComponent method exits, the Graphics
         object that was passed into it has the same state that it had at the
         start of the method. For example, you should not alter the clip
         Rectangle or modify the transform.
         If you need to do these operations you may find it easier to
         create a new Graphics from the passed in Graphics and manipulate
         it. Further, if you do not invoker super's implementation you
         must honor the opaque property, that is if this component is
         opaque, you must completely fill in the background in a
         non-opaque color. If you do not honor the opaque property you
         will likely see visual artifacts.
         */
        Graphics2D g2d = (Graphics2D) g.create(); //copy g. Recomended by Sun tutorial.
        AffineTransform origTransform = g2d.getTransform();

        final Insets insets = getInsets();
        final int currentWidth = getWidth() - insets.left - insets.right;
        final int currentHeight = getHeight() - insets.top - insets.bottom;

        // make sure the doubleBuffer image is allocated
        if (doubleBuffer == null
                || doubleBuffer.getWidth() != currentWidth
                || doubleBuffer.getHeight() != currentHeight) {
            doubleBuffer = (BufferedImage) createImage(currentWidth, currentHeight);
        }

        // Give the current MapTool a chance to draw some background drawing.
        // Returns true if the the tool also painted the map, i.e. there is no
        // need to paint the map.
        MapTool mapTool = this.getMapTool();
        boolean toolPaintedMap = mapTool == null ? false : mapTool.drawBackground(g2d);

        // paint the map if this has not been done by the current MapTool
        if (toolPaintedMap == false) {
            // draw the map into the doubleBuffer image
            Graphics2D doubleBufferG2D = (Graphics2D) doubleBuffer.getGraphics();
            Color backgroundColor = this.getBackground();
            if (backgroundColor == null) {
                backgroundColor = Color.WHITE;
            }
            doubleBufferG2D.setBackground(backgroundColor);
            doubleBufferG2D.clearRect(0, 0, currentWidth, currentHeight);
            this.paintMap(doubleBufferG2D, false);

            // draw the doubleBuffer image
            g2d.setTransform(origTransform);
            g2d.drawImage(doubleBuffer, insets.left, insets.top, this);

            // draw a box around all selected GeoObjects
            g2d.setTransform(origTransform);
            this.setupGraphics2DForDrawingInWorldCoordinates(g2d);
            final Rectangle2D selectionBox = this.root.getMainGeoSet().getBounds2D(scale, true, true);
            if (this.transformForSelectedObjects != null) {
                g2d.transform(this.transformForSelectedObjects);
            }
            SelectionBox.paintSelectionBox(selectionBox, g2d, scale, true);
        }

        // Give the current MapTool a chance to draw some custom graphics.
        g2d.setTransform(origTransform);
        if (mapTool != null) {
            RenderParams rp = this.getRenderParams(g2d);
            mapTool.draw(rp);
        }

        // draw the info string if the map does not contain any data
        if (this.root.getMainGeoSet().hasVisibleGeoObjects() == false) {
            g2d.setTransform(origTransform);
            this.paintInfoString(g2d);
        }

        g2d.dispose(); //release the copy's resources. Recomended by Sun tutorial.

        /*
        System.out.println ("Time for Drawing Map: "
                + (timer.nanoTime() - startTime) / 1000 / 1000
                + " Milliseconds");
         */
    }

    /**
     * Inform Swing that this JComponent is opaque, i.e. we are drawing the
     * whole area of this Component. This accelerates the drawing of the
     * component.
     *
     * @return true if opaque.
     */
    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return true;
    }

    /**
     * Returns the current MapTool
     *
     * @return The currently active MapTool.
     */
    public MapTool getMapTool() {
        return mapEventHandler.getMapTool();
    }

    /**
     * Sets the current MapTool.
     *
     * @param mapTool The new MapTool
     */
    public void setMapTool(MapTool mapTool) {
        mapEventHandler.setMapTool(mapTool, false);
    }

    public void removeMouseMotionListener(MapToolMouseMotionListener listener) {
        this.mapEventHandler.removeMouseMotionListener(listener);
    }

    public void addMouseMotionListener(MapToolMouseMotionListener listener) {
        this.mapEventHandler.addMouseMotionListener(listener);
    }

    public Object getImageRenderingHint() {
        return imageRenderingHint;
    }

    public void setImageRenderingHint(Object imageRenderingHint) {
        if (imageRenderingHint != RenderingHints.VALUE_INTERPOLATION_BICUBIC
                && imageRenderingHint != RenderingHints.VALUE_INTERPOLATION_BILINEAR
                && imageRenderingHint != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
            throw new IllegalArgumentException();
        }

        this.imageRenderingHint = imageRenderingHint;
    }

    /**
     * Returns a formatter that is used to display coordinates of this map.
     */
    public CoordinateFormatter getCoordinateFormatter() {
        return coordinateFormatter;
    }

    /**
     * Sets the formatter that is used to display coordinates of this map.
     */
    public void setCoordinateFormatter(CoordinateFormatter coordinateFormatter) {
        this.coordinateFormatter = coordinateFormatter;
    }

    public MapDropTarget getMapDropTarget() {
        return mapDropTarget;
    }

    public void setMapDropTarget(MapDropTarget mapDropTarget) {
        this.mapDropTarget = mapDropTarget;
    }

    public String getInfoString() {
        return infoString;
    }

    public void setInfoString(String dragInfoString) {
        this.infoString = dragInfoString;
    }

    public RenderParamsProvider getRenderParamsProvider() {
        return renderParamsProvider;
    }

    public void setRenderParamsProvider(RenderParamsProvider renderParamsProvider) {
        this.renderParamsProvider = renderParamsProvider;
    }

    /**
     * Add a ScaleChangeHandler. It will be informed when the scale of this
     * MapComponent changes.
     */
    public void addScaleChangeHandler(ScaleChangeHandler h) {
        this.scaleChangeHandlers.add(h);
    }

    /**
     * Remove a ScaleChangeHandler from the array of registered handlers.
     */
    public void removeScaleChangeHandler(ScaleChangeHandler h) {
        this.scaleChangeHandlers.remove(h);
    }

    /**
     * Inform all registered ScaleChangeHandlers that the scale of this
     * MapComponent changed.
     */
    protected void fireScaleChangeEvent() {
        final MapComponent mapComponent = this;
        final double scaleFactor = getScaleFactor();
        final double scaleNumber = getScaleNumber();
        ika.utils.SwingThreadUtils.invokeAndWait(new Runnable() {
            public void run() {
                for (int i = scaleChangeHandlers.size() - 1; i >= 0; i--) {
                    ScaleChangeHandler h
                            = (ScaleChangeHandler) scaleChangeHandlers.get(i);
                    h.scaleChanged(mapComponent, scaleFactor, scaleNumber);
                }
            }
        });
    }

    /**
     * Take a data snapshot and store it in the Undo manager. Call addUndo after
     * changing the mainGeoSet.
     *
     * @param name The name of the action that can be undone later.
     */
    public void addUndo(String name) {
        /*try {
            this.undo.add(name, getUndoRedoState());
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public AffineTransform getTransformForSelectedObjects() {
        return transformForSelectedObjects;
    }

    public void setTransformForSelectedObjects(AffineTransform transformForSelectedObjects) {
        this.transformForSelectedObjects = transformForSelectedObjects;
    }

    public PageFormat getPageFormat() {
        return this.root.getPageFormat();
    }

    public MapUndoManager getMapUndoManager() {
        return mapUndoManager;
    }

    public void setMapUndoManager(MapUndoManager mapUndoManager) {
        this.mapUndoManager = mapUndoManager;
    }

}
