/*
 * GeoTreeRoot.java
 *
 * Created on April 17, 2007, 3:44 PM
 *
 */
package ika.geo;

import ika.gui.PageFormat;
import ika.utils.Serializer;
import java.awt.Color;
import java.awt.geom.Rectangle2D;

/**
 * GeoTreeRoot has three GeoSets: background, main, and foreground. Usually the
 * mainGeoSet is edited by the application, the foreground and the background
 * serve as container for supplemental data. GeoTreeRoot also has a reference to
 * a PageFormat that determines the size and position of the map document.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class GeoTreeRoot extends GeoSet {

    /**
     * A GeoSet containing additional information that is drawn, but cannot
     * usually be manipulated by the user. The data contained by
     * backgroundGeoSet is not exported by GeoSetExporters. It can be used for a
     * background raster grid or other usually non-manipulatable graphics.
     */
    private GeoSet backgroundGeoSet = null;

    /**
     * The main GeoSet that is displayed by this MapComponent and exported by
     * GeoExporters.
     */
    private GeoSet mainGeoSet = null;

    /**
     * A GeoSet containing additional GeoObjects that are displayed in front of
     * the backgroundGeoSet and the mainGeoSet, e.g. the page outline. This
     * GeoSet is not exported by GeoSetExporters.
     */
    private GeoSet foregroundGeoSet = null;

    /**
     * The size, position and scale of the page.
     */
    private PageFormat pageFormat = new PageFormat();

    /**
     * Creates a new instance of GeoTreeRoot
     */
    public GeoTreeRoot() {
        this.setName("root");
        this.mainGeoSet = new GeoSet();
        this.mainGeoSet.setName("main");
        this.backgroundGeoSet = new GeoSet();
        this.backgroundGeoSet.setName("background");
        this.foregroundGeoSet = new GeoSet();
        this.foregroundGeoSet.setName("foreground");
        this.add(this.backgroundGeoSet);
        this.add(this.mainGeoSet);
        this.add(this.foregroundGeoSet);
    }

    public byte[][] serializeModel() throws java.io.IOException {
        // serialize the backgroundGeoSet, the mainGeoSet and the foregroundGeoSet.
        byte[] b = Serializer.serialize(this.backgroundGeoSet, true);
        byte[] m = Serializer.serialize(this.mainGeoSet, true);
        byte[] f = Serializer.serialize(this.foregroundGeoSet, true);
        byte[] pf = Serializer.serialize(this.pageFormat, true);
        return new byte[][]{b, m, f, pf};
    }

    public void deserializeModel(byte[][] data)
            throws java.lang.ClassNotFoundException, java.io.IOException {

        Object b = Serializer.deserialize(data[0], true);
        Object m = Serializer.deserialize(data[1], true);
        Object f = Serializer.deserialize(data[2], true);
        Object pf = Serializer.deserialize(data[3], true);

        this.setBackgroundGeoSet((GeoSet) b);
        this.setMainGeoSet((GeoSet) m);
        this.setForegroundGeoSet((GeoSet) f);
        this.pageFormat = (PageFormat) pf;
    }

    public GeoSet getMainGeoSet() {
        return this.mainGeoSet;
    }

    public void setMainGeoSet(GeoSet newMainGeoSet) {
        if (newMainGeoSet != null) {
            GeoSet oldMainGeoSet = this.mainGeoSet;
            // replaceGeoObject() will trigger a MapEvent. An event listener may
            // access this.mainGeoSet. Therefore replace this.mainGeoSet before
            // calling replaceGeoObject().
            this.mainGeoSet = newMainGeoSet;
            this.replaceGeoObject(newMainGeoSet, oldMainGeoSet);
        }
    }

    public GeoSet getBackgroundGeoSet() {
        return backgroundGeoSet;
    }

    public void setBackgroundGeoSet(GeoSet geoSet) {
        if (geoSet != null) {
            this.backgroundGeoSet = geoSet;
        }
    }

    public GeoSet getForegroundGeoSet() {
        return foregroundGeoSet;
    }

    public void setForegroundGeoSet(GeoSet geoSet) {
        if (geoSet != null) {
            this.foregroundGeoSet = geoSet;
        }
    }

    /**
     * Returns this GeoTreeRoot. Overwrites getRoot() of GeoObject which
     * traverses the tree of GeoObject in upwards direction. This call is the
     * last one in the chain.
     *
     * @return The GeoTreeRoot of the tree. This is the topmost Geoset.
     */
    public GeoTreeRoot getRoot() {
        return this;
    }

    public PageFormat getPageFormat() {
        return pageFormat;
    }

}
