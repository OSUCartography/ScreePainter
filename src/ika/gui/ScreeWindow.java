/*
 * ScreeWindow.java
 *
 */
package ika.gui;

import ika.app.ApplicationInfo;
import ika.app.SwissLV95GeospatialPDFExport;
import ika.app.ScreeDataFilePaths;
import ika.app.ScreeGenerator;
import ika.app.ScreeGeneratorManager;
import ika.geo.*;
import ika.geoexport.*;
import ika.utils.*;
import ika.map.tools.*;
import java.awt.*;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Properties;
import javax.swing.*;

/**
 * Scree Painter main window.
 * @author  Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ScreeWindow extends MainWindow {

    /**
     * path to the default settings
     */
    private static final String SETTINGS_FILE_PATH = "/ika/app/settings.txt";

    /**
     * Object responsible for generating scree dots and gully lines.
     */
    private ScreeGenerator screeGenerator;

    /**
     * backgroundGeoSet is added to the map and contains all images, of which
     * only one can be visible.
     */
    private GeoSet backgroundGeoSet;

    /**
     * The area of interest that is filled with screed dots.
     */
    private GeoPath areaOfInterest;

    /**
     * file path to the input data sets.
     */
    private ScreeDataFilePaths screeDataFilePaths = new ScreeDataFilePaths();

    /**
     * The panel on the left side of this window.
     */
    private ScreeParametersPanel screeParametersPanel = new ScreeParametersPanel();

    /**
     * A string reporting on the last scree generation.
     */
    private String screeGenerationReport = null;
    
    /**
     * Creates new form
     */
    public ScreeWindow() {

        // build the GUI
        this.initComponents();

        Dimension dim = areaToggleButton.getSize();
        areaToggleButton.setMinimumSize(dim);
        areaToggleButton.setPreferredSize(dim);

        if (ika.utils.Sys.isWindows()) {
            this.menuBar.remove(macHelpMenu);
        }
        this.initMenusForMac();
    }

    @Override
    protected boolean init() {

        // initialize the map
        this.mapComponent.setGeoSet(new GeoMap());
        this.mapComponent.setCoordinateFormatter(new CoordinateFormatter("###,##0.#", "###,##0.#", 1));
        this.mapComponent.setMapTool(new ScaleMoveSelectionTool(this.mapComponent));

        // add a MapEventListener: When the map changes, the dirty
        // flag is set and the Save menu item updated.
        MapEventListener mel = new MapEventListener() {

            @Override
            public void mapEvent(MapEvent evt) {
                setDocumentDirty();
                updateAllMenus();
            }
        };
        // register the MapEventListener to be informed whenever the map changes.
        GeoSetBroadcaster.addMapEventListener(mel, this.mapComponent.getGeoSet());

        // register the coordinate info panel with the map
        this.coordinateInfoPanel.registerWithMapComponent(this.mapComponent);

        // register the scale info panel with the map
        this.scaleLabel.registerWithMapComponent(this.mapComponent);

        // maximise the size of this window. Fill the primary screen.
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.validate();

        // add a window listener that updates the menus when the
        // state of the window changes (minimized, close, focus lost, activated, etc.)
        WindowListener windowListener = new WindowListener() {

            public void windowChanged(WindowEvent e) {
                ScreeWindow mainWindow = (ScreeWindow) e.getWindow();
                mainWindow.updateAllMenus();
                MacWindowsManager.updateFramelessMenuBar();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                this.windowChanged(e);
            }

            @Override
            public void windowClosing(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
                this.windowChanged(e);
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                this.windowChanged(e);
            }

            @Override
            public void windowActivated(WindowEvent e) {
                this.windowChanged(e);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                this.windowChanged(e);
            }
        };
        this.addWindowListener(windowListener);

        // initialize the undo/redo manager with the current (empty) map content.
        this.mapComponent.addUndo(null);

        this.getRootPane().addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            @Override
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                windowModifiedPropertyChange(evt);
            }
        });

        // background container that will contain the raster images
        this.backgroundGeoSet = new GeoSet();
        this.backgroundGeoSet.setSelectable(false);
        this.mapComponent.addGeoObject(this.backgroundGeoSet, false);

        this.screeGenerator = new ScreeGenerator();

        PageFormat pageFormat = this.mapComponent.getPageFormat();
        pageFormat.setAutomatic(true);
        pageFormat.setUnitPixels(false);
        pageFormat.setPageScale(25000);
        pageFormat.setVisible(false);

        if (!showScreeDataDialog(true)) {
            return false;
        }

        this.synchronizeBackgroundImageWithMenu();

        screeParametersPanel.setParameters(screeGenerator.p, screeGenerator.screeData);
        leftPanel.add(screeParametersPanel);
        this.validate();

        centerAreaOfInterest();


        this.mapComponent.addMouseMotionListener(new MapToolMouseMotionListener() {

            final DecimalFormat format = new DecimalFormat("0.#\u00B0");

            @Override
            public void mouseMoved(Double point, MapComponent mapComponent) {
                if (point != null && screeGenerator != null
                        && screeGenerator.screeData != null) {
                
                    // update highlighted value in the histograms
                    if (screeGenerator.screeData.shadingImage != null) {
                        GeoImage shading = screeGenerator.screeData.shadingImage;
                        int shadingVal = -1;
                        if (shading != null) {
                            shadingVal = shading.getNearestGrayNeighbor(point.x, point.y);
                            // if outside of shading image, shadingVal is -1
                        }

                        GeoImage gradMask = screeGenerator.screeData.shadingGradationMaskImage;
                        int mask = -1;
                        if (gradMask != null) {
                            mask = gradMask.getNearestGrayNeighbor(point.x, point.y);
                            // if outside of mask image, mask is -1
                        }
                        screeParametersPanel.setHistogramHighlight(shadingVal, mask);
                    }
                    
                    // update slope display
                    if (screeGenerator.screeData.hasDEM()) {
                        final GeoGrid dem = screeGenerator.screeData.dem;
                        final double slope = dem.getSlope(point.x, point.y);
                        if (java.lang.Double.isNaN(slope)) {
                            slopeLabel.setText("-");
                        } else {
                            slopeLabel.setText(format.format(Math.toDegrees(slope)));
                        }
                    }

                }
            }
        });

        // read default settings from JAR
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            URL url = this.getClass().getResource(SETTINGS_FILE_PATH);
            BufferedInputStream bis = new BufferedInputStream(url.openStream());
            InputStreamReader isr = new InputStreamReader(bis, "UTF-8");
            reader = new BufferedReader(isr);
            char[] chars = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(chars)) > -1) {
                sb.append(String.valueOf(chars));
            }
            screeGenerator.p.fromString(sb.toString());
            screeParametersPanel.writeGUI();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        mapComponent.showAll();

        return true;

    }


    @Override
    protected String getWindowTitle(int windowNumber) {
        return ika.app.ApplicationInfo.getApplicationName() + " " + windowNumber;
    }

    @Override
    protected boolean canDocumentBeClosed() {

        String msg = "<html>Do you want to save the changes you made" +
                "<br>to the current settings?</html>";
        switch (SaveFilePanel.showSaveDialog(this, msg)) {
            case DONTSAVE:
                // document has possibly been edited but user does not want to save it
                return true;

            case CANCEL:
                // document has been edited and user canceled
                return false;

            case SAVE:
                // document has been edited and user wants to save it
                return screeParametersPanel.saveSettings();
        }
        return false;
    }

    private void centerAreaOfInterest() {
        mapComponent.getGeoSet().remove(areaOfInterest);

        Rectangle2D bounds = null;
        if (screeGenerator.screeData.screePolygons.getNumberOfChildren() > 0) {
            bounds = screeGenerator.screeData.screePolygons.getBounds2D(GeoObject.UNDEFINED_SCALE);
        }

        if (bounds != null) {
            areaOfInterest = GeoPath.newRect(bounds);
            areaOfInterest.scale(0.5, bounds.getCenterX(), bounds.getCenterY());
            VectorSymbol symbol = new VectorSymbol(null, Color.RED, 5);
            symbol.setScaleInvariant(true);
            areaOfInterest.setVectorSymbol(symbol);
            areaOfInterest.setVisible(!areaToggleButton.isSelected());
            mapComponent.addGeoObject(areaOfInterest, false);
        }
    }
    private boolean showScreeDataDialog(boolean showCancelButton) {

        if (!ScreeDataPanel.showDialog(this,
                screeDataFilePaths,
                screeGenerator.screeData,
                backgroundGeoSet,
                mapComponent.getGeoSet(),
                showCancelButton)) {
            return false;
        }

        // update the area of interest
        centerAreaOfInterest();

        // remove existing scree
        screeGenerator.screeData.screeStones.removeAllGeoObjects();
        
        // remove existing gully lines
        if (!screeGenerator.screeData.fixedScreeLines) {
            screeGenerator.screeData.gullyLines.removeAllGeoObjects();
        }

        // update the shading histogram
        int[] histo = null;
        if (screeGenerator.screeData.shadingImage != null) {
            histo = screeGenerator.screeData.shadingImage.getHistogram();
        }
        screeParametersPanel.setShadingHistogram(histo);

        if (screeGenerator.screeData.shadingGradationMaskImage != null) {
            histo = screeGenerator.screeData.shadingGradationMaskImage.getHistogram();
        } else {
            histo = null;
        }
        screeParametersPanel.setGradationMaskHistogram(histo);
        
        // update the enabled state of the items in the view menu
        synchronizeBackgroundImageWithMenu();
        // synchronize visibility of gully lines and scree polygons with view menu
        boolean viewGullies = viewGullyLinesCheckBoxMenuItem.isSelected();
        screeGenerator.screeData.gullyLines.setVisible(viewGullies);
        boolean viewPolygons = viewPolygonsCheckBoxMenuItem.isSelected();
        screeGenerator.screeData.screePolygons.setVisible(viewPolygons);
        
        // update enable state of gully lines controls
        this.screeParametersPanel.writeGUI();


        // show all visible data
        mapComponent.showAll();
        return true;

    }

       private class ScreeWorker extends ika.gui.SwingWorkerWithProgressIndicator {

        private Rectangle2D screeBB = null;
        private boolean generateScreeStones = true;
        private MapEventTrigger trigger;

        public ScreeWorker(Frame owner) {
            super(owner, "Scree Painter", "Generating scree...", true);
        }

        @Override
        protected Object doInBackground() throws Exception {

            ScreeGeneratorManager manager = new ScreeGeneratorManager();
            screeGenerationReport = null;
            try {
                manager.generateScree(screeGenerator, screeBB, this, generateScreeStones);
            } catch (Throwable exc) {
                String msg = "Scree could not be generated completely.";
                if (exc instanceof java.lang.OutOfMemoryError) {
                    msg += "\nThere is not enough memory available.";
                    msg += "\nTry using a smaller Update Area.";
                }
                String title = "Scree Painter Error";
                ika.utils.ErrorDialog.showErrorDialog(msg, title, exc, owner);
                exc.printStackTrace();
            } finally {
                screeGenerationReport = manager.getHTMLReportForLastGeneration();
                this.complete();
            }

            return null;
        }

        private void generateScree() {
            generateScreeStones = true;
            this.generate();
        }

        private void generateOnlyGullyLinesForAllPolygons() {
            generateScreeStones = false;
            this.generate();
        }

        private void generate() {

            if (!areaToggleButton.isSelected()) {
                screeBB = areaOfInterest.getBounds2D(GeoObject.UNDEFINED_SCALE);
            } else {
                screeBB = null;
            }

            this.progress(0);
            this.setMaxTimeWithoutDialog(0);
            this.setIndeterminate(true);
            this.setTotalTasksCount(1);
            this.disableCancel();
            this.setMessage("Generating scree...");

            // block map events
            trigger = new MapEventTrigger(mapComponent.getGeoSet());
            
            // remove features created last time
            mapComponent.getGeoSet().remove(screeGenerator.screeData.screeStones);
            if (!screeGenerator.screeData.fixedScreeLines) {
                screeGenerator.screeData.gullyLines.removeAllGeoObjects(); // FIXME
            }
            
            // create new features
            this.execute();
        }

        @Override
        protected void done() {
            try {
                viewScreeCheckBoxMenuItem.setSelected(true);
                screeGenerator.screeData.screeStones.setVisible(true);
                mapComponent.getGeoSet().add(screeGenerator.screeData.screeStones);
            } finally {
                // enable map events again
                trigger.inform();
            }
        }
    }

    private void generateScree() {
        new ScreeWorker(this).generateScree();
    }

    /**
     * Mac OS X specific initialization.
     */
    private void initMenusForMac() {
        if (ika.utils.Sys.isMacOSX()) {

            // remove exit menu item on Mac OS X
            this.fileMenu.remove(this.exitMenuSeparator);
            this.fileMenu.remove(this.exitMenuItem);
            this.fileMenu.validate();

            // remove info menu item on Mac OS X
            this.menuBar.remove(helpMenu);
            this.menuBar.validate();
        }
    }

    /**
     * Return a GeoMap that can be stored in an external file.
     * @return The document content.
     */
    @Override
    protected byte[] getDocumentData() {
        try {
            return ika.utils.Serializer.serialize(mapComponent.getGeoSet(), false);
        } catch (java.io.IOException exc) {
            exc.printStackTrace();
            return null;
        }
    }

    /**
     * Restore the document content from a passed GeoMap.
     * @param screeDataFilePaths The document content.
     */
    @Override
    protected void setDocumentData(byte[] data) throws Exception {
        GeoMap geoMap = (GeoMap) ika.utils.Serializer.deserialize(data, false);
        this.mapComponent.setGeoSet(geoMap);
    }

    /**
     * Update all menus of this window.
     */
    private void updateAllMenus() {
        // Only update the menu items if this frame is visible.
        // This avoids menu items being enabled that will be detached from
        // this frame and will be attached to a utility frame or will be
        // displayed when no frame is visible on Mac OS X.
        if (this.isVisible()) {
            this.updateFileMenu();
            this.updateScreeMenu();
            this.updateViewMenu();
            MainWindow.updateWindowMenu(this.windowMenu, this);
        }
    }

    /**
     * Update the enabled/disabled state of the items in the file menu.
     */
    private void updateFileMenu() {
        this.loadInputDataMenuItem.setEnabled(true);
        this.closeMenuItem.setEnabled(true);
        this.saveSettingsMenuItem.setEnabled(this.isDocumentDirty());
        this.loadSettingsMenuItem.setEnabled(true);
        this.exportScreeMenuItem.setEnabled(screeGenerator.screeData.hasScreeStones());
        this.exportGullyLinesMenuItem.setEnabled(screeGenerator.screeData.hasGullyLines());
    }

    private void updateScreeMenu() {
        this.updateScreeMenuItem.setEnabled(true);
        this.adjustUpdateAreaScreeMenuItem.setEnabled(true);
        this.generateScreeLinesMenuItem.setEnabled(true);
        this.loadInputDataMenuItem.setEnabled(true);
        this.reportMenuItem.setEnabled(screeGenerator.screeData.hasScreeStones()
                || screeGenerator.screeData.hasGullyLines());
    }

    /**
     * Update the enabled/disabled state of the items in the view menu.
     */
    private void updateViewMenu() {
        this.zoomInMenuItem.setEnabled(true);
        this.zoomOutMenuItem.setEnabled(true);
        this.zoomOnUpdateAreaMenuItem.setEnabled(true);
        this.showAllMenuItem.setEnabled(true);
        this.showPageCheckBoxMenuItem.setEnabled(true);
        this.toggleViewMenuItem.setEnabled(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        toolBarButtonGroup = new javax.swing.ButtonGroup();
        viewPopupMenu = new javax.swing.JPopupMenu();
        viewScreeCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        viewPolygonsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        viewGullyLinesCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator13 = new javax.swing.JSeparator();
        viewNoneCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        viewObstaclesCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        viewShadingCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        viewGradationMaskCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        viewLargeStonesMaskCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        viewReferenceCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        leftPanel = new javax.swing.JPanel();
        rightPanel = new javax.swing.JPanel();
        navigationToolBar = new javax.swing.JToolBar();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        arrowToggleButton = new javax.swing.JToggleButton();
        zoomInToggleButton = new javax.swing.JToggleButton();
        zoomOutToggleButton = new javax.swing.JToggleButton();
        handToggleButton = new javax.swing.JToggleButton();
        distanceToggleButton = new javax.swing.JToggleButton();
        showAllButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        updateButton = new javax.swing.JButton();
        areaToggleButton = new javax.swing.JToggleButton();
        dataButton = new javax.swing.JButton();
        viewMenuButton = new ika.gui.MenuToggleButton();
        infoPanel = new javax.swing.JPanel();
        scaleLabel = new ika.gui.ScaleLabel();
        coordinateInfoPanel = new ika.gui.CoordinateInfoPanel();
        slopeLabel = new ika.gui.ScaleLabel();
        mapComponent = new ika.gui.MapComponent();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        closeMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        saveSettingsMenuItem = new javax.swing.JMenuItem();
        loadSettingsMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        exportScreeMenuItem = new javax.swing.JMenuItem();
        exportGullyLinesMenuItem = new javax.swing.JMenuItem();
        exitMenuSeparator = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        screeMenu = new javax.swing.JMenu();
        loadInputDataMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        updateScreeMenuItem = new javax.swing.JMenuItem();
        generateScreeLinesMenuItem = new javax.swing.JMenuItem();
        adjustUpdateAreaScreeMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        reportMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        zoomInMenuItem = new javax.swing.JMenuItem();
        zoomOutMenuItem = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JSeparator();
        zoomOnUpdateAreaMenuItem = new javax.swing.JMenuItem();
        showAllMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        showPageCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        toggleViewMenuItem = new javax.swing.JMenuItem();
        windowMenu = new javax.swing.JMenu();
        minimizeMenuItem = new javax.swing.JMenuItem();
        zoomMenuItem = new javax.swing.JMenuItem();
        windowSeparator = new javax.swing.JSeparator();
        helpMenu = new javax.swing.JMenu();
        winHelpMenuItem = new javax.swing.JMenuItem();
        infoMenuItem = new javax.swing.JMenuItem();
        macHelpMenu = new javax.swing.JMenu();
        macInfoMenuItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator3 = new javax.swing.JPopupMenu.Separator();
        macHelpMenuItem = new javax.swing.JMenuItem();

        viewScreeCheckBoxMenuItem.setSelected(true);
        viewScreeCheckBoxMenuItem.setText("Scree");
        viewScreeCheckBoxMenuItem.setToolTipText("Show or hide the generated scree dots.");
        viewScreeCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewScreeCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewPopupMenu.add(viewScreeCheckBoxMenuItem);

        viewPolygonsCheckBoxMenuItem.setText("Scree Polygons");
        viewPolygonsCheckBoxMenuItem.setToolTipText("The polyongs to fill with scree.");
        viewPolygonsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewPolygonsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewPopupMenu.add(viewPolygonsCheckBoxMenuItem);

        viewGullyLinesCheckBoxMenuItem.setText("Gully Lines");
        viewGullyLinesCheckBoxMenuItem.setToolTipText("Gully lines symbolized by braids of stones.");
        viewGullyLinesCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewGullyLinesCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewPopupMenu.add(viewGullyLinesCheckBoxMenuItem);
        viewPopupMenu.add(jSeparator13);

        viewNoneCheckBoxMenuItem.setText("<No Background Image>");
        viewNoneCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewNoneCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewPopupMenu.add(viewNoneCheckBoxMenuItem);

        viewObstaclesCheckBoxMenuItem.setSelected(true);
        viewObstaclesCheckBoxMenuItem.setText("Obstacles Mask");
        viewObstaclesCheckBoxMenuItem.setToolTipText("Stones are not placed where this image is black.");
        viewObstaclesCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewObstaclesCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewPopupMenu.add(viewObstaclesCheckBoxMenuItem);

        viewShadingCheckBoxMenuItem.setText("Shaded Relief");
        viewShadingCheckBoxMenuItem.setToolTipText("The size and density of stones varies with the brightness of this image.");
        viewShadingCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewShadingCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewPopupMenu.add(viewShadingCheckBoxMenuItem);

        viewGradationMaskCheckBoxMenuItem.setText("Gradation Mask");
        viewGradationMaskCheckBoxMenuItem.setToolTipText("An alternative gradation curve is applied where this image is dark.");
        viewGradationMaskCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewGradationMaskCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewPopupMenu.add(viewGradationMaskCheckBoxMenuItem);

        viewLargeStonesMaskCheckBoxMenuItem.setText("Large Stones Mask");
        viewLargeStonesMaskCheckBoxMenuItem.setToolTipText("Stones are enlarged where this mask is dark.");
        viewLargeStonesMaskCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewLargeStonesMaskCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewPopupMenu.add(viewLargeStonesMaskCheckBoxMenuItem);

        viewReferenceCheckBoxMenuItem.setText("Reference Image");
        viewReferenceCheckBoxMenuItem.setToolTipText("This image is not used for scree generation.");
        viewReferenceCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewReferenceCheckBoxMenuItemActionPerformed(evt);
            }
        });
        viewPopupMenu.add(viewReferenceCheckBoxMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setName(""); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeWindow(evt);
            }
        });
        getContentPane().add(leftPanel, java.awt.BorderLayout.WEST);

        rightPanel.setLayout(new java.awt.BorderLayout());

        navigationToolBar.setFloatable(false);
        navigationToolBar.setRollover(true);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.GridBagLayout());

        toolBarButtonGroup.add(arrowToggleButton);
        arrowToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/Arrow16x16.gif"))); // NOI18N
        arrowToggleButton.setSelected(true);
        arrowToggleButton.setToolTipText("Select, move and scale objects.");
        arrowToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        arrowToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        arrowToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        arrowToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                arrowToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(arrowToggleButton);

        toolBarButtonGroup.add(zoomInToggleButton);
        zoomInToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/ZoomIn16x16.gif"))); // NOI18N
        zoomInToggleButton.setToolTipText("Zoom In");
        zoomInToggleButton.setFocusable(false);
        zoomInToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomInToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        zoomInToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zoomInToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomInToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(zoomInToggleButton);

        toolBarButtonGroup.add(zoomOutToggleButton);
        zoomOutToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/ZoomOut16x16.gif"))); // NOI18N
        zoomOutToggleButton.setToolTipText("Zoom Out");
        zoomOutToggleButton.setFocusable(false);
        zoomOutToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomOutToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        zoomOutToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zoomOutToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOutToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(zoomOutToggleButton);

        toolBarButtonGroup.add(handToggleButton);
        handToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/Hand16x16.gif"))); // NOI18N
        handToggleButton.setToolTipText("Pan");
        handToggleButton.setFocusable(false);
        handToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        handToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        handToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        handToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(handToggleButton);

        toolBarButtonGroup.add(distanceToggleButton);
        distanceToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/Ruler16x16.gif"))); // NOI18N
        distanceToggleButton.setToolTipText("Measure Distance and Angle");
        distanceToggleButton.setFocusable(false);
        distanceToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        distanceToggleButton.setPreferredSize(new java.awt.Dimension(24, 24));
        distanceToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        distanceToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                distanceToggleButtonActionPerformed(evt);
            }
        });
        jPanel2.add(distanceToggleButton);

        showAllButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/ShowAll20x14.png"))); // NOI18N
        showAllButton.setToolTipText("Show All");
        showAllButton.setBorderPainted(false);
        showAllButton.setContentAreaFilled(false);
        showAllButton.setFocusable(false);
        showAllButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        showAllButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        showAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAllButtonActionPerformed(evt);
            }
        });
        jPanel2.add(showAllButton);

        jPanel4.add(jPanel2, new java.awt.GridBagConstraints());

        jPanel1.add(jPanel4, java.awt.BorderLayout.WEST);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 0));

        updateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/update2.png"))); // NOI18N
        updateButton.setText("Update");
        updateButton.setToolTipText("Generate scree stones.");
        updateButton.setBorderPainted(false);
        updateButton.setContentAreaFilled(false);
        updateButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        updateButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });
        jPanel3.add(updateButton);

        areaToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/redbox.png"))); // NOI18N
        areaToggleButton.setText("Update Area");
        areaToggleButton.setBorderPainted(false);
        areaToggleButton.setComponentPopupMenu(viewPopupMenu);
        areaToggleButton.setContentAreaFilled(false);
        areaToggleButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        areaToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        areaToggleButton.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/showall.png"))); // NOI18N
        areaToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        areaToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                areaToggleButtonActionPerformed(evt);
            }
        });
        jPanel3.add(areaToggleButton);

        dataButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/file.png"))); // NOI18N
        dataButton.setText("Data");
        dataButton.setToolTipText("Select the input data.");
        dataButton.setBorderPainted(false);
        dataButton.setContentAreaFilled(false);
        dataButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        dataButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        dataButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dataButtonActionPerformed(evt);
            }
        });
        jPanel3.add(dataButton);

        viewMenuButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/view.png"))); // NOI18N
        viewMenuButton.setText("View");
        viewMenuButton.setToolTipText("Select the information layers displayed in the map.");
        viewMenuButton.setBorderPainted(false);
        viewMenuButton.setContentAreaFilled(false);
        viewMenuButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        viewMenuButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        viewMenuButton.setPopupMenu(viewPopupMenu);
        jPanel3.add(viewMenuButton);

        jPanel1.add(jPanel3, java.awt.BorderLayout.CENTER);

        infoPanel.setLayout(new java.awt.GridBagLayout());

        scaleLabel.setMaximumSize(new java.awt.Dimension(150, 12));
        scaleLabel.setMinimumSize(new java.awt.Dimension(50, 20));
        scaleLabel.setPreferredSize(new java.awt.Dimension(80, 12));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        infoPanel.add(scaleLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        infoPanel.add(coordinateInfoPanel, gridBagConstraints);

        slopeLabel.setMaximumSize(new java.awt.Dimension(150, 12));
        slopeLabel.setMinimumSize(new java.awt.Dimension(50, 20));
        slopeLabel.setPreferredSize(new java.awt.Dimension(80, 12));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        infoPanel.add(slopeLabel, gridBagConstraints);

        jPanel1.add(infoPanel, java.awt.BorderLayout.EAST);

        navigationToolBar.add(jPanel1);

        rightPanel.add(navigationToolBar, java.awt.BorderLayout.NORTH);

        mapComponent.setBackground(new java.awt.Color(255, 255, 255));
        mapComponent.setInfoString("");
        mapComponent.setMinimumSize(new java.awt.Dimension(100, 200));
        mapComponent.setPreferredSize(new java.awt.Dimension(200, 200));
        rightPanel.add(mapComponent, java.awt.BorderLayout.CENTER);

        getContentPane().add(rightPanel, java.awt.BorderLayout.CENTER);

        fileMenu.setText("File");

        newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N,
            java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    newMenuItem.setText("New");
    newMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            newMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(newMenuItem);

    closeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
closeMenuItem.setText("Close");
closeMenuItem.setEnabled(false);
closeMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        closeMenuItemActionPerformed(evt);
    }
    });
    fileMenu.add(closeMenuItem);
    fileMenu.add(jSeparator5);

    saveSettingsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
        | java.awt.event.InputEvent.SHIFT_MASK));
