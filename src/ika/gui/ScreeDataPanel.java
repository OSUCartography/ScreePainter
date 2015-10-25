/*
 * ScreeDataPanel.java
 *
 * Created on Nov 26, 2008, 3:53:33 PM
 */
package ika.gui;

import ika.app.ScreeData;
import ika.app.ScreeDataFilePaths;
import ika.app.ScreeGenerator;
import ika.geo.GeoImage;
import ika.geo.GeoObject;
import ika.geo.GeoSet;
import ika.geo.MapEventTrigger;
import ika.geo.grid.GridPlanCurvatureOperator;
import ika.geoimport.ESRIASCIIGridReader;
import ika.geoimport.GeoImporter;
import ika.geoimport.ImageImporter;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ScreeDataPanel extends javax.swing.JPanel {

    private final ScreeDataFilePaths screeInputData;
    private final ScreeData screeData;
    private boolean okButtonPressed = false;
    private static String lastPathSelected = ScreeDataFilePaths.getDirPath();

    public static boolean showDialog(JFrame owner,
            ScreeDataFilePaths data,
            ScreeData screeData,
            GeoSet backgroundGeoSet,
            GeoSet foregroundGeoSet,
            boolean showCancel) {

        final ScreeDataPanel screeDataPanel = new ScreeDataPanel(owner, data,
                screeData,
                backgroundGeoSet,
                foregroundGeoSet,
                showCancel);

        screeDataPanel.screeDataDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                screeDataPanel.okButtonPressed = screeDataPanel.okButton.isEnabled();
                screeDataPanel.screeDataDialog.setVisible(false);
                if (screeDataPanel.okButtonPressed) {
                    data.writePathsToPreferences();
                }
            }
        });
        screeDataPanel.screeDataDialog.setLocationRelativeTo(owner);
        screeDataPanel.screeDataDialog.setVisible(true);
        return screeDataPanel.okButtonPressed;

    }
    private final GeoSet backgroundGeoSet;
    private final GeoSet foregroundGeoSet;
    private static final String REF_IMAGE_NAME = "refmap";
    private static final String OBSTACLES_IMAGE_NAME = "obstacles";
    private static final String LARGE_STONE_IMAGE_NAME = "largeStonesMask";
    private static final String SHADING_IMAGE_NAME = "shading";
    private static final String GRADATION_MASK_IMAGE_NAME = "gradationMask";
    private static final String LINES_NAME = "lines";
    private static final String POLYGONS_NAME = "polygons";

    /**
     * store the owner of the dialog: a hack to pass the owner to
     * initComponents()
     */
    private final JFrame owner;

    /**
     * Creates new form ScreeDataPanel
     */
    private ScreeDataPanel(JFrame owner,
            ScreeDataFilePaths screeInputData,
            ScreeData screeData,
            GeoSet backgroundGeoSet,
            GeoSet foregroundGeoSet,
            boolean showCancel) {
        this.owner = owner;
        this.screeInputData = screeInputData;
        this.screeData = screeData;
        this.backgroundGeoSet = backgroundGeoSet;
        this.foregroundGeoSet = foregroundGeoSet;
        initComponents();
        writeToGUI();
        screeDataDialog.getContentPane().add(this, BorderLayout.CENTER);

        // make the dialog dismissable with the escape key
        if (showCancel) {
            ActionListener actionListener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    screeDataDialog.setVisible(false);
                }
            };
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            screeDataDialog.getRootPane().registerKeyboardAction(
                    actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        cancelButton.setVisible(showCancel);
        screeDataDialog.pack();
    }

    private void removeAllData() {
        this.clearDEM();

        this.clearShading();
        this.clearReferenceImage();
        this.clearGradationMask();
        this.clearLargeStonesMask();
        this.clearObstaclesMask();

        this.clearScreePolygons();
        this.clearGullyLines();

    }

    private void clearDEM() {
        screeData.dem = null;
        screeData.curvatureGrid = null;
        screeInputData.demFilePath = null;
    }

    private void clearShading() {
        backgroundGeoSet.remove(screeData.shadingImage);
        screeData.shadingImage = null;
        screeInputData.shadingFilePath = null;
    }

    private void clearObstaclesMask() {
        backgroundGeoSet.remove(screeData.obstaclesMaskImage);
        screeData.obstaclesMaskImage = null;
        screeInputData.obstaclesFilePath = null;
    }

    private void clearReferenceImage() {
        backgroundGeoSet.remove(screeData.referenceImage);
        screeData.referenceImage = null;
        screeInputData.referenceFilePath = null;
    }

    private void clearGradationMask() {
        backgroundGeoSet.remove(screeData.shadingGradationMaskImage);
        screeData.shadingGradationMaskImage = null;
        screeInputData.gradationMaskFilePath = null;
    }

    private void clearLargeStonesMask() {
        backgroundGeoSet.remove(screeData.largeStoneMaskImage);
        screeData.largeStoneMaskImage = null;
        screeInputData.largeStoneFilePath = null;
    }

    private void clearScreePolygons() {
        screeData.screePolygons.removeAllGeoObjects();
        screeInputData.screePolygonsFilePath = null;
    }

    private void clearGullyLines() {
        screeData.gullyLines.removeAllGeoObjects();
        screeInputData.gullyLinesFilePath = null;
        screeData.fixedScreeLines = false;
    }

    private void loadData(
            final boolean loadDem,
            final boolean loadShading,
            final boolean loadLargeStoneMask,
            final boolean loadGradationMask,
            final boolean loadObstaclesMask,
            final boolean loadRefImage,
            final boolean loadScreePolygons,
            final boolean loadGullyLines) {

        int tasks = 0;
        if (loadDem) {
            ++tasks;
        }
        if (loadShading) {
            ++tasks;
        }
        if (loadLargeStoneMask) {
            ++tasks;
        }
        if (loadGradationMask) {
            ++tasks;
        }
        if (loadObstaclesMask) {
            ++tasks;
        }
        if (loadRefImage) {
            ++tasks;
        }
        if (loadScreePolygons) {
            this.foregroundGeoSet.remove(screeData.screePolygons);
            ++tasks;
        }
        if (loadGullyLines) {
            this.foregroundGeoSet.remove(screeData.gullyLines);
            ++tasks;
        }

        final Frame frame = ika.gui.GUIUtil.getOwnerFrame(this);
        SwingWorkerWithProgressIndicator worker = new SwingWorkerWithProgressIndicator(
                frame, "Scree Painter Data Import", "", true) {

                    String errorMsg = "The file could not be imported.";
                    String errorTitle = "Scree Painter Error";

                    @Override
                    protected void done() {
                        MapEventTrigger trigger = new MapEventTrigger(backgroundGeoSet);
                        try {
                            get(); // exceptions that occured in the backround tasks are thrown here

                            writeToGUI();

                            warnOfSmallCellSize(screeData.dem);

                            backgroundGeoSet.replaceGeoObject(screeData.shadingImage, SHADING_IMAGE_NAME);
                            warnOfSmallCellSize(screeData.shadingImage);

                            backgroundGeoSet.replaceGeoObject(screeData.largeStoneMaskImage, LARGE_STONE_IMAGE_NAME);
                            warnOfSmallCellSize(screeData.largeStoneMaskImage);

                            backgroundGeoSet.replaceGeoObject(screeData.shadingGradationMaskImage, GRADATION_MASK_IMAGE_NAME);
                            warnOfSmallCellSize(screeData.shadingGradationMaskImage);

                            backgroundGeoSet.replaceGeoObject(screeData.obstaclesMaskImage, OBSTACLES_IMAGE_NAME);
                            warnOfSmallCellSize(screeData.obstaclesMaskImage);

                            backgroundGeoSet.replaceGeoObject(screeData.referenceImage, REF_IMAGE_NAME);
                            warnOfSmallCellSize(screeData.referenceImage);

                            foregroundGeoSet.add(screeData.screePolygons);

                            foregroundGeoSet.add(screeData.gullyLines);
                        } catch (ExecutionException ex) {
                            ika.utils.ErrorDialog.showErrorDialog(errorMsg, errorTitle, ex.getCause(), dialog);
                        } catch (Throwable ex) {
                            ika.utils.ErrorDialog.showErrorDialog(errorMsg, errorTitle, ex, dialog);
                        } finally {
                            trigger.inform();
                        }
                    }

                    @Override
                    protected Object doInBackground() throws Exception {

                        MapEventTrigger trigger = new MapEventTrigger(backgroundGeoSet);
                        try {

                            if (loadShading) {
                                this.nextTask();
                                loadShading(this);
                                if (isAborted()) {
                                    return null;
                                }
                            }

                            if (loadLargeStoneMask) {
                                this.nextTask();
                                loadLargeStonesMask(this);
                                if (isAborted()) {
                                    return null;
                                }
                            }

                            if (loadGradationMask) {
                                this.nextTask();
                                loadGradationMask(this);
                                if (isAborted()) {
                                    return null;
                                }
                            }

                            if (loadObstaclesMask) {
                                this.nextTask();
                                loadObstaclesMask(this);
                                if (isAborted()) {
                                    return null;
                                }
                            }

                            if (loadRefImage) {
                                this.nextTask();
                                loadReferenceImage(this);
                                if (isAborted()) {
                                    return null;
                                }
                            }

                            if (loadDem) {
                                loadDEM(this);
                                if (isAborted()) {
                                    return null;
                                }
                            }

                            if (loadScreePolygons) {
                                this.nextTask();
                                loadScreePolygons(this);
                                if (isAborted()) {
                                    return null;
                                }
                            }

                            if (loadGullyLines) {
                                this.nextTask();
                                loadGullyLines(this);
                                if (isAborted()) {
                                    return null;
                                }
                            }

                        } finally {
                            this.complete();
                            trigger.abort();
                        }
                        return null;
                    }
                };
        worker.setTotalTasksCount(tasks);
        worker.setMaxTimeWithoutDialog(1);
        worker.execute();
    }

    private void warnOfSmallCellSize(ika.geo.AbstractRaster raster) {
        if (raster != null && raster.getCellSize() < 0.1) {
            String msg = "<html> \""
                    + raster.getName()
                    + "\" has very small cells.<br>"
                    + "It is either in geographic \"unprojected\" coordinates,<br> "
                    + "or it is extremely detailed. The data should be<br> "
                    + "reprojected or resampled before generating scree.</html>";
            String title = raster.getName() + ": Small Cell Size";
            ika.utils.ErrorDialog.showErrorDialog(msg, title, null, this);
        }
    }

    private static GeoImage loadImage(String filePath,
            String name,
            ProgressIndicator progressIndicator) throws IOException {
        ImageImporter importer = new ImageImporter();
        importer.setOptimizeForDisplay(false);
        importer.setProgressIndicator(progressIndicator, false);
        GeoImage geoImage = (GeoImage) importer.read(filePath);
        if (geoImage != null) {
            geoImage.setName(name);
            geoImage.setVisible(false);
            geoImage.setSelectable(false);
        }
        return geoImage;
    }

    private void loadDEM(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading elevation model...");
        prog.enableCancel();
        screeData.dem = ESRIASCIIGridReader.read(screeInputData.demFilePath());
        screeData.curvatureGrid = new GridPlanCurvatureOperator().operate(screeData.dem);
    }

    private void loadShading(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading shaded relief...");
        prog.enableCancel();
        screeData.shadingImage = loadImage(screeInputData.shadingFilePath(),
                SHADING_IMAGE_NAME, prog);
        if (screeData.shadingImage != null) {
            screeData.shadingImage.convertToGrayscale();
        }
    }

    private void loadLargeStonesMask(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading mask for large stones...");
        prog.enableCancel();
        screeData.largeStoneMaskImage = loadImage(screeInputData.largeStonesFilePath(),
                LARGE_STONE_IMAGE_NAME, prog);
        if (screeData.largeStoneMaskImage != null) {
            screeData.largeStoneMaskImage.convertToGrayscale();
        }
    }

    private void loadGradationMask(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading mask for alternative gradation...");
        prog.enableCancel();
        screeData.shadingGradationMaskImage = loadImage(screeInputData.gradationMaskFilePath(),
                GRADATION_MASK_IMAGE_NAME, prog);
        if (screeData.shadingGradationMaskImage != null) {
            screeData.shadingGradationMaskImage.convertToGrayscale();
        }
    }

    private void loadObstaclesMask(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading obstacles mask image...");
        prog.enableCancel();
        screeData.obstaclesMaskImage = loadImage(screeInputData.obstaclesFilePath(),
                OBSTACLES_IMAGE_NAME, prog);
        if (screeData.obstaclesMaskImage != null) {
            // this image will be drawn frequently, and it could be in color,
            // so optimize it for the display hardware.
            screeData.obstaclesMaskImage.optimizeForDisplay();
        }
    }

    private void loadReferenceImage(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading reference image...");
        prog.enableCancel();
        screeData.referenceImage = loadImage(screeInputData.referenceFilePath(),
                REF_IMAGE_NAME, prog);
        if (screeData.referenceImage != null) {
            // this image will be drawn frequently, and it could be in color,
            // so optimize it for the display hardware.
            screeData.referenceImage.optimizeForDisplay();
        }
    }

    private void loadScreePolygons(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading scree polygons...");
        prog.enableCancel();
        String filePath = screeInputData.screePolygonsFilePath();
        GeoImporter importer = GeoImporter.findGeoImporter(filePath);

        GeoObject geoObject = importer.read(filePath);
        final GeoSet newScreePolygons;
        if (geoObject instanceof GeoSet) {
            newScreePolygons = (GeoSet) geoObject;
        } else {
            // single paths are not returned in a GeoSet
            newScreePolygons = new GeoSet();
            newScreePolygons.add(geoObject);
        }
        screeData.screePolygons.replaceGeoObjects(newScreePolygons);
        screeData.screePolygons.setVectorSymbol(ScreeGenerator.SCREE_POLYGON_VECTOR_SYMBOL);
        screeData.screePolygons.setVisible(false);
        screeData.screePolygons.setSelectable(false);
        screeData.screePolygons.setName(POLYGONS_NAME);
    }

    private void loadGullyLines(ProgressIndicator prog) throws IOException {
        prog.setMessage("Loading gully lines...");
        prog.enableCancel();
        String filePath = screeInputData.gullyLinesFilePath();
        GeoImporter importer = GeoImporter.findGeoImporter(filePath);
        GeoObject geoObject = importer.read(filePath);
        final GeoSet newGullyLines;
        if (geoObject instanceof GeoSet) {
            newGullyLines = (GeoSet) geoObject;
        } else {
            // single paths are not returned in a GeoSet
            newGullyLines = new GeoSet();
            newGullyLines.add(geoObject);
        }
        screeData.gullyLines.replaceGeoObjects(newGullyLines);
        screeData.gullyLines.setVectorSymbol(ScreeGenerator.GULLIES_VECTOR_SYMBOL);
        screeData.gullyLines.setVisible(false);
        screeData.gullyLines.setSelectable(false);
        screeData.gullyLines.setName(LINES_NAME);
        screeData.fixedScreeLines = true;
    }

    private void writeToGUI() {

        // file paths
        this.polygonsFilePathLabel.setText(screeInputData.screePolygonsFilePathOrName());
        this.demFilePathLabel.setText(screeInputData.demFilePathOrName());
        this.shadingFilePathLabel.setText(screeInputData.shadingFilePathOrName());
        this.largeStonesFilePathLabel.setText(screeInputData.largeStonesFilePathOrName());
        this.obstaclesFilePathLabel.setText(screeInputData.obstaclesFilePathOrName());
        this.gradationMaskFilePathLabel.setText(screeInputData.gradationMaskFilePathOrName());
        this.screeLinesFilePathLabel.setText(screeInputData.gullyLinesFilePathOrName());
        this.referenceImageFilePathLabel.setText(screeInputData.referenceFilePathOrName());

        // reload buttons
        this.reloadShadingButton.setEnabled(this.screeData.hasShading());
        this.reloadGradationMaskButton.setEnabled(this.screeData.hasShadingGradationMask());
        this.reloadLargeStonesMaskButton.setEnabled(this.screeData.hasLargeStoneMask());
        this.reloadObstaclesButton.setEnabled(this.screeData.hasObstaclesMask());
        this.reloadReferenceButton.setEnabled(this.screeData.hasReferenceImage());
        this.reloadScreeLinesButton.setEnabled(this.screeData.fixedScreeLines);
        this.reloadScreePolygonButton.setEnabled(this.screeData.hasScreePolygons());
        this.reloadDEMButton.setEnabled(this.screeData.hasDEM());

        // clear buttons
        this.clearGradationMaskButton.setEnabled(this.screeData.hasShadingGradationMask());
        this.clearLargeStonesMaskButton.setEnabled(this.screeData.hasLargeStoneMask());
        this.clearReferenceImageButton.setEnabled(this.screeData.hasReferenceImage());
        this.clearScreeLinesButton.setEnabled(this.screeData.fixedScreeLines);

        // check marks
        Icon tickIcon = new ImageIcon(getClass().getResource("/ika/icons/checkmark.png"));
        Icon emptyIcon = new ImageIcon(getClass().getResource("/ika/icons/empty16x16.png"));
        this.shadingCheckMark.setIcon(screeData.hasShading() ? tickIcon : emptyIcon);
        this.obstaclesCheckMark.setIcon(screeData.hasObstaclesMask() ? tickIcon : emptyIcon);
        this.demCheckMark.setIcon(screeData.hasDEM() ? tickIcon : emptyIcon);
        this.screePolygonsCheckMark.setIcon(screeData.hasScreePolygons() ? tickIcon : emptyIcon);
        this.largeStonesMaskCheckMark.setIcon(screeData.hasLargeStoneMask() ? tickIcon : emptyIcon);
        this.gradationMaskCheckMark.setIcon(screeData.hasShadingGradationMask() ? tickIcon : emptyIcon);
        this.referenceImageCheckMark.setIcon(screeData.hasReferenceImage() ? tickIcon : emptyIcon);
        this.gullyLinesCheckMark.setIcon(screeData.fixedScreeLines ? tickIcon : emptyIcon);

        // enable or disable OK button
        boolean enableOK = screeData.hasShading() && screeData.hasObstaclesMask() && screeData.hasDEM() && screeData.hasScreePolygons();
        this.okButton.setEnabled(enableOK);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        screeDataDialog = new javax.swing.JDialog(this.owner);
        javax.swing.JPanel jPanel7 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel33 = new javax.swing.JLabel();
        javax.swing.JPanel jPanel8 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel6 = new javax.swing.JPanel();
        selectFolderButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        javax.swing.JTabbedPane jTabbedPane1 = new javax.swing.JTabbedPane();
        javax.swing.JPanel requiredDataPanel = new ika.gui.TransparentMacPanel();
        javax.swing.JPanel jPanel2 = new ika.gui.TransparentMacPanel();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        selectShadingButton = new javax.swing.JButton();
        selectDemButton = new javax.swing.JButton();
        selectPolygonsButton = new javax.swing.JButton();
        selectObstaclesButton = new javax.swing.JButton();
        shadingFilePathLabel = new javax.swing.JLabel();
        demFilePathLabel = new javax.swing.JLabel();
        polygonsFilePathLabel = new javax.swing.JLabel();
        obstaclesFilePathLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel13 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel14 = new javax.swing.JLabel();
        reloadShadingButton = new javax.swing.JButton();
        reloadDEMButton = new javax.swing.JButton();
        reloadScreePolygonButton = new javax.swing.JButton();
        reloadObstaclesButton = new javax.swing.JButton();
        shadingCheckMark = new javax.swing.JLabel();
        demCheckMark = new javax.swing.JLabel();
        screePolygonsCheckMark = new javax.swing.JLabel();
        obstaclesCheckMark = new javax.swing.JLabel();
        javax.swing.JPanel optionalDataPanel = new ika.gui.TransparentMacPanel();
        javax.swing.JPanel jPanel1 = new ika.gui.TransparentMacPanel();
        javax.swing.JLabel jLabel10 = new javax.swing.JLabel();
        selectLargeStonesMaskButton = new javax.swing.JButton();
        reloadLargeStonesMaskButton = new javax.swing.JButton();
        largeStonesFilePathLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        selectGradationMaskButton = new javax.swing.JButton();
        reloadGradationMaskButton = new javax.swing.JButton();
        gradationMaskFilePathLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel15 = new javax.swing.JLabel();
        screeLinesButton = new javax.swing.JButton();
        reloadScreeLinesButton = new javax.swing.JButton();
        clearScreeLinesButton = new javax.swing.JButton();
        screeLinesFilePathLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel16 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel11 = new javax.swing.JLabel();
        selectReferenceImageButton = new javax.swing.JButton();
        reloadReferenceButton = new javax.swing.JButton();
        referenceImageFilePathLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        clearReferenceImageButton = new javax.swing.JButton();
        clearGradationMaskButton = new javax.swing.JButton();
        clearLargeStonesMaskButton = new javax.swing.JButton();
        largeStonesMaskCheckMark = new javax.swing.JLabel();
        gradationMaskCheckMark = new javax.swing.JLabel();
        gullyLinesCheckMark = new javax.swing.JLabel();
        referenceImageCheckMark = new javax.swing.JLabel();

        screeDataDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        screeDataDialog.setTitle("Scree Painter Data");
        screeDataDialog.setLocationByPlatform(true);
        screeDataDialog.setModal(true);
        screeDataDialog.setResizable(false);

        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 30, 30));

        jLabel33.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/logo64x64.png"))); // NOI18N
        jPanel7.add(jLabel33);

        screeDataDialog.getContentPane().add(jPanel7, java.awt.BorderLayout.WEST);

        jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 15, 10, 15));
        jPanel6.setLayout(new java.awt.GridBagLayout());

        selectFolderButton.setText("Select Data Folder\u2026");
        selectFolderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectFolderButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 50);
        jPanel6.add(selectFolderButton, gridBagConstraints);

        cancelButton.setText("Cancel");
        cancelButton.setPreferredSize(new java.awt.Dimension(95, 29));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel6.add(cancelButton, gridBagConstraints);

        okButton.setText("OK");
        okButton.setPreferredSize(new java.awt.Dimension(95, 29));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        jPanel6.add(okButton, new java.awt.GridBagConstraints());

        jPanel8.add(jPanel6);

        screeDataDialog.getContentPane().add(jPanel8, java.awt.BorderLayout.SOUTH);

        screeDataDialog.getRootPane().setDefaultButton(okButton);
        screeDataDialog.pack();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 1, 5, 15));
        setLayout(new java.awt.BorderLayout());

        requiredDataPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 15, 1, 1));

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel6.setText("Shaded Relief");
        jLabel6.setPreferredSize(new java.awt.Dimension(130, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel6, gridBagConstraints);

        jLabel7.setText("Elevation Model");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel7, gridBagConstraints);

        jLabel8.setText("Scree Polygons");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel8, gridBagConstraints);

        jLabel9.setText("Obstacles Mask");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel9, gridBagConstraints);

        selectShadingButton.setText("Select\u2026");
        selectShadingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectShadingButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel2.add(selectShadingButton, gridBagConstraints);

        selectDemButton.setText("Select\u2026");
        selectDemButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectDemButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel2.add(selectDemButton, gridBagConstraints);

        selectPolygonsButton.setText("Select\u2026");
        selectPolygonsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectPolygonsButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel2.add(selectPolygonsButton, gridBagConstraints);

        selectObstaclesButton.setText("Select\u2026");
        selectObstaclesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectObstaclesButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel2.add(selectObstaclesButton, gridBagConstraints);

        shadingFilePathLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        shadingFilePathLabel.setText("-");
        shadingFilePathLabel.setEnabled(false);
        shadingFilePathLabel.setPreferredSize(new java.awt.Dimension(700, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel2.add(shadingFilePathLabel, gridBagConstraints);

        demFilePathLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        demFilePathLabel.setText("-");
        demFilePathLabel.setEnabled(false);
        demFilePathLabel.setPreferredSize(new java.awt.Dimension(700, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel2.add(demFilePathLabel, gridBagConstraints);

        polygonsFilePathLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        polygonsFilePathLabel.setText("-");
        polygonsFilePathLabel.setEnabled(false);
        polygonsFilePathLabel.setPreferredSize(new java.awt.Dimension(700, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel2.add(polygonsFilePathLabel, gridBagConstraints);

        obstaclesFilePathLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        obstaclesFilePathLabel.setText("-");
        obstaclesFilePathLabel.setEnabled(false);
        obstaclesFilePathLabel.setPreferredSize(new java.awt.Dimension(700, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel2.add(obstaclesFilePathLabel, gridBagConstraints);

        jLabel4.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel4.setText("<html>Scree stones are not placed where this image is black.<br>Format: TIFF, PNG, or JPEG grayscale image.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 20, 0);
        jPanel2.add(jLabel4, gridBagConstraints);

        jLabel5.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel5.setText("<html>The polygons to fill with scree stones.<br>Format: ESRI Shape file (polygons).</small></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 20, 0);
        jPanel2.add(jLabel5, gridBagConstraints);

        jLabel13.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel13.setText("<html>The digital elevation model is used to vary scree dots and find gully lines.<br>Format: ESRI ASCII Grid</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 20, 0);
        jPanel2.add(jLabel13, gridBagConstraints);

        jLabel14.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel14.setText("<html>Where this shaded relief image is dark, more and larger stones are placed.<br>Format: TIFF, PNG, or JPEG grayscale image.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 20, 0);
        jPanel2.add(jLabel14, gridBagConstraints);

        reloadShadingButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/reload.png"))); // NOI18N
        reloadShadingButton.setEnabled(false);
        reloadShadingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadShadingButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(reloadShadingButton, gridBagConstraints);

        reloadDEMButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/reload.png"))); // NOI18N
        reloadDEMButton.setEnabled(false);
        reloadDEMButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadDEMButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(reloadDEMButton, gridBagConstraints);

        reloadScreePolygonButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/reload.png"))); // NOI18N
        reloadScreePolygonButton.setEnabled(false);
        reloadScreePolygonButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadScreePolygonButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(reloadScreePolygonButton, gridBagConstraints);

        reloadObstaclesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/reload.png"))); // NOI18N
        reloadObstaclesButton.setEnabled(false);
        reloadObstaclesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadObstaclesButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(reloadObstaclesButton, gridBagConstraints);

        shadingCheckMark.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/checkmark.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel2.add(shadingCheckMark, gridBagConstraints);

        demCheckMark.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/checkmark.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel2.add(demCheckMark, gridBagConstraints);

        screePolygonsCheckMark.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/checkmark.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel2.add(screePolygonsCheckMark, gridBagConstraints);

        obstaclesCheckMark.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/checkmark.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel2.add(obstaclesCheckMark, gridBagConstraints);

        requiredDataPanel.add(jPanel2);

        jTabbedPane1.addTab("Required Data", requiredDataPanel);

        optionalDataPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 15, 1, 1));
        optionalDataPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 20));

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel10.setText("Large Stones Mask");
        jLabel10.setPreferredSize(new java.awt.Dimension(130, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel10, gridBagConstraints);

        selectLargeStonesMaskButton.setText("Select\u2026");
        selectLargeStonesMaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectLargeStonesMaskButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel1.add(selectLargeStonesMaskButton, gridBagConstraints);

        reloadLargeStonesMaskButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/reload.png"))); // NOI18N
        reloadLargeStonesMaskButton.setEnabled(false);
        reloadLargeStonesMaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadLargeStonesMaskButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(reloadLargeStonesMaskButton, gridBagConstraints);

        largeStonesFilePathLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        largeStonesFilePathLabel.setText("-");
        largeStonesFilePathLabel.setEnabled(false);
        largeStonesFilePathLabel.setPreferredSize(new java.awt.Dimension(550, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel1.add(largeStonesFilePathLabel, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel3.setText("<html><small>Larger scree stones are placed where this image is not white.<br>Format: TIFF, PNG, or JPEG grayscale image.</small></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 20, 0);
        jPanel1.add(jLabel3, gridBagConstraints);

        jLabel12.setText("Gradation Mask");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel12, gridBagConstraints);

        selectGradationMaskButton.setText("Select\u2026");
        selectGradationMaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectGradationMaskButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel1.add(selectGradationMaskButton, gridBagConstraints);

        reloadGradationMaskButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/reload.png"))); // NOI18N
        reloadGradationMaskButton.setEnabled(false);
        reloadGradationMaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadGradationMaskButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(reloadGradationMaskButton, gridBagConstraints);

        gradationMaskFilePathLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        gradationMaskFilePathLabel.setText("-");
        gradationMaskFilePathLabel.setEnabled(false);
        gradationMaskFilePathLabel.setPreferredSize(new java.awt.Dimension(550, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel1.add(gradationMaskFilePathLabel, gridBagConstraints);

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel2.setText("<html>An alternative gradation curve is applied to the shaded relief image where this image is dark.<br>Format: TIFF, PNG, or JPEG grayscale image.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 20, 0);
        jPanel1.add(jLabel2, gridBagConstraints);

        jLabel15.setText("Gully Lines");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel15, gridBagConstraints);

        screeLinesButton.setText("Select\u2026");
        screeLinesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                screeLinesButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel1.add(screeLinesButton, gridBagConstraints);

        reloadScreeLinesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/reload.png"))); // NOI18N
        reloadScreeLinesButton.setEnabled(false);
        reloadScreeLinesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadScreeLinesButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(reloadScreeLinesButton, gridBagConstraints);

        clearScreeLinesButton.setText("Clear");
        clearScreeLinesButton.setEnabled(false);
        clearScreeLinesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearScreeLinesButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel1.add(clearScreeLinesButton, gridBagConstraints);

        screeLinesFilePathLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        screeLinesFilePathLabel.setText("-");
        screeLinesFilePathLabel.setEnabled(false);
        screeLinesFilePathLabel.setPreferredSize(new java.awt.Dimension(550, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel1.add(screeLinesFilePathLabel, gridBagConstraints);

        jLabel16.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel16.setText("<html>Gully lines used instead of gully lines generated from the elevation model.<br>Format: ESRI Shape file (polylines).</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 20, 0);
        jPanel1.add(jLabel16, gridBagConstraints);

        jLabel11.setText("Reference Image");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jLabel11, gridBagConstraints);

        selectReferenceImageButton.setText("Select\u2026");
        selectReferenceImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectReferenceImageButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        jPanel1.add(selectReferenceImageButton, gridBagConstraints);

        reloadReferenceButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/reload.png"))); // NOI18N
        reloadReferenceButton.setEnabled(false);
        reloadReferenceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadReferenceButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(reloadReferenceButton, gridBagConstraints);

        referenceImageFilePathLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        referenceImageFilePathLabel.setText("-");
        referenceImageFilePathLabel.setEnabled(false);
        referenceImageFilePathLabel.setPreferredSize(new java.awt.Dimension(550, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 23;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel1.add(referenceImageFilePathLabel, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel1.setText("<html>An optional image to display in the background.<br>Format: TIFF, PNG, or JPEG image.</small></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 10, 0);
        jPanel1.add(jLabel1, gridBagConstraints);

        clearReferenceImageButton.setText("Clear");
        clearReferenceImageButton.setEnabled(false);
        clearReferenceImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearReferenceImageButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel1.add(clearReferenceImageButton, gridBagConstraints);

        clearGradationMaskButton.setText("Clear");
        clearGradationMaskButton.setEnabled(false);
        clearGradationMaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearGradationMaskButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel1.add(clearGradationMaskButton, gridBagConstraints);

        clearLargeStonesMaskButton.setText("Clear");
        clearLargeStonesMaskButton.setEnabled(false);
        clearLargeStonesMaskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearLargeStonesMaskButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel1.add(clearLargeStonesMaskButton, gridBagConstraints);

        largeStonesMaskCheckMark.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/checkmark.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel1.add(largeStonesMaskCheckMark, gridBagConstraints);

        gradationMaskCheckMark.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/checkmark.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel1.add(gradationMaskCheckMark, gridBagConstraints);

        gullyLinesCheckMark.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/checkmark.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 19;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel1.add(gullyLinesCheckMark, gridBagConstraints);

        referenceImageCheckMark.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ika/icons/checkmark.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel1.add(referenceImageCheckMark, gridBagConstraints);

        optionalDataPanel.add(jPanel1);

        jTabbedPane1.addTab("Optional Data", optionalDataPanel);

        add(jTabbedPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void selectShadingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectShadingButtonActionPerformed

        String filePath = ika.utils.FileUtils.askFile(null, "Load Relief Shading (Image Format)", lastPathSelected, true, null);
        if (filePath == null) {
            return;
        }
        lastPathSelected = filePath;
        this.screeInputData.shadingFilePath = filePath;
        this.loadData(false, true, false, false, false, false, false, false);
}//GEN-LAST:event_selectShadingButtonActionPerformed

    private void selectDemButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectDemButtonActionPerformed
        String filePath = ika.utils.FileUtils.askFile(null, "Load Elevation Model (ESRI ASCII Format)", lastPathSelected, true, null);
        if (filePath == null) {
            return;
        }
        lastPathSelected = filePath;
        this.screeInputData.demFilePath = filePath;
        this.loadData(true, false, false, false, false, false, false, false);
}//GEN-LAST:event_selectDemButtonActionPerformed

    private void selectPolygonsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectPolygonsButtonActionPerformed
        String filePath = ika.utils.FileUtils.askFile(null, "Load Scree Polygons (ESRI Shape File)", lastPathSelected, true, null);
        if (filePath == null) {
            return;
        }
        lastPathSelected = filePath;
        this.screeInputData.screePolygonsFilePath = filePath;
        this.loadData(false, false, false, false, false, false, true, false);
}//GEN-LAST:event_selectPolygonsButtonActionPerformed

    private void selectObstaclesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectObstaclesButtonActionPerformed
        String filePath = ika.utils.FileUtils.askFile(null, "Load Obstacles Mask (Image Format)", lastPathSelected, true, null);
        if (filePath == null) {
            return;
        }
        lastPathSelected = filePath;
        this.screeInputData.obstaclesFilePath = filePath;
        this.loadData(false, false, false, false, true, false, false, false);
}//GEN-LAST:event_selectObstaclesButtonActionPerformed

    private void selectLargeStonesMaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectLargeStonesMaskButtonActionPerformed
        String filePath = ika.utils.FileUtils.askFile(null, "Load Large Stones Mask (Image Format)", lastPathSelected, true, null);
        if (filePath == null) {
            return;
        }
        lastPathSelected = filePath;
        this.screeInputData.largeStoneFilePath = filePath;
        this.loadData(false, false, true, false, false, false, false, false);
}//GEN-LAST:event_selectLargeStonesMaskButtonActionPerformed

    private void selectReferenceImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectReferenceImageButtonActionPerformed
        String filePath = ika.utils.FileUtils.askFile(null, "Load Reference Image", lastPathSelected, true, null);
        if (filePath == null) {
            return;
        }
        lastPathSelected = filePath;
        this.screeInputData.referenceFilePath = filePath;
        this.loadData(false, false, false, false, false, true, false, false);
}//GEN-LAST:event_selectReferenceImageButtonActionPerformed

    private void selectGradationMaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectGradationMaskButtonActionPerformed
        String filePath = ika.utils.FileUtils.askFile(null, "Load Lines Density Mask (Image Format)", lastPathSelected, true, null);
        if (filePath == null) {
            return;
        }
        lastPathSelected = filePath;
        this.screeInputData.gradationMaskFilePath = filePath;
        this.loadData(false, false, false, true, false, false, false, false);
}//GEN-LAST:event_selectGradationMaskButtonActionPerformed

    private void reloadShadingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadShadingButtonActionPerformed
        this.loadData(false, true, false, false, false, false, false, false);
}//GEN-LAST:event_reloadShadingButtonActionPerformed

    private void reloadDEMButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadDEMButtonActionPerformed
        this.loadData(true, false, false, false, false, false, false, false);
}//GEN-LAST:event_reloadDEMButtonActionPerformed

    private void reloadScreePolygonButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadScreePolygonButtonActionPerformed
        this.loadData(false, false, false, false, false, false, true, false);
}//GEN-LAST:event_reloadScreePolygonButtonActionPerformed

    private void reloadObstaclesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadObstaclesButtonActionPerformed
        this.loadData(false, false, false, false, true, false, false, false);
}//GEN-LAST:event_reloadObstaclesButtonActionPerformed

    private void reloadLargeStonesMaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadLargeStonesMaskButtonActionPerformed
        this.loadData(false, false, true, false, false, false, false, false);
}//GEN-LAST:event_reloadLargeStonesMaskButtonActionPerformed

    private void reloadGradationMaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadGradationMaskButtonActionPerformed
        this.loadData(false, false, false, true, false, false, false, false);
}//GEN-LAST:event_reloadGradationMaskButtonActionPerformed

    private void reloadReferenceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadReferenceButtonActionPerformed
        this.loadData(false, false, false, false, false, true, false, false);
}//GEN-LAST:event_reloadReferenceButtonActionPerformed

    private void screeLinesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_screeLinesButtonActionPerformed
        String filePath = ika.utils.FileUtils.askFile(null, "Load Gully Lines (ESRI Shape File)", lastPathSelected, true, null);
        if (filePath == null) {
            return;
        }
        lastPathSelected = filePath;
        this.screeInputData.gullyLinesFilePath = filePath;
        this.loadData(false, false, false, false, false, false, false, true);
}//GEN-LAST:event_screeLinesButtonActionPerformed

    private void reloadScreeLinesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadScreeLinesButtonActionPerformed
        boolean vis = screeData.gullyLines.isVisible();
        this.loadData(false, false, false, false, false, false, false, true);
        screeData.gullyLines.setVisible(vis);
}//GEN-LAST:event_reloadScreeLinesButtonActionPerformed

    private void clearScreeLinesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearScreeLinesButtonActionPerformed
        clearGullyLines();
        writeToGUI();
}//GEN-LAST:event_clearScreeLinesButtonActionPerformed

    private void clearReferenceImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearReferenceImageButtonActionPerformed
        clearReferenceImage();
        writeToGUI();
}//GEN-LAST:event_clearReferenceImageButtonActionPerformed

    private void clearGradationMaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearGradationMaskButtonActionPerformed
        clearGradationMask();
        writeToGUI();
}//GEN-LAST:event_clearGradationMaskButtonActionPerformed

    private void clearLargeStonesMaskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearLargeStonesMaskButtonActionPerformed
        clearLargeStonesMask();
        writeToGUI();
}//GEN-LAST:event_clearLargeStonesMaskButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        this.okButtonPressed = true;
        this.screeDataDialog.setVisible(false);
}//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.screeDataDialog.setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void selectFolderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectFolderButtonActionPerformed
        try {
            String directory = ika.utils.FileUtils.askDirectory(null,
                    "Select Folder with Scree Data",
                    true,
                    ScreeDataFilePaths.getDirPath());
            if (directory == null) {
                return;
            }
            lastPathSelected = directory;

            // delete old data
            this.removeAllData();
            this.writeToGUI();

            ScreeDataFilePaths.setDirPath(directory);

            boolean loadShading = false;
            boolean loadLargeStoneMask = false;
            boolean loadGradationMask = false;
            boolean loadObstaclesMask = false;
            boolean loadDem = false;
            boolean loadRefImage = false;
            boolean loadScreePolygons = false;
            boolean loadScreeLines = false;

            File file = new File(directory, ScreeDataFilePaths.DEF_SHADING_PATH);
            if (file.exists()) {
                screeInputData.shadingFilePath = file.getCanonicalPath();
                loadShading = true;
            }
            file = new File(directory, ScreeDataFilePaths.DEF_DEM_PATH);
            if (file.exists()) {
                screeInputData.demFilePath = file.getCanonicalPath();
                loadDem = true;
            }
            file = new File(directory, ScreeDataFilePaths.DEF_SCREE_PATH);
            if (file.exists()) {
                screeInputData.screePolygonsFilePath = file.getCanonicalPath();
                loadScreePolygons = true;
            }
            file = new File(directory, ScreeDataFilePaths.DEF_OBSTACLES_PATH);
            if (file.exists()) {
                screeInputData.obstaclesFilePath = file.getCanonicalPath();
                loadObstaclesMask = true;
            }
            file = new File(directory, ScreeDataFilePaths.DEF_LARGE_STONES_MASK_PATH);
            if (file.exists()) {
                screeInputData.largeStoneFilePath = file.getCanonicalPath();
                loadLargeStoneMask = true;
            }
            file = new File(directory, ScreeDataFilePaths.DEF_GRADATION_MASK_PATH);
            if (file.exists()) {
                screeInputData.gradationMaskFilePath = file.getCanonicalPath();
                loadGradationMask = true;
            }
            file = new File(directory, ScreeDataFilePaths.DEF_REF_PATH);
            if (file.exists()) {
                screeInputData.referenceFilePath = file.getCanonicalPath();
                loadRefImage = true;
            }

            this.loadData(loadDem, loadShading, loadLargeStoneMask,
                    loadGradationMask, loadObstaclesMask, loadRefImage,
                    loadScreePolygons, loadScreeLines);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }//GEN-LAST:event_selectFolderButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton clearGradationMaskButton;
    private javax.swing.JButton clearLargeStonesMaskButton;
    private javax.swing.JButton clearReferenceImageButton;
    private javax.swing.JButton clearScreeLinesButton;
    private javax.swing.JLabel demCheckMark;
    private javax.swing.JLabel demFilePathLabel;
    private javax.swing.JLabel gradationMaskCheckMark;
    private javax.swing.JLabel gradationMaskFilePathLabel;
    private javax.swing.JLabel gullyLinesCheckMark;
    private javax.swing.JLabel largeStonesFilePathLabel;
    private javax.swing.JLabel largeStonesMaskCheckMark;
    private javax.swing.JLabel obstaclesCheckMark;
    private javax.swing.JLabel obstaclesFilePathLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel polygonsFilePathLabel;
    private javax.swing.JLabel referenceImageCheckMark;
    private javax.swing.JLabel referenceImageFilePathLabel;
    private javax.swing.JButton reloadDEMButton;
    private javax.swing.JButton reloadGradationMaskButton;
    private javax.swing.JButton reloadLargeStonesMaskButton;
    private javax.swing.JButton reloadObstaclesButton;
    private javax.swing.JButton reloadReferenceButton;
    private javax.swing.JButton reloadScreeLinesButton;
    private javax.swing.JButton reloadScreePolygonButton;
    private javax.swing.JButton reloadShadingButton;
    private javax.swing.JDialog screeDataDialog;
    private javax.swing.JButton screeLinesButton;
    private javax.swing.JLabel screeLinesFilePathLabel;
    private javax.swing.JLabel screePolygonsCheckMark;
    private javax.swing.JButton selectDemButton;
    private javax.swing.JButton selectFolderButton;
    private javax.swing.JButton selectGradationMaskButton;
    private javax.swing.JButton selectLargeStonesMaskButton;
    private javax.swing.JButton selectObstaclesButton;
    private javax.swing.JButton selectPolygonsButton;
    private javax.swing.JButton selectReferenceImageButton;
    private javax.swing.JButton selectShadingButton;
    private javax.swing.JLabel shadingCheckMark;
    private javax.swing.JLabel shadingFilePathLabel;
    // End of variables declaration//GEN-END:variables
}
