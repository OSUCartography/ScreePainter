/*
 * ScreeParametersPanel.java
 *
 * Created on November 13, 2008, 4:16 PM
 */
package ika.gui;

import ika.app.ScreeData;
import ika.app.ScreeParameters;
import ika.utils.FileUtils;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.InternationalFormatter;

/**
 *
 * @author  Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ScreeParametersPanel extends javax.swing.JPanel {
    private static final String DISABLED_GRADATION_EXTERNAL_LINES = "Disabled because gully lines are loaded from file";
    private static final String DISABLED_GRADATION_NO_MASK = "Disabled because no gradation mask is loaded";

    private GradationGraph gradationGraph1 = new GradationGraph();
    private GradationGraph gradationGraph2 = new GradationGraph();
    private GradationGraph lineGradationGraph = new GradationGraph();
    private boolean updatingGUI = false;
    private ScreeParameters p;
    private ScreeData screeData;

    public ScreeParametersPanel() {
        initComponents();
        gradationGraph2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lineGradationGraph.setFont(new Font("SansSerif", Font.PLAIN, 10));
        shadowLabel.setFont(maxDiameterSlider.getFont());
        sunLabel.setFont(minDiameterSlider.getFont());
        this.validate();
    }

    public void setParameters(ScreeParameters p, ScreeData data) {
        this.p = p;
        this.screeData = data;
        writeGUI();
    }

    public void setShadingHistogram(int[] histogram) {
        this.gradationGraph1.setHistogram(histogram);
        this.lineGradationGraph.setHistogram(histogram);
    }

    public void setGradationMaskHistogram(int[] histogram) {
        this.gradationGraph2.setHistogram(histogram);
    }

    public void setHistogramHighlight(int shadingHistoMark, int gradationMaskHistoMark) {
        this.gradationGraph1.setHistogramHighlight(shadingHistoMark);
        this.gradationGraph2.setHistogramHighlight(gradationMaskHistoMark);
        this.lineGradationGraph.setHistogramHighlight(shadingHistoMark);
    }

    protected void writeGUI() {
        if (this.updatingGUI || this.p == null) {
            return;
        }
        try {
            this.updatingGUI = true;

            //map scale
            scaleFormattedTextField.setValue(p.mapScale);

            // stone density
            gradationGraph1.setCurve(p.shadingGradationCurve1.clone());
            gradationGraph2.setCurve(p.shadingGradationCurve2.clone());
            // disable second gradation curve if gradation mask is not loaded
            gradationGraph2.setEnabled(screeData.hasShadingGradationMask());
            String gradationLabel = screeData.hasShadingGradationMask() ? "" :
                DISABLED_GRADATION_NO_MASK;
            gradationGraph2.setLabel(gradationLabel);

            // stone size
            maxDiameterSlider.setValue((int) (p.stoneMaxDiameter / p.mapScale * 1000 * 100));
            minDiameterSlider.setValue((int) (p.stoneMinDiameterScale * 100));
            largeStoneScaleSlider.setValue((int) (p.stoneLargeMaxScale * 100d));

            // distance between stones and obstacles
            minStonesDistSlider.setValue((int) (p.stoneMinDistanceFraction * 100));

            // distance between stones and other obstacles
            minObstaclesDistSlider.setValue((int) (p.stoneMinObstacleDistanceFraction * 100));

            // position jitter of stones
            jitterSlider.setValue((int) (p.stoneMaxPosJitterFraction * 100));

            // stone shape
            minCornersSlider.setValue(p.stoneMinCornerCount);
            maxCornersSlider.setValue(p.stoneMaxCornerCount);
            radiusVariabilitySlider.setValue((int) p.stoneRadiusVariabilityPerc);
            angleVariabilitySlider.setValue((int) p.stoneAngleVariabilityPerc);

            // gully lines
            extractGullyLinesCheckBox.setSelected(p.extractGullyLines);
            lineGradationGraph.setCurve(p.lineGradationCurve.clone());
            lineStoneDistSlider.setValue((int) (p.lineStoneDistFraction * 100));
            gullyLinesScaleTopSlider.setValue((int) (p.lineSizeScaleTop * 100));
            gullyLinesScaleBottomSlider.setValue((int) (p.lineSizeScaleBottom * 100));
            lineToPointDistanceSlider.setValue((int) (p.lineToPointDistFraction * 100));
            minLineDistSlider.setValue((int) (p.lineMinDistance / p.mapScale * 1000));
            minLineLengthSlider.setValue((int) (p.lineMinLengthApprox / p.mapScale * 1000));
            lineMinCurvatureSlider.setValue((int) (p.lineMinCurvature * 10000f));
            lineMinSlopeSlider.setValue((int) Math.round(p.lineMinSlopeDegree));

            // disable the gully lines controls if gullies are not derived
            // from the dem
            Component[] com = linePanel.getComponents();  
            for (int a = 0; a < com.length; a++) {  
                com[a].setEnabled(!screeData.fixedScreeLines);  
            }
            lineGradationGraph.setEnabled(!screeData.fixedScreeLines);
            gradationLabel = !screeData.fixedScreeLines ? "" :
                DISABLED_GRADATION_EXTERNAL_LINES;
            lineGradationGraph.setLabel(gradationLabel);

        } finally {
            this.updatingGUI = false;
        }

    }

    public void readGUI() {
        if (this.updatingGUI || this.p == null) {
            return;
        }
        try {
            this.updatingGUI = true;

            // map scale
            scaleFormattedTextField.commitEdit();
            p.mapScale = ((Number) (scaleFormattedTextField.getValue())).doubleValue();

            // stone density
            p.shadingGradationCurve1 = gradationGraph1.getCurve(0).clone();
            p.shadingGradationCurve2 = gradationGraph2.getCurve(0).clone();

            // stone size
            p.stoneMaxDiameter = maxDiameterSlider.getValue() * p.mapScale / 1000d / 100d;
            p.stoneMinDiameterScale = minDiameterSlider.getValue() / 100d;
            p.stoneLargeMaxScale = largeStoneScaleSlider.getValue() / 100d;

            // distance between stones
            p.stoneMinDistanceFraction = minStonesDistSlider.getValue() / 100d;

            // distance between stones and obstacles
            p.stoneMinObstacleDistanceFraction = minObstaclesDistSlider.getValue() / 100d;

            // position jitter of stones
            p.stoneMaxPosJitterFraction = jitterSlider.getValue() / 100d;

            // stone shape
            p.stoneMinCornerCount = this.minCornersSlider.getValue();
            p.stoneMaxCornerCount = this.maxCornersSlider.getValue();
            p.stoneRadiusVariabilityPerc = this.radiusVariabilitySlider.getValue();
            p.stoneAngleVariabilityPerc = this.angleVariabilitySlider.getValue();

            // gully lines
            p.extractGullyLines = extractGullyLinesCheckBox.isSelected();
            p.lineGradationCurve = lineGradationGraph.getCurve(0).clone();
            p.lineStoneDistFraction = lineStoneDistSlider.getValue() / 100d;
            p.lineSizeScaleTop = gullyLinesScaleTopSlider.getValue() / 100d;
            p.lineSizeScaleBottom = gullyLinesScaleBottomSlider.getValue() / 100d;
            p.lineToPointDistFraction = lineToPointDistanceSlider.getValue() / 100d;
            p.lineMinDistance = minLineDistSlider.getValue() * p.mapScale / 1000;
            p.lineMinLengthApprox = minLineLengthSlider.getValue() * p.mapScale / 1000;
            p.lineMinCurvature = lineMinCurvatureSlider.getValue() / 10000f;
            p.lineMinSlopeDegree = lineMinSlopeSlider.getValue();

        } catch (ParseException ex) {
            // a ParseException is thrown when an invalid number is entered in
            // the scale field.
            String msg = "Please enter a map scale between 1:5,000 and 1:500,000.";
            String title = "Invalid Map Scale";
            ika.utils.ErrorDialog.showErrorDialog(msg, title, null, this);
        } finally {
            this.updatingGUI = false;
        }


    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        curvesButtonGroup = new javax.swing.ButtonGroup();
        tabbedPane = new javax.swing.JTabbedPane();
        jPanel5 = new ika.gui.TransparentMacPanel();
        stoneSizePanel = new ika.gui.TransparentMacPanel();
        shadowLabel = new javax.swing.JLabel();
        sunLabel = new javax.swing.JLabel();
        minDiameterLabel = new javax.swing.JLabel();
        maxDiameterLabel = new javax.swing.JLabel();
        largeStoneScaleSlider = new javax.swing.JSlider();
        largeStonesLabel = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        minDiameterSlider = new javax.swing.JSlider();
        jLabel51 = new javax.swing.JLabel();
        gullyLinesScaleTopSlider = new javax.swing.JSlider();
        jLabel52 = new javax.swing.JLabel();
        scaleFormattedTextField = new javax.swing.JFormattedTextField();
        javax.swing.JLabel jLabel21 = new javax.swing.JLabel();
        maxDiameterSlider = new javax.swing.JSlider();
        gullyScaleTopLabel = new javax.swing.JLabel();
        gullyLinesScaleBottomSlider = new javax.swing.JSlider();
        jLabel53 = new javax.swing.JLabel();
        gullyScaleBottomLabel = new javax.swing.JLabel();
        jPanel1 = new ika.gui.TransparentMacPanel();
        jPanel8 = new ika.gui.TransparentMacPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        minStonesDistSlider = new javax.swing.JSlider();
        minStoneDistLabel = new javax.swing.JLabel();
        minObstacleDistLabel = new javax.swing.JLabel();
        minObstaclesDistSlider = new javax.swing.JSlider();
        jLabel40 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel22 = new javax.swing.JLabel();
        jitterSlider = new javax.swing.JSlider();
        jitterLabel = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        lineToPointDistanceSlider = new javax.swing.JSlider();
        jLabel41 = new javax.swing.JLabel();
        lineToPointLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel29 = new javax.swing.JLabel();
        lineStoneDistSlider = new javax.swing.JSlider();
        lineStoneDistLabel = new javax.swing.JLabel();
        jPanel4 = new ika.gui.TransparentMacPanel();
        densityPanel = new ika.gui.TransparentMacPanel();
        gradationPanel1 = new javax.swing.JPanel();
        gradationPanel2 = new javax.swing.JPanel();
        jLabel27 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        densityHelpButton = new javax.swing.JButton();
        jPanel6 = new ika.gui.TransparentMacPanel();
        stoneShapePanel = new ika.gui.TransparentMacPanel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        minCornersSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        angleVariabilityLabel = new javax.swing.JLabel();
        angleVariabilitySlider = new javax.swing.JSlider();
        radiusVariabilityLabel = new javax.swing.JLabel();
        radiusVariabilitySlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        maxCornersSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel13 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel7 = new ika.gui.TransparentMacPanel();
        javax.swing.JPanel jPanel2 = new ika.gui.TransparentMacPanel();
        linePanel = new ika.gui.TransparentMacPanel();
        extractGullyLinesCheckBox = new javax.swing.JCheckBox();
        javax.swing.JLabel jLabel32 = new javax.swing.JLabel();
        minLineDistLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel34 = new javax.swing.JLabel();
        minLineLengthLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel36 = new javax.swing.JLabel();
        lineMinSlopeSlider = new javax.swing.JSlider();
        slopeLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel50 = new javax.swing.JLabel();
        lineMinCurvatureSlider = new javax.swing.JSlider();
        lineMinCurvatureLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        gullyLinesDensityLabel = new javax.swing.JLabel();
        gullyGradationPanel = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        minLineLengthSlider = new javax.swing.JSlider();
        minLineDistSlider = new javax.swing.JSlider();

        setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 20, 0));

        stoneSizePanel.setLayout(new java.awt.GridBagLayout());

        shadowLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        shadowLabel.setText("Shadow");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 3);
        stoneSizePanel.add(shadowLabel, gridBagConstraints);

        sunLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        sunLabel.setText("Sun");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 12, 0, 3);
        stoneSizePanel.add(sunLabel, gridBagConstraints);

        minDiameterLabel.setText("0.00 mm");
        minDiameterLabel.setPreferredSize(new java.awt.Dimension(70, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        stoneSizePanel.add(minDiameterLabel, gridBagConstraints);

        maxDiameterLabel.setText("0.00 mm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        stoneSizePanel.add(maxDiameterLabel, gridBagConstraints);

        largeStoneScaleSlider.setMajorTickSpacing(50);
        largeStoneScaleSlider.setMaximum(300);
        largeStoneScaleSlider.setMinimum(100);
        largeStoneScaleSlider.setMinorTickSpacing(10);
        largeStoneScaleSlider.setPaintLabels(true);
        largeStoneScaleSlider.setPaintTicks(true);
        largeStoneScaleSlider.setValue(-1);
        largeStoneScaleSlider.setPreferredSize(new java.awt.Dimension(130, 54));
        largeStoneScaleSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                largeStoneScaleSlidershapeSliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        stoneSizePanel.add(largeStoneScaleSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = largeStoneScaleSlider.createStandardLabels(100);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            largeStoneScaleSlider.setLabelTable(labels);
        }

        largeStonesLabel.setText("0.00 mm");
        largeStonesLabel.setPreferredSize(new java.awt.Dimension(90, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        stoneSizePanel.add(largeStonesLabel, gridBagConstraints);

        jLabel10.setText("Stone Diameters");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(40, 0, 10, 0);
        stoneSizePanel.add(jLabel10, gridBagConstraints);

        jLabel48.setText("Stone Diameters on Large Stones Mask");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(40, 0, 10, 0);
        stoneSizePanel.add(jLabel48, gridBagConstraints);

        jLabel49.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel49.setText("<html>Stones on the Large Stones Mask<br>are enlarged by this factor.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        stoneSizePanel.add(jLabel49, gridBagConstraints);

        minDiameterSlider.setMajorTickSpacing(25);
        minDiameterSlider.setMinorTickSpacing(5);
        minDiameterSlider.setPaintLabels(true);
        minDiameterSlider.setPaintTicks(true);
        minDiameterSlider.setValue(-1);
        minDiameterSlider.setPreferredSize(new java.awt.Dimension(130, 54));
        minDiameterSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                minDiameterSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        stoneSizePanel.add(minDiameterSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = minDiameterSlider.createStandardLabels(50);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            minDiameterSlider.setLabelTable(labels);
        }

        jLabel51.setText("Stone Diameters on Gully Lines");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(40, 0, 10, 0);
        stoneSizePanel.add(jLabel51, gridBagConstraints);

        gullyLinesScaleTopSlider.setMajorTickSpacing(50);
        gullyLinesScaleTopSlider.setMaximum(200);
        gullyLinesScaleTopSlider.setMinimum(100);
        gullyLinesScaleTopSlider.setMinorTickSpacing(10);
        gullyLinesScaleTopSlider.setPaintLabels(true);
        gullyLinesScaleTopSlider.setPaintTicks(true);
        gullyLinesScaleTopSlider.setValue(-1);
        gullyLinesScaleTopSlider.setPreferredSize(new java.awt.Dimension(130, 54));
        gullyLinesScaleTopSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                gullyLinesScaleTopSlidershapeSliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        stoneSizePanel.add(gullyLinesScaleTopSlider, gridBagConstraints);
        {
            javax.swing.JSlider slider = gullyLinesScaleTopSlider;
            java.util.Hashtable labels = slider.createStandardLabels(slider.getMajorTickSpacing());
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            slider.setLabelTable(labels);
        }

        jLabel52.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel52.setText("<html>The highest stones in gully lines<br>are enlarged by this factor.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 20, 0);
        stoneSizePanel.add(jLabel52, gridBagConstraints);

        scaleFormattedTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        javax.swing.text.InternationalFormatter intFormat;
        intFormat = (InternationalFormatter)scaleFormattedTextField.getFormatter();
        intFormat.setMinimum(5000);
        intFormat.setMaximum(500000);
        scaleFormattedTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scaleFormattedTextFieldActionPerformed(evt);
            }
        });
        scaleFormattedTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                scaleFormattedTextFieldFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        stoneSizePanel.add(scaleFormattedTextField, gridBagConstraints);

        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel21.setText("Map Scale 1:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        stoneSizePanel.add(jLabel21, gridBagConstraints);

        maxDiameterSlider.setMajorTickSpacing(10);
        maxDiameterSlider.setMaximum(50);
        maxDiameterSlider.setMinimum(10);
        maxDiameterSlider.setMinorTickSpacing(5);
        maxDiameterSlider.setPaintLabels(true);
        maxDiameterSlider.setPaintTicks(true);
        maxDiameterSlider.setValue(22);
        maxDiameterSlider.setPreferredSize(new java.awt.Dimension(180, 54));
        maxDiameterSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                maxDiameterSliderStateChanged(evt);
            }
        });
        {
            Hashtable labelTable = new Hashtable();
            labelTable.put( new Integer( 10 ), new JLabel("0.1") );
            labelTable.put( new Integer( 30 ), new JLabel("0.3") );
            labelTable.put( new Integer( 50 ), new JLabel("0.5") );
            maxDiameterSlider.setLabelTable( labelTable );
        }
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        stoneSizePanel.add(maxDiameterSlider, gridBagConstraints);
        {
            Font font = maxDiameterSlider.getFont();
            Dictionary labelTable = maxDiameterSlider.getLabelTable();
            for ( Enumeration e = labelTable.elements(); e.hasMoreElements(); ) {
                Object element = e.nextElement();
                if ( element instanceof JComponent ) {
                    ((JComponent)element).setFont( font );
                    // Update the label size and slider layout
                    ((JComponent)element).setSize( ((JComponent)element).getPreferredSize() );
                }
            }

        }

        gullyScaleTopLabel.setText("0.00 mm");
        gullyScaleTopLabel.setPreferredSize(new java.awt.Dimension(90, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        stoneSizePanel.add(gullyScaleTopLabel, gridBagConstraints);

        gullyLinesScaleBottomSlider.setMajorTickSpacing(50);
        gullyLinesScaleBottomSlider.setMaximum(200);
        gullyLinesScaleBottomSlider.setMinimum(100);
        gullyLinesScaleBottomSlider.setMinorTickSpacing(10);
        gullyLinesScaleBottomSlider.setPaintLabels(true);
        gullyLinesScaleBottomSlider.setPaintTicks(true);
        gullyLinesScaleBottomSlider.setValue(-1);
        gullyLinesScaleBottomSlider.setPreferredSize(new java.awt.Dimension(130, 54));
        gullyLinesScaleBottomSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                gullyLinesScaleBottomSlidershapeSliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        stoneSizePanel.add(gullyLinesScaleBottomSlider, gridBagConstraints);
        {
            javax.swing.JSlider slider = gullyLinesScaleBottomSlider;
            java.util.Hashtable labels = slider.createStandardLabels(slider.getMajorTickSpacing());
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            slider.setLabelTable(labels);
        }

        jLabel53.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel53.setText("<html>The lowest stones in gully lines<br>are enlarged by this factor.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        stoneSizePanel.add(jLabel53, gridBagConstraints);

        gullyScaleBottomLabel.setText("0.00 mm");
        gullyScaleBottomLabel.setPreferredSize(new java.awt.Dimension(90, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        stoneSizePanel.add(gullyScaleBottomLabel, gridBagConstraints);

        jPanel5.add(stoneSizePanel);

        tabbedPane.addTab("Size", jPanel5);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 20, 10));

        jPanel8.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Minimum Distances");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        jPanel8.add(jLabel1, gridBagConstraints);

        jLabel38.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel38.setText("Distance between Stones");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel8.add(jLabel38, gridBagConstraints);

        minStonesDistSlider.setMajorTickSpacing(25);
        minStonesDistSlider.setMinorTickSpacing(5);
        minStonesDistSlider.setPaintLabels(true);
        minStonesDistSlider.setPaintTicks(true);
        minStonesDistSlider.setValue(-1);
        minStonesDistSlider.setPreferredSize(new java.awt.Dimension(200, 54));
        minStonesDistSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                minStonesDistSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel8.add(minStonesDistSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = minStonesDistSlider.createStandardLabels(50);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            minStonesDistSlider.setLabelTable(labels);
        }

        minStoneDistLabel.setText("mm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel8.add(minStoneDistLabel, gridBagConstraints);

        minObstacleDistLabel.setText("mm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel8.add(minObstacleDistLabel, gridBagConstraints);

        minObstaclesDistSlider.setMajorTickSpacing(25);
        minObstaclesDistSlider.setMinorTickSpacing(5);
        minObstaclesDistSlider.setPaintLabels(true);
        minObstaclesDistSlider.setPaintTicks(true);
        minObstaclesDistSlider.setValue(-1);
        minObstaclesDistSlider.setPreferredSize(new java.awt.Dimension(130, 54));
        minObstaclesDistSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                minObstaclesDistSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel8.add(minObstaclesDistSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = minObstaclesDistSlider.createStandardLabels(50);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            minObstaclesDistSlider.setLabelTable(labels);
        }

        jLabel40.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel40.setText("Distance to Obstacles");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 0, 0);
        jPanel8.add(jLabel40, gridBagConstraints);

        jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel22.setText("Position Jitter");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(40, 0, 5, 0);
        jPanel8.add(jLabel22, gridBagConstraints);

        jitterSlider.setMajorTickSpacing(25);
        jitterSlider.setMaximum(50);
        jitterSlider.setMinorTickSpacing(5);
        jitterSlider.setPaintLabels(true);
        jitterSlider.setPaintTicks(true);
        jitterSlider.setValue(-1);
        jitterSlider.setPreferredSize(new java.awt.Dimension(130, 54));
        jitterSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jitterSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel8.add(jitterSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = jitterSlider.createStandardLabels(25);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            jitterSlider.setLabelTable(labels);
        }

        jitterLabel.setText("mm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel8.add(jitterLabel, gridBagConstraints);

        jLabel16.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel16.setText("<html>All values are relative to the diameter <br>of a stone in the shadow.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        jPanel8.add(jLabel16, gridBagConstraints);

        lineToPointDistanceSlider.setMajorTickSpacing(100);
        lineToPointDistanceSlider.setMaximum(200);
        lineToPointDistanceSlider.setMinorTickSpacing(10);
        lineToPointDistanceSlider.setPaintLabels(true);
        lineToPointDistanceSlider.setPaintTicks(true);
        lineToPointDistanceSlider.setValue(-1);
        lineToPointDistanceSlider.setPreferredSize(new java.awt.Dimension(130, 54));
        lineToPointDistanceSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                lineToPointDistanceSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel8.add(lineToPointDistanceSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = lineToPointDistanceSlider.createStandardLabels(100);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            lineToPointDistanceSlider.setLabelTable(labels);
        }

        jLabel41.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel41.setText("White Space around Gully Lines");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 0, 0);
        jPanel8.add(jLabel41, gridBagConstraints);

        lineToPointLabel.setText("mm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel8.add(lineToPointLabel, gridBagConstraints);

        jLabel29.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel29.setText("Distance between Stones on Gully Lines");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 20, 0, 3);
        jPanel8.add(jLabel29, gridBagConstraints);

        lineStoneDistSlider.setMajorTickSpacing(100);
        lineStoneDistSlider.setMinorTickSpacing(10);
        lineStoneDistSlider.setPaintLabels(true);
        lineStoneDistSlider.setPaintTicks(true);
        lineStoneDistSlider.setPreferredSize(new java.awt.Dimension(160, 54));
        lineStoneDistSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                lineStoneDistSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel8.add(lineStoneDistSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = lineStoneDistSlider.createStandardLabels(50);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            lineStoneDistSlider.setLabelTable(labels);
        }

        lineStoneDistLabel.setText("20 mm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel8.add(lineStoneDistLabel, gridBagConstraints);

        jPanel1.add(jPanel8);

        tabbedPane.addTab("Spread", jPanel1);

        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 20, 10));

        densityPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 7, 7, 7));
        densityPanel.setLayout(new java.awt.GridBagLayout());

        gradationPanel1.setToolTipText("Place and move control points to adjust the density and size of stones.");
        gradationPanel1.setPreferredSize(new java.awt.Dimension(256, 256));
        gradationPanel1.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        densityPanel.add(gradationPanel1, gridBagConstraints);
        gradationPanel1.add(gradationGraph1, java.awt.BorderLayout.CENTER);
        gradationPanel1.setPreferredSize(gradationGraph1.getPreferredSize());

        gradationPanel2.setPreferredSize(new java.awt.Dimension(256, 256));
        gradationPanel2.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        densityPanel.add(gradationPanel2, gridBagConstraints);
        gradationPanel2.add(gradationGraph2, java.awt.BorderLayout.CENTER);
        gradationPanel2.setPreferredSize(gradationGraph2.getPreferredSize());

        jLabel27.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel27.setText("Stone Size and Density on Gradation Mask");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 5, 0);
        densityPanel.add(jLabel27, gridBagConstraints);

        jLabel30.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel30.setText("Stone Size and Density");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        densityPanel.add(jLabel30, gridBagConstraints);

        densityHelpButton.setText("Help");
        densityHelpButton.setPreferredSize(new java.awt.Dimension(70, 29));
        if (ika.utils.Sys.isMacOSX_10_5_orHigher()) {
            densityHelpButton.setText("");
            densityHelpButton.putClientProperty("JButton.buttonType", "help");
        }
        densityHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                densityHelpButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        densityPanel.add(densityHelpButton, gridBagConstraints);

        jPanel4.add(densityPanel);

        tabbedPane.addTab("Density", jPanel4);

        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 20, 10));

        stoneShapePanel.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Minimum");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        stoneShapePanel.add(jLabel3, gridBagConstraints);

        minCornersSlider.setMajorTickSpacing(1);
        minCornersSlider.setMaximum(10);
        minCornersSlider.setMinimum(3);
        minCornersSlider.setPaintLabels(true);
        minCornersSlider.setPaintTicks(true);
        minCornersSlider.setSnapToTicks(true);
        minCornersSlider.setValue(4);
        minCornersSlider.setPreferredSize(new java.awt.Dimension(150, 54));
        minCornersSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                minCornersSlidershapeSliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        stoneShapePanel.add(minCornersSlider, gridBagConstraints);

        jLabel5.setText("Maximum");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        stoneShapePanel.add(jLabel5, gridBagConstraints);

        angleVariabilityLabel.setPreferredSize(new java.awt.Dimension(50, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        stoneShapePanel.add(angleVariabilityLabel, gridBagConstraints);

        angleVariabilitySlider.setMajorTickSpacing(25);
        angleVariabilitySlider.setMinorTickSpacing(5);
        angleVariabilitySlider.setPaintLabels(true);
        angleVariabilitySlider.setPaintTicks(true);
        angleVariabilitySlider.setValue(-1);
        angleVariabilitySlider.setPreferredSize(new java.awt.Dimension(150, 54));
        angleVariabilitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                angleVariabilitySliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 6;
        stoneShapePanel.add(angleVariabilitySlider, gridBagConstraints);
        {
            java.util.Hashtable labels = angleVariabilitySlider.createStandardLabels(50);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            angleVariabilitySlider.setLabelTable(labels);
        }

        radiusVariabilityLabel.setPreferredSize(new java.awt.Dimension(50, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        stoneShapePanel.add(radiusVariabilityLabel, gridBagConstraints);

        radiusVariabilitySlider.setMajorTickSpacing(25);
        radiusVariabilitySlider.setMinorTickSpacing(5);
        radiusVariabilitySlider.setPaintLabels(true);
        radiusVariabilitySlider.setPaintTicks(true);
        radiusVariabilitySlider.setValue(-1);
        radiusVariabilitySlider.setPreferredSize(new java.awt.Dimension(150, 54));
        radiusVariabilitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                radiusVariabilitySliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        stoneShapePanel.add(radiusVariabilitySlider, gridBagConstraints);
        {
            java.util.Hashtable labels = radiusVariabilitySlider.createStandardLabels(50);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "%");
                }
            }
            radiusVariabilitySlider.setLabelTable(labels);
        }

        jLabel12.setText("Radius");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        stoneShapePanel.add(jLabel12, gridBagConstraints);

        maxCornersSlider.setMajorTickSpacing(1);
        maxCornersSlider.setMaximum(10);
        maxCornersSlider.setMinimum(3);
        maxCornersSlider.setPaintLabels(true);
        maxCornersSlider.setPaintTicks(true);
        maxCornersSlider.setSnapToTicks(true);
        maxCornersSlider.setValue(8);
        maxCornersSlider.setPreferredSize(new java.awt.Dimension(150, 54));
        maxCornersSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                maxCornersSlidershapeSliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        stoneShapePanel.add(maxCornersSlider, gridBagConstraints);

        jLabel13.setText("Angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        stoneShapePanel.add(jLabel13, gridBagConstraints);

        jLabel8.setText("Number of Corners");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        stoneShapePanel.add(jLabel8, gridBagConstraints);

        jLabel2.setText("Shape Variability");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 10, 0);
        stoneShapePanel.add(jLabel2, gridBagConstraints);

        jPanel6.add(stoneShapePanel);

        tabbedPane.addTab("Shape", jPanel6);

        jPanel7.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 0, 0, 0));
        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 20, 0));

        jPanel2.setLayout(new java.awt.GridBagLayout());

        linePanel.setLayout(new java.awt.GridBagLayout());

        extractGullyLinesCheckBox.setSelected(true);
        extractGullyLinesCheckBox.setText("Extract Gully Lines from Terrain Model");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        linePanel.add(extractGullyLinesCheckBox, gridBagConstraints);

        jLabel32.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel32.setText("Minimum Distance between Gullies");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 40, 0, 0);
        linePanel.add(jLabel32, gridBagConstraints);

        minLineDistLabel.setText("20 mm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        linePanel.add(minLineDistLabel, gridBagConstraints);

        jLabel34.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel34.setText("Minimum Length (Approx.)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 40, 0, 0);
        linePanel.add(jLabel34, gridBagConstraints);

        minLineLengthLabel.setText("20 mm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        linePanel.add(minLineLengthLabel, gridBagConstraints);

        jLabel36.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel36.setText("Minimum Slope");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 40, 0, 0);
        linePanel.add(jLabel36, gridBagConstraints);

        lineMinSlopeSlider.setMajorTickSpacing(15);
        lineMinSlopeSlider.setMaximum(45);
        lineMinSlopeSlider.setMinorTickSpacing(5);
        lineMinSlopeSlider.setPaintLabels(true);
        lineMinSlopeSlider.setPaintTicks(true);
        lineMinSlopeSlider.setValue(10);
        lineMinSlopeSlider.setPreferredSize(new java.awt.Dimension(130, 54));
        lineMinSlopeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                lineMinSlopeSlidershapeSliderChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 40, 0, 0);
        linePanel.add(lineMinSlopeSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = lineMinSlopeSlider.createStandardLabels(15);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00B0");
                }
            }
            lineMinSlopeSlider.setLabelTable(labels);
        }

        slopeLabel.setPreferredSize(new java.awt.Dimension(30, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        linePanel.add(slopeLabel, gridBagConstraints);

        jLabel50.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel50.setText("Minimum Curvature");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 40, 0, 0);
        linePanel.add(jLabel50, gridBagConstraints);

        lineMinCurvatureSlider.setMajorTickSpacing(50);
        lineMinCurvatureSlider.setMinimum(-50);
        lineMinCurvatureSlider.setMinorTickSpacing(10);
        lineMinCurvatureSlider.setPaintTicks(true);
        lineMinCurvatureSlider.setValue(10);
        lineMinCurvatureSlider.setPreferredSize(new java.awt.Dimension(130, 29));
        lineMinCurvatureSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                lineMinCurvatureSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 40, 0, 0);
        linePanel.add(lineMinCurvatureSlider, gridBagConstraints);

        lineMinCurvatureLabel.setPreferredSize(new java.awt.Dimension(30, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        linePanel.add(lineMinCurvatureLabel, gridBagConstraints);

        jLabel7.setText("Gully Lines Geometry");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        linePanel.add(jLabel7, gridBagConstraints);

        gullyLinesDensityLabel.setText("Gully Lines Density");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 5, 0);
        linePanel.add(gullyLinesDensityLabel, gridBagConstraints);

        gullyGradationPanel.setPreferredSize(new java.awt.Dimension(256, 256));
        gullyGradationPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 3;
        linePanel.add(gullyGradationPanel, gridBagConstraints);
        gullyGradationPanel.add(lineGradationGraph, java.awt.BorderLayout.CENTER);
        gullyGradationPanel.setPreferredSize(lineGradationGraph.getPreferredSize());

        jLabel15.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel15.setText("<html>This gradation curve adjusts the density of <br>gully lines. Where the shaded relief is darker, <br>more lines are generated.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        linePanel.add(jLabel15, gridBagConstraints);

        minLineLengthSlider.setMajorTickSpacing(10);
        minLineLengthSlider.setMaximum(30);
        minLineLengthSlider.setMinorTickSpacing(5);
        minLineLengthSlider.setPaintLabels(true);
        minLineLengthSlider.setPaintTicks(true);
        minLineLengthSlider.setValue(6);
        minLineLengthSlider.setPreferredSize(new java.awt.Dimension(160, 54));
        minLineLengthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                minLineLengthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 40, 0, 0);
        linePanel.add(minLineLengthSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = minLineLengthSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "mm");
                }
            }
            minLineLengthSlider.setLabelTable(labels);
        }

        minLineDistSlider.setMajorTickSpacing(10);
        minLineDistSlider.setMaximum(20);
        minLineDistSlider.setMinorTickSpacing(5);
        minLineDistSlider.setPaintLabels(true);
        minLineDistSlider.setPaintTicks(true);
        minLineDistSlider.setValue(2);
        minLineDistSlider.setPreferredSize(new java.awt.Dimension(130, 54));
        minLineDistSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                minLineDistSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 40, 0, 0);
        linePanel.add(minLineDistSlider, gridBagConstraints);
        {
            java.util.Hashtable labels = minLineDistSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "mm");
                }
            }
            minLineDistSlider.setLabelTable(labels);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(linePanel, gridBagConstraints);

        jPanel7.add(jPanel2);

        tabbedPane.addTab("Gullies", jPanel7);

        add(tabbedPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

private void minCornersSlidershapeSliderChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_minCornersSlidershapeSliderChanged

    if (updatingGUI) {
        return;
    }
    try {
        updatingGUI = true;
        int min = minCornersSlider.getValue();
        int max = maxCornersSlider.getValue();
        if (min > max) {
            maxCornersSlider.setValue(min);
        }
    } finally {
        updatingGUI = false;
    }

}//GEN-LAST:event_minCornersSlidershapeSliderChanged

private void maxCornersSlidershapeSliderChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_maxCornersSlidershapeSliderChanged
    if (updatingGUI) {
        return;
    }
    try {
        updatingGUI = true;
        int min = minCornersSlider.getValue();
        int max = maxCornersSlider.getValue();
        if (max < min) {
            minCornersSlider.setValue(max);
        }
    } finally {
        updatingGUI = false;
    }
}//GEN-LAST:event_maxCornersSlidershapeSliderChanged

private void scaleFormattedTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scaleFormattedTextFieldActionPerformed
    writeLabels();
}//GEN-LAST:event_scaleFormattedTextFieldActionPerformed

private void largeStoneScaleSlidershapeSliderChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_largeStoneScaleSlidershapeSliderChanged
    writeLabels();
}//GEN-LAST:event_largeStoneScaleSlidershapeSliderChanged

private void lineMinSlopeSlidershapeSliderChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lineMinSlopeSlidershapeSliderChanged
    writeLabels();
}//GEN-LAST:event_lineMinSlopeSlidershapeSliderChanged

    public boolean saveSettings() {
        this.readGUI();

        Frame frame = ika.gui.GUIUtil.getOwnerFrame(this);
        String filePath = FileUtils.askFile(frame, "Save Scree Painter Settings",
                "scree settings.txt", false, "txt");
        if (filePath == null) {
            return false;
        }
        String str = p.toString();
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filePath));
            out.write(str);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            ika.utils.ErrorDialog.showErrorDialog("The settings could not be written.", e);
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public void loadSettings(String filePath) throws IOException {
        File file = new File(filePath);
        this.p.fromString(new String(FileUtils.getBytesFromFile(file)));
        this.writeGUI();
    }

    private void writeLabels() {
        DecimalFormat format = new DecimalFormat("0.00");
        String str;
        readGUI();

        str = format.format(p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.maxDiameterLabel.setFont(this.maxDiameterSlider.getFont());
        this.maxDiameterLabel.setText(str);

        str = format.format(p.stoneMinDiameterScale * p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.minDiameterLabel.setFont(this.minDiameterSlider.getFont());
        this.minDiameterLabel.setText(str);

        str = format.format(p.stoneLargeMaxScale * p.stoneMaxDiameter * p.stoneMinDiameterScale / p.mapScale * 1000);
        str += " - ";
        str += format.format(p.stoneLargeMaxScale * p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.largeStonesLabel.setFont(this.largeStoneScaleSlider.getFont());
        this.largeStonesLabel.setText(str);

        str = format.format(p.lineSizeScaleTop * p.stoneMaxDiameter * p.stoneMinDiameterScale / p.mapScale * 1000);
        str += " - ";
        str += format.format(p.lineSizeScaleTop * p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.gullyScaleTopLabel.setFont(this.gullyLinesScaleTopSlider.getFont());
        this.gullyScaleTopLabel.setText(str);

        str = format.format(p.lineSizeScaleBottom * p.stoneMaxDiameter * p.stoneMinDiameterScale / p.mapScale * 1000);
        str += " - ";
        str += format.format(p.lineSizeScaleBottom * p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.gullyScaleBottomLabel.setFont(this.gullyLinesScaleBottomSlider.getFont());
        this.gullyScaleBottomLabel.setText(str);

        str = format.format(p.lineToPointDistFraction * p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.lineToPointLabel.setFont(this.lineToPointDistanceSlider.getFont());
        this.lineToPointLabel.setText(str);

        str = format.format(p.stoneMinDistanceFraction * p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.minStoneDistLabel.setFont(this.minStonesDistSlider.getFont());
        this.minStoneDistLabel.setText(str);

        str = format.format(p.stoneMinObstacleDistanceFraction * p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.minObstacleDistLabel.setFont(this.minObstaclesDistSlider.getFont());
        this.minObstacleDistLabel.setText(str);

        str = format.format(p.stoneMaxPosJitterFraction * p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.jitterLabel.setFont(this.jitterSlider.getFont());
        this.jitterLabel.setText(str);

        str = format.format(p.lineStoneDistFraction * p.stoneMaxDiameter / p.mapScale * 1000);
        str += " mm";
        this.lineStoneDistLabel.setFont(this.lineStoneDistSlider.getFont());
        this.lineStoneDistLabel.setText(str);

        str = (int) (p.lineMinDistance / p.mapScale * 1000) + " mm";
        this.minLineDistLabel.setFont(this.minLineDistSlider.getFont());
        this.minLineDistLabel.setText(str);

        str = (int) (p.lineMinLengthApprox / p.mapScale * 1000) + " mm";
        this.minLineLengthLabel.setFont(this.minLineLengthSlider.getFont());
        this.minLineLengthLabel.setText(str);

        str = Integer.toString(this.lineMinCurvatureSlider.getValue());
        this.lineMinCurvatureLabel.setFont(lineMinCurvatureSlider.getFont());
        this.lineMinCurvatureLabel.setText(str);

        this.slopeLabel.setText(lineMinSlopeSlider.getValue() + "\u00B0");
        this.slopeLabel.setFont(lineMinSlopeSlider.getFont());
    }

private void minDiameterSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_minDiameterSliderStateChanged
    writeLabels();
}//GEN-LAST:event_minDiameterSliderStateChanged

private void radiusVariabilitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_radiusVariabilitySliderStateChanged
    int v = radiusVariabilitySlider.getValue();
    radiusVariabilityLabel.setText("\u00B1" + v + "%");
    radiusVariabilityLabel.setFont(radiusVariabilitySlider.getFont());
}//GEN-LAST:event_radiusVariabilitySliderStateChanged

private void angleVariabilitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_angleVariabilitySliderStateChanged
    int v = angleVariabilitySlider.getValue();
    angleVariabilityLabel.setText("\u00B1" + v + "%");
    angleVariabilityLabel.setFont(angleVariabilitySlider.getFont());
}//GEN-LAST:event_angleVariabilitySliderStateChanged

private void gullyLinesScaleTopSlidershapeSliderChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_gullyLinesScaleTopSlidershapeSliderChanged
    writeLabels();
}//GEN-LAST:event_gullyLinesScaleTopSlidershapeSliderChanged

private void minStonesDistSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_minStonesDistSliderStateChanged
    writeLabels();
}//GEN-LAST:event_minStonesDistSliderStateChanged

private void minObstaclesDistSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_minObstaclesDistSliderStateChanged
    writeLabels();
}//GEN-LAST:event_minObstaclesDistSliderStateChanged

private void jitterSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jitterSliderStateChanged
    writeLabels();
}//GEN-LAST:event_jitterSliderStateChanged

private void scaleFormattedTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_scaleFormattedTextFieldFocusLost
    writeLabels();
}//GEN-LAST:event_scaleFormattedTextFieldFocusLost

private void maxDiameterSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_maxDiameterSliderStateChanged
    writeLabels();
}//GEN-LAST:event_maxDiameterSliderStateChanged

private void lineMinCurvatureSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lineMinCurvatureSliderStateChanged
    writeLabels();
}//GEN-LAST:event_lineMinCurvatureSliderStateChanged

private void minLineDistSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_minLineDistSliderStateChanged
    writeLabels();
}//GEN-LAST:event_minLineDistSliderStateChanged

private void minLineLengthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_minLineLengthSliderStateChanged
    writeLabels();
}//GEN-LAST:event_minLineLengthSliderStateChanged

private void lineStoneDistSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lineStoneDistSliderStateChanged
    writeLabels();
}//GEN-LAST:event_lineStoneDistSliderStateChanged

private void gullyLinesScaleBottomSlidershapeSliderChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_gullyLinesScaleBottomSlidershapeSliderChanged
    writeLabels();
}//GEN-LAST:event_gullyLinesScaleBottomSlidershapeSliderChanged

private void lineToPointDistanceSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lineToPointDistanceSliderStateChanged
    writeLabels();
}//GEN-LAST:event_lineToPointDistanceSliderStateChanged

private void densityHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_densityHelpButtonActionPerformed
    Properties props =
            ika.utils.PropertiesLoader.loadProperties("ika.app.Application.properties");
    String url = props.getProperty("DensityHelpWebPage");
    ika.utils.BrowserLauncherWrapper.openURL(url);
}//GEN-LAST:event_densityHelpButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel angleVariabilityLabel;
    private javax.swing.JSlider angleVariabilitySlider;
    private javax.swing.ButtonGroup curvesButtonGroup;
    private javax.swing.JButton densityHelpButton;
    private javax.swing.JPanel densityPanel;
    private javax.swing.JCheckBox extractGullyLinesCheckBox;
    private javax.swing.JPanel gradationPanel1;
    private javax.swing.JPanel gradationPanel2;
    private javax.swing.JPanel gullyGradationPanel;
    private javax.swing.JLabel gullyLinesDensityLabel;
    private javax.swing.JSlider gullyLinesScaleBottomSlider;
    private javax.swing.JSlider gullyLinesScaleTopSlider;
    private javax.swing.JLabel gullyScaleBottomLabel;
    private javax.swing.JLabel gullyScaleTopLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JLabel jitterLabel;
    private javax.swing.JSlider jitterSlider;
    private javax.swing.JSlider largeStoneScaleSlider;
    private javax.swing.JLabel largeStonesLabel;
    private javax.swing.JLabel lineMinCurvatureLabel;
    private javax.swing.JSlider lineMinCurvatureSlider;
    private javax.swing.JSlider lineMinSlopeSlider;
    private javax.swing.JPanel linePanel;
    private javax.swing.JLabel lineStoneDistLabel;
    private javax.swing.JSlider lineStoneDistSlider;
    private javax.swing.JSlider lineToPointDistanceSlider;
    private javax.swing.JLabel lineToPointLabel;
    private javax.swing.JSlider maxCornersSlider;
    private javax.swing.JLabel maxDiameterLabel;
    private javax.swing.JSlider maxDiameterSlider;
    private javax.swing.JSlider minCornersSlider;
    private javax.swing.JLabel minDiameterLabel;
    private javax.swing.JSlider minDiameterSlider;
    private javax.swing.JLabel minLineDistLabel;
    private javax.swing.JSlider minLineDistSlider;
    private javax.swing.JLabel minLineLengthLabel;
    private javax.swing.JSlider minLineLengthSlider;
    private javax.swing.JLabel minObstacleDistLabel;
    private javax.swing.JSlider minObstaclesDistSlider;
    private javax.swing.JLabel minStoneDistLabel;
    private javax.swing.JSlider minStonesDistSlider;
    private javax.swing.JLabel radiusVariabilityLabel;
    private javax.swing.JSlider radiusVariabilitySlider;
    private javax.swing.JFormattedTextField scaleFormattedTextField;
    private javax.swing.JLabel shadowLabel;
    private javax.swing.JLabel slopeLabel;
    private javax.swing.JPanel stoneShapePanel;
    private javax.swing.JPanel stoneSizePanel;
    private javax.swing.JLabel sunLabel;
    private javax.swing.JTabbedPane tabbedPane;
    // End of variables declaration//GEN-END:variables
}