saveSettingsMenuItem.setText("Save Settings...");
saveSettingsMenuItem.setEnabled(false);
saveSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        saveSettingsMenuItemActionPerformed(evt);
    }
    });
    fileMenu.add(saveSettingsMenuItem);

    loadSettingsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
loadSettingsMenuItem.setText("Load Settings…");
loadSettingsMenuItem.setEnabled(false);
loadSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        loadSettingsMenuItemActionPerformed(evt);
    }
    });
    fileMenu.add(loadSettingsMenuItem);
    fileMenu.add(jSeparator1);

    exportScreeMenuItem.setText("Export Scree…");
    exportScreeMenuItem.setEnabled(false);
    exportScreeMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            exportScreeMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(exportScreeMenuItem);

    exportGullyLinesMenuItem.setText("Export Gully Lines…");
    exportGullyLinesMenuItem.setEnabled(false);
    exportGullyLinesMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            exportGullyLinesMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(exportGullyLinesMenuItem);
    fileMenu.add(exitMenuSeparator);

    exitMenuItem.setText("Exit");
    exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            exitMenuItemActionPerformed(evt);
        }
    });
    fileMenu.add(exitMenuItem);

    menuBar.add(fileMenu);

    screeMenu.setText("Scree");

    loadInputDataMenuItem.setText("Scree Input Data…");
    loadInputDataMenuItem.setEnabled(false);
    loadInputDataMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadInputDataMenuItemActionPerformed(evt);
        }
    });
    screeMenu.add(loadInputDataMenuItem);
    screeMenu.add(jSeparator4);

    updateScreeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
