/*
 * DataReceiver.java
 *
 * Created on November 5, 2006, 3:43 PM
 *
 */
package ika.geoimport;

import ika.geo.*;
import ika.table.Table;
import ika.utils.FileUtils;

/**
 * DataReceiver receives imported GeoObjects from a GeoImporter and is
 * responsible for storing the data. GeoObjects are stored in a destination
 * GeoSet that must be provided by derived classes. It stores passed attribute
 * Tables if the destination GeoSet is a GeoMap.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich
 */
abstract public class DataReceiver {

    private boolean showMessageOnError = true;

    private boolean hasReceivedError = false;

    /**
     * Creates a new instance of DataReceiver.
     */
    public DataReceiver() {
    }

    /**
     * Derived classes must provide a GeoSet to store the imported GeoObjects.
     * @return the GeoSet that collects all received objects
     */
    abstract protected GeoSet getDestinationGeoSet();

    /**
     * Add a GeoObject.
     * @param geoObject object to add
     */
    public void add(GeoObject geoObject) {
        if (geoObject instanceof GeoSet) {
            this.add((GeoSet) geoObject);
        } else if (geoObject instanceof GeoImage) {
            this.add((GeoImage) geoObject);
        }

    }

    /**
     * Add a GeoSet.
     * @param geoSet object to add
     * @return true if successfully added
     */
    public boolean add(GeoSet geoSet) {
        GeoSet destGeoSet = this.getDestinationGeoSet();
        if (destGeoSet == null || geoSet == null || geoSet.getNumberOfChildren() < 1) {
            return false;
        }
        destGeoSet.add(geoSet);
        return true;
    }

    /**
     * Add a GeoImage.
     * @param geoImage image to add
     * @return true if successfully added
     */
    public boolean add(GeoImage geoImage) {
        GeoSet destGeoSet = this.getDestinationGeoSet();
        if (destGeoSet == null || geoImage == null) {
            return false;
        }
        destGeoSet.add(geoImage);
        return true;
    }

    /**
     * Add a TableLink, which usually has references to a Table and a GeoSet.
     * DataReceiver does not store the Table, derived classes can overwrite this
     * method and take care of this.
     * @param tableLink geometry and attributes to add
     * @return true if successfully added
     */
    public boolean add(ika.table.TableLink tableLink) {
        GeoSet destGeoSet = this.getDestinationGeoSet();
        if (tableLink == null || destGeoSet == null) {
            return false;        // make sure GeoSetChangedListeners are only informed after all 
            // changes are made.
        }
        try {
            GeoSet geoSet = tableLink.getGeoSet();
            if (geoSet != null && geoSet.getNumberOfChildren() == 0) {
                return false;
            }
            if (geoSet != null) {
                destGeoSet.add(geoSet);
            }

            // store the attribute tables
            Table table = tableLink.getTable();
            if (table != null && destGeoSet instanceof ika.geo.GeoMap) {
                ika.geo.GeoMap map = (ika.geo.GeoMap) destGeoSet;
                map.tableAdd(table);
                map.tableLinkAdd(tableLink);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void error(Exception exc, java.net.URL url) {

        hasReceivedError = true;

        // display a dialog with an error message.
        if (showMessageOnError) {
            String message;
            if (url == null) {
                message = "The data could not be imported.";
            } else if ("file".equalsIgnoreCase(url.getProtocol())) {
                String fileName = FileUtils.getFileName(url.getFile());
                message = "The file \"" + fileName + "\" could not be imported.";
            } else {
                String fileName = FileUtils.getFileName(url.toExternalForm());
                message = "The data  \"" + fileName + "\" could not be imported.";
            }
            ika.utils.ErrorDialog.showErrorDialog(message, "Import Error", exc, null);
        }

        if (exc != null) {
            //exc.printStackTrace();

        }
    }

    public boolean isShowMessageOnError() {
        return showMessageOnError;
    }

    public void setShowMessageOnError(boolean showMessageOnError) {
        this.showMessageOnError = showMessageOnError;
    }

    public boolean hasReceivedError() {
        return hasReceivedError;
    }

}