updateScreeMenuItem.setText("Update Scree");
updateScreeMenuItem.setEnabled(false);
updateScreeMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        updateScreeMenuItemActionPerformed(evt);
    }
    });
    screeMenu.add(updateScreeMenuItem);

    generateScreeLinesMenuItem.setText("Generate Gully Lines");
    generateScreeLinesMenuItem.setEnabled(false);
    generateScreeLinesMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            generateScreeLinesMenuItemActionPerformed(evt);
        }
    });
    screeMenu.add(generateScreeLinesMenuItem);

    adjustUpdateAreaScreeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
adjustUpdateAreaScreeMenuItem.setText("Adjust Update Area to Visible Area");
adjustUpdateAreaScreeMenuItem.setEnabled(false);
adjustUpdateAreaScreeMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        adjustUpdateAreaScreeMenuItemActionPerformed(evt);
    }
    });
    screeMenu.add(adjustUpdateAreaScreeMenuItem);
    screeMenu.add(jSeparator2);

    reportMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
reportMenuItem.setText("Show Report…");
reportMenuItem.setEnabled(false);
reportMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        reportMenuItemActionPerformed(evt);
    }
    });
    screeMenu.add(reportMenuItem);

    menuBar.add(screeMenu);

    viewMenu.setText("View");

    zoomInMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ADD,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
zoomInMenuItem.setText("Zoom In");
zoomInMenuItem.setEnabled(false);
zoomInMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        zoomInMenuItemActionPerformed(evt);
    }
    });
    viewMenu.add(zoomInMenuItem);

    zoomOutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SUBTRACT,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
zoomOutMenuItem.setText("Zoom Out");
zoomOutMenuItem.setEnabled(false);
zoomOutMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        zoomOutMenuItemActionPerformed(evt);
    }
    });
    viewMenu.add(zoomOutMenuItem);
    viewMenu.add(jSeparator12);

    zoomOnUpdateAreaMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
zoomOnUpdateAreaMenuItem.setText("Zoom On Updatea Area");
zoomOnUpdateAreaMenuItem.setEnabled(false);
zoomOnUpdateAreaMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        zoomOnUpdateAreaMenuItemActionPerformed(evt);
    }
    });
    viewMenu.add(zoomOnUpdateAreaMenuItem);

    showAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_NUMPAD0,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
showAllMenuItem.setText("Show All");
showAllMenuItem.setEnabled(false);
showAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        showAllMenuItemActionPerformed(evt);
    }
    });
    viewMenu.add(showAllMenuItem);
    viewMenu.add(jSeparator8);

    showPageCheckBoxMenuItem.setText("Show Map Outline");
    showPageCheckBoxMenuItem.setEnabled(false);
    showPageCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            showPageCheckBoxMenuItemActionPerformed(evt);
        }
    });
    viewMenu.add(showPageCheckBoxMenuItem);

    toggleViewMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
toggleViewMenuItem.setText("Toggle Reference Image and Obstacles Mask");
toggleViewMenuItem.setEnabled(false);
toggleViewMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        toggleViewMenuItemActionPerformed(evt);
    }
    });
    viewMenu.add(toggleViewMenuItem);

    menuBar.add(viewMenu);

    windowMenu.setText("Window");
    windowMenu.setName("WindowsMenu"); // NOI18N

    minimizeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M,
        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
minimizeMenuItem.setText("Minimize");
minimizeMenuItem.setEnabled(false);
minimizeMenuItem.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        minimizeMenuItemActionPerformed(evt);
    }
    });
    windowMenu.add(minimizeMenuItem);

    zoomMenuItem.setText("Zoom");
    zoomMenuItem.setEnabled(false);
    zoomMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            zoomMenuItemActionPerformed(evt);
        }
    });
    windowMenu.add(zoomMenuItem);
    windowMenu.add(windowSeparator);

    menuBar.add(windowMenu);

    helpMenu.setText("?");

    winHelpMenuItem.setText("Scree Painter Online Help");
    winHelpMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            helpMenuItemActionPerformed(evt);
        }
    });
    helpMenu.add(winHelpMenuItem);

    infoMenuItem.setText("Info");
    infoMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            infoMenuItemActionPerformed(evt);
        }
    });
    helpMenu.add(infoMenuItem);

    menuBar.add(helpMenu);

    macHelpMenu.setText("Help");

    macInfoMenuItem.setText("Info");
    macInfoMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            macInfoMenuItemActionPerformed(evt);
        }
    });
    macHelpMenu.add(macInfoMenuItem);
    macHelpMenu.add(jSeparator3);

    macHelpMenuItem.setText("Scree Painter Online Help");
    macHelpMenuItem.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            helpMenuItemActionPerformed(evt);
        }
    });
    macHelpMenu.add(macHelpMenuItem);

    menuBar.add(macHelpMenu);

    setJMenuBar(menuBar);

    pack();
    }// </editor-fold>//GEN-END:initComponents

    private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
        ScreeWindow.newDocumentWindow();
    }//GEN-LAST:event_newMenuItemActionPerformed

    private void loadSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadSettingsMenuItemActionPerformed
        try {
            Frame frame = ika.gui.GUIUtil.getOwnerFrame(this);
            String filePath = FileUtils.askFile(frame, "Load Scree Painter Settings", true);
            if (filePath != null) {
                screeParametersPanel.loadSettings(filePath);
                setTitle(FileUtils.getFileNameWithoutExtension(filePath));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ika.utils.ErrorDialog.showErrorDialog("The settings could not be loaded.", ex);
        }
        
}//GEN-LAST:event_loadSettingsMenuItemActionPerformed

    private void closeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeMenuItemActionPerformed
        this.closeDocumentWindow();
    }//GEN-LAST:event_closeMenuItemActionPerformed

    private void saveSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSettingsMenuItemActionPerformed
        screeParametersPanel.saveSettings();
}//GEN-LAST:event_saveSettingsMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        // this handler is not used on Macintosh. On Windows and other platforms
        // only this window is closed.
        this.closeDocumentWindow();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void zoomInMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomInMenuItemActionPerformed
        this.mapComponent.zoomIn();
    }//GEN-LAST:event_zoomInMenuItemActionPerformed

    private void zoomOutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOutMenuItemActionPerformed
        this.mapComponent.zoomOut();
    }//GEN-LAST:event_zoomOutMenuItemActionPerformed

    private void showAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAllMenuItemActionPerformed
        mapComponent.showAll();
    }//GEN-LAST:event_showAllMenuItemActionPerformed

    private void showPageCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showPageCheckBoxMenuItemActionPerformed
        boolean show = this.showPageCheckBoxMenuItem.isSelected();
        this.mapComponent.getPageFormat().setVisible(show);
    }//GEN-LAST:event_showPageCheckBoxMenuItemActionPerformed

    private void minimizeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_minimizeMenuItemActionPerformed
        this.setState(Frame.ICONIFIED);
    }//GEN-LAST:event_minimizeMenuItemActionPerformed

    private void zoomMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomMenuItemActionPerformed
        if ((this.getExtendedState() & Frame.MAXIMIZED_BOTH) != MAXIMIZED_BOTH) {
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            this.setExtendedState(JFrame.NORMAL);
        }
        this.validate();
    }//GEN-LAST:event_zoomMenuItemActionPerformed

    private void infoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoMenuItemActionPerformed
        ika.gui.ProgramInfoPanel.showApplicationInfo();
    }//GEN-LAST:event_infoMenuItemActionPerformed

    private void arrowToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_arrowToggleButtonActionPerformed
        this.mapComponent.setMapTool(new ScaleMoveSelectionTool(this.mapComponent));
    }//GEN-LAST:event_arrowToggleButtonActionPerformed

    private void zoomInToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomInToggleButtonActionPerformed
        this.mapComponent.setMapTool(new ZoomInTool(this.mapComponent));
    }//GEN-LAST:event_zoomInToggleButtonActionPerformed

    private void zoomOutToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOutToggleButtonActionPerformed
        this.mapComponent.setMapTool(new ZoomOutTool(this.mapComponent));
    }//GEN-LAST:event_zoomOutToggleButtonActionPerformed

    private void handToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_handToggleButtonActionPerformed
        this.mapComponent.setMapTool(new PanTool(this.mapComponent));
    }//GEN-LAST:event_handToggleButtonActionPerformed

    private void distanceToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_distanceToggleButtonActionPerformed
        MeasureTool tool = new MeasureTool(this.mapComponent);
        tool.addMeasureToolListener(this.coordinateInfoPanel);
        this.mapComponent.setMapTool(tool);
    }//GEN-LAST:event_distanceToggleButtonActionPerformed

    private void showAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAllButtonActionPerformed
        this.mapComponent.showAll();
    }//GEN-LAST:event_showAllButtonActionPerformed

    private void closeWindow(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeWindow
        this.closeDocumentWindow();
    }//GEN-LAST:event_closeWindow

    private void initGeospatialPDFExporter(GeospatialPDFExporter geospatialPDFExporter,
            PageFormat pageFormat) {
        geospatialPDFExporter.setWKT(SwissLV95GeospatialPDFExport.wkt);
        float[] corners = SwissLV95GeospatialPDFExport.lonLatCornerPoints(pageFormat);
        if (corners == null) {
            ErrorDialog.showErrorDialog("An error occured when initializing the "
                    + "Swiss LV95 Coordinate System", this);
            return;
        }
        geospatialPDFExporter.setLonLatCornerPoints(corners);
    }
        
    private void exportScreeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportScreeMenuItemActionPerformed

        GeoSetExporter exporter = GeoExportGUI.askExporter(this);
        if (exporter == null) {
            return;
        }

        PageFormat pageFormat = this.mapComponent.getPageFormat();
        if (exporter instanceof VectorGraphicsExporter) {
            pageFormat.setPageScale(this.screeGenerator.p.mapScale);
            PageFormatDialog.setPageFormat(this, pageFormat);
            pageFormat = PageFormatDialog.showDialog(this, false);
            if (pageFormat == null) {
                return; // user canceled
            }
            if (pageFormat.isVisible()) {
                this.mapComponent.repaint();
            }
        }

        exporter.setDisplayMapScale(this.screeGenerator.p.mapScale);
        
        
        if (exporter instanceof ShapeExporter) {
            ((ShapeExporter)exporter).setShapeType(ShapeGeometryExporter.POLYGON_SHAPE_TYPE);
        }
        
        // for Swiss LV95 coordinate system only
        if (exporter instanceof GeospatialPDFExporter) {
            GeospatialPDFExporter geospatialPDFExporter = (GeospatialPDFExporter)exporter;
            initGeospatialPDFExporter(geospatialPDFExporter, pageFormat);
        }
        
        // screeGenerator.screeData.screeStones contains ScreeGenerator.Stone,
        // a class that derives from GeoObject but is not usually supported by
        // exporters. The stones could be converted to GeoPaths using
        // stone.toGeoPath, however, this would multiply the amount of memory
        // required to store the graphics. The exporters have therefore each
        // been hacked to call stone.toGeoPath and then using the standard
        // export routines for GeoPaths.
        
        GeoExportGUI.export(exporter,
                screeGenerator.screeData.screeStones,
                this.getTitle(),
                this,
                pageFormat,
                false,
                ApplicationInfo.getApplicationName(),
                this.getTitle(),
                System.getProperty("user.name"),
                "scree",
                "",
                null);

}//GEN-LAST:event_exportScreeMenuItemActionPerformed

    private void loadInputDataMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadInputDataMenuItemActionPerformed
        showScreeDataDialog(false);
}//GEN-LAST:event_loadInputDataMenuItemActionPerformed

private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed
    screeParametersPanel.readGUI();
    generateScree();
}//GEN-LAST:event_updateButtonActionPerformed

private void viewScreeCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewScreeCheckBoxMenuItemActionPerformed
    screeGenerator.screeData.screeStones.setVisible(viewScreeCheckBoxMenuItem.isSelected());
}//GEN-LAST:event_viewScreeCheckBoxMenuItemActionPerformed

private void viewPolygonsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewPolygonsCheckBoxMenuItemActionPerformed
    screeGenerator.screeData.screePolygons.setVisible(viewPolygonsCheckBoxMenuItem.isSelected());
}//GEN-LAST:event_viewPolygonsCheckBoxMenuItemActionPerformed

    private void synchronizeBackgroundImageWithMenu() {
        GeoObject obj;

        obj = screeGenerator.screeData.obstaclesMaskImage;
        this.viewObstaclesCheckBoxMenuItem.setEnabled(obj != null);
        if (obj != null) {
            obj.setVisible(viewObstaclesCheckBoxMenuItem.isSelected());
        }

        obj = screeGenerator.screeData.shadingImage;
        this.viewShadingCheckBoxMenuItem.setEnabled(obj != null);
        if (obj != null) {
            obj.setVisible(viewShadingCheckBoxMenuItem.isSelected());
        }

        obj = screeGenerator.screeData.shadingGradationMaskImage;
        this.viewGradationMaskCheckBoxMenuItem.setEnabled(obj != null);
        if (obj != null) {
            obj.setVisible(viewGradationMaskCheckBoxMenuItem.isSelected());
        }
        
        obj = screeGenerator.screeData.largeStoneMaskImage;
        this.viewLargeStonesMaskCheckBoxMenuItem.setEnabled(obj != null);
        if (obj != null) {
            obj.setVisible(viewLargeStonesMaskCheckBoxMenuItem.isSelected());
        }

        obj = screeGenerator.screeData.referenceImage;
        this.viewReferenceCheckBoxMenuItem.setEnabled(obj != null);
        if (obj != null) {
            obj.setVisible(viewReferenceCheckBoxMenuItem.isSelected());
        }

        viewNoneCheckBoxMenuItem.setEnabled(!viewNoneCheckBoxMenuItem.isSelected());

    }

    private void viewObstaclesCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewObstaclesCheckBoxMenuItemActionPerformed
        viewNoneCheckBoxMenuItem.setSelected(!viewObstaclesCheckBoxMenuItem.isSelected());

        viewShadingCheckBoxMenuItem.setSelected(false);
        viewLargeStonesMaskCheckBoxMenuItem.setSelected(false);
        viewGradationMaskCheckBoxMenuItem.setSelected(false);
        viewReferenceCheckBoxMenuItem.setSelected(false);
        synchronizeBackgroundImageWithMenu();
}//GEN-LAST:event_viewObstaclesCheckBoxMenuItemActionPerformed

private void viewShadingCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewShadingCheckBoxMenuItemActionPerformed
    viewNoneCheckBoxMenuItem.setSelected(!viewShadingCheckBoxMenuItem.isSelected());
    viewLargeStonesMaskCheckBoxMenuItem.setSelected(false);
    viewGradationMaskCheckBoxMenuItem.setSelected(false);
    viewObstaclesCheckBoxMenuItem.setSelected(false);
    viewReferenceCheckBoxMenuItem.setSelected(false);
    synchronizeBackgroundImageWithMenu();
}//GEN-LAST:event_viewShadingCheckBoxMenuItemActionPerformed

private void viewLargeStonesMaskCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewLargeStonesMaskCheckBoxMenuItemActionPerformed
    viewNoneCheckBoxMenuItem.setSelected(!viewLargeStonesMaskCheckBoxMenuItem.isSelected());
    viewShadingCheckBoxMenuItem.setSelected(false);
    viewGradationMaskCheckBoxMenuItem.setSelected(false);
    viewObstaclesCheckBoxMenuItem.setSelected(false);
    viewReferenceCheckBoxMenuItem.setSelected(false);
    synchronizeBackgroundImageWithMenu();
}//GEN-LAST:event_viewLargeStonesMaskCheckBoxMenuItemActionPerformed

private void viewReferenceCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewReferenceCheckBoxMenuItemActionPerformed
    viewNoneCheckBoxMenuItem.setSelected(!viewReferenceCheckBoxMenuItem.isSelected());
    viewShadingCheckBoxMenuItem.setSelected(false);
    viewLargeStonesMaskCheckBoxMenuItem.setSelected(false);
    viewGradationMaskCheckBoxMenuItem.setSelected(false);
    viewObstaclesCheckBoxMenuItem.setSelected(false);
    synchronizeBackgroundImageWithMenu();
}//GEN-LAST:event_viewReferenceCheckBoxMenuItemActionPerformed

private void toggleViewMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleViewMenuItemActionPerformed

    boolean showRef = !viewReferenceCheckBoxMenuItem.isSelected();

    // adjust selection flag in menu
    viewNoneCheckBoxMenuItem.setSelected(false);
    viewShadingCheckBoxMenuItem.setSelected(false);
    viewLargeStonesMaskCheckBoxMenuItem.setSelected(false);
    viewGradationMaskCheckBoxMenuItem.setSelected(false);
    viewObstaclesCheckBoxMenuItem.setSelected(!showRef);
    viewReferenceCheckBoxMenuItem.setSelected(showRef);

    // toggle visibility of scree stones
    viewScreeCheckBoxMenuItem.setSelected(!showRef);
    screeGenerator.screeData.screeStones.setVisible(!showRef);

    // adjust background image to new selection
    synchronizeBackgroundImageWithMenu();
  
}//GEN-LAST:event_toggleViewMenuItemActionPerformed

private void viewGullyLinesCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewGullyLinesCheckBoxMenuItemActionPerformed
    screeGenerator.screeData.gullyLines.setVisible(viewGullyLinesCheckBoxMenuItem.isSelected());
}//GEN-LAST:event_viewGullyLinesCheckBoxMenuItemActionPerformed

private void dataButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dataButtonActionPerformed
    showScreeDataDialog(false);
}//GEN-LAST:event_dataButtonActionPerformed

private void areaToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_areaToggleButtonActionPerformed
    areaOfInterest.setVisible(!areaToggleButton.isSelected());
}//GEN-LAST:event_areaToggleButtonActionPerformed

    private void exportGullyLines() {
        // make sure lines are visible, as only visible vectors are exported.
        final boolean vis = screeGenerator.screeData.gullyLines.isVisible();
        MapEventTrigger trigger = new MapEventTrigger(screeGenerator.screeData.gullyLines);
        screeGenerator.screeData.gullyLines.setVisible(true);
        try {
            ShapeExporter exporter = new ShapeExporter();
            GeoExportGUI.export(exporter, screeGenerator.screeData.gullyLines, "lines.shp", this, null, false);
        } finally {
            screeGenerator.screeData.gullyLines.setVisible(vis);
            trigger.abort();
        }
    }

private void exportGullyLinesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGullyLinesMenuItemActionPerformed

    if (screeGenerator.screeData.gullyLines.getNumberOfChildren() == 0) {
        String msg = "<html>There are currently no scree lines." +
                "<br>Do you want to generate scree lines for all polygons first?</html>";
        String title = "Export Scree Lines";
        int res = JOptionPane.showConfirmDialog(mapComponent, msg, title, JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            return;
        }
        new ScreeWorker(this).generateOnlyGullyLinesForAllPolygons();
    }

    exportGullyLines();


}//GEN-LAST:event_exportGullyLinesMenuItemActionPerformed

private void updateScreeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateScreeMenuItemActionPerformed
    screeParametersPanel.readGUI();
    generateScree();
}//GEN-LAST:event_updateScreeMenuItemActionPerformed

private void generateScreeLinesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateScreeLinesMenuItemActionPerformed
    screeParametersPanel.readGUI();
    new ScreeWorker(this).generateOnlyGullyLinesForAllPolygons();
    screeGenerator.screeData.gullyLines.setVisible(true);
    viewGullyLinesCheckBoxMenuItem.setSelected(true);
}//GEN-LAST:event_generateScreeLinesMenuItemActionPerformed

private void viewGradationMaskCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewGradationMaskCheckBoxMenuItemActionPerformed
    viewNoneCheckBoxMenuItem.setSelected(!viewGradationMaskCheckBoxMenuItem.isSelected());
    viewShadingCheckBoxMenuItem.setSelected(false);
    viewLargeStonesMaskCheckBoxMenuItem.setSelected(false);
    viewObstaclesCheckBoxMenuItem.setSelected(false);
    viewReferenceCheckBoxMenuItem.setSelected(false);
    synchronizeBackgroundImageWithMenu();
}//GEN-LAST:event_viewGradationMaskCheckBoxMenuItemActionPerformed

private void viewNoneCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewNoneCheckBoxMenuItemActionPerformed
    viewShadingCheckBoxMenuItem.setSelected(false);
    viewLargeStonesMaskCheckBoxMenuItem.setSelected(false);
    viewGradationMaskCheckBoxMenuItem.setSelected(false);
    viewObstaclesCheckBoxMenuItem.setSelected(false);
    viewReferenceCheckBoxMenuItem.setSelected(false);
    synchronizeBackgroundImageWithMenu();
}//GEN-LAST:event_viewNoneCheckBoxMenuItemActionPerformed

private void reportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reportMenuItemActionPerformed
    JOptionPane.showMessageDialog(this, this.screeGenerationReport, "Report", JOptionPane.INFORMATION_MESSAGE);
}//GEN-LAST:event_reportMenuItemActionPerformed

private void helpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuItemActionPerformed
    Properties props =
            ika.utils.PropertiesLoader.loadProperties("ika.app.Application.properties");
    String url = props.getProperty("HelpWebPage");
    ika.utils.BrowserLauncherWrapper.openURL(url);
}//GEN-LAST:event_helpMenuItemActionPerformed

private void adjustUpdateAreaScreeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adjustUpdateAreaScreeMenuItemActionPerformed
    Rectangle2D visArea = mapComponent.getVisibleArea();
    double dh = visArea.getWidth() * 0.02;
    double dv = visArea.getHeight() * 0.02;
    visArea.setRect(visArea.getX() + dh, visArea.getY() + dv,
                visArea.getWidth()- 2 * dh, visArea.getHeight() - 2 * dv);
    areaOfInterest.reset();
    areaOfInterest.append(visArea, false);
    areaOfInterest.setVisible(true);
    areaToggleButton.setSelected(false);
}//GEN-LAST:event_adjustUpdateAreaScreeMenuItemActionPerformed

private void zoomOnUpdateAreaMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOnUpdateAreaMenuItemActionPerformed
    this.areaOfInterest.setVisible(true);
    areaToggleButton.setSelected(false);
    this.mapComponent.zoomOnRectangle(this.areaOfInterest.getBounds2D(GeoObject.UNDEFINED_SCALE));
}//GEN-LAST:event_zoomOnUpdateAreaMenuItemActionPerformed

    private void macInfoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macInfoMenuItemActionPerformed
        ika.gui.ProgramInfoPanel.showApplicationInfo();
    }//GEN-LAST:event_macInfoMenuItemActionPerformed

    /**
     * A property change listener for the root pane that adjusts the enabled
     * state of the save menu depending on the windowModified property attached
     * to the root pane.
     */
    private void windowModifiedPropertyChange(java.beans.PropertyChangeEvent evt) {

        // only treat changes to the windowModified property
        if (!"windowModified".equals(evt.getPropertyName())) {
            return;
        }

        // retrieve the value of the windowModified property
        Boolean windowModified = null;
        if (saveSettingsMenuItem != null && this.getRootPane() != null) {
            windowModified =
                    (Boolean) this.getRootPane().getClientProperty("windowModified");
        }

        // enable or disable the saveMenu accordingly
        if (windowModified != null) {
            this.saveSettingsMenuItem.setEnabled(windowModified.booleanValue());
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem adjustUpdateAreaScreeMenuItem;
    private javax.swing.JToggleButton areaToggleButton;
    private javax.swing.JToggleButton arrowToggleButton;
    private javax.swing.JMenuItem closeMenuItem;
    private ika.gui.CoordinateInfoPanel coordinateInfoPanel;
    private javax.swing.JButton dataButton;
    private javax.swing.JToggleButton distanceToggleButton;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JSeparator exitMenuSeparator;
    private javax.swing.JMenuItem exportGullyLinesMenuItem;
    private javax.swing.JMenuItem exportScreeMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem generateScreeLinesMenuItem;
    private javax.swing.JToggleButton handToggleButton;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem infoMenuItem;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JSeparator jSeparator13;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JMenuItem loadInputDataMenuItem;
    private javax.swing.JMenuItem loadSettingsMenuItem;
    private javax.swing.JMenu macHelpMenu;
    private javax.swing.JMenuItem macHelpMenuItem;
    private javax.swing.JMenuItem macInfoMenuItem;
    private ika.gui.MapComponent mapComponent;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem minimizeMenuItem;
    private javax.swing.JToolBar navigationToolBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem reportMenuItem;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JMenuItem saveSettingsMenuItem;
    private ika.gui.ScaleLabel scaleLabel;
    private javax.swing.JMenu screeMenu;
    private javax.swing.JButton showAllButton;
    private javax.swing.JMenuItem showAllMenuItem;
    private javax.swing.JCheckBoxMenuItem showPageCheckBoxMenuItem;
    private ika.gui.ScaleLabel slopeLabel;
    private javax.swing.JMenuItem toggleViewMenuItem;
    private javax.swing.ButtonGroup toolBarButtonGroup;
    private javax.swing.JButton updateButton;
    private javax.swing.JMenuItem updateScreeMenuItem;
    private javax.swing.JCheckBoxMenuItem viewGradationMaskCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem viewGullyLinesCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem viewLargeStonesMaskCheckBoxMenuItem;
    private javax.swing.JMenu viewMenu;
    private ika.gui.MenuToggleButton viewMenuButton;
    private javax.swing.JCheckBoxMenuItem viewNoneCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem viewObstaclesCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem viewPolygonsCheckBoxMenuItem;
    private javax.swing.JPopupMenu viewPopupMenu;
    private javax.swing.JCheckBoxMenuItem viewReferenceCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem viewScreeCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem viewShadingCheckBoxMenuItem;
    private javax.swing.JMenuItem winHelpMenuItem;
    private javax.swing.JMenu windowMenu;
    private javax.swing.JSeparator windowSeparator;
    private javax.swing.JMenuItem zoomInMenuItem;
    private javax.swing.JToggleButton zoomInToggleButton;
    private javax.swing.JMenuItem zoomMenuItem;
    private javax.swing.JMenuItem zoomOnUpdateAreaMenuItem;
    private javax.swing.JMenuItem zoomOutMenuItem;
    private javax.swing.JToggleButton zoomOutToggleButton;
    // End of variables declaration//GEN-END:variables
}
