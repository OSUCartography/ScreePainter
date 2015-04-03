package ika.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.swing.JComponent;

public class GradationGraph extends JComponent implements MouseListener, MouseMotionListener, KeyListener {

    public static void main(String args[]) {

        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            
            public void run() {
                /*
                GradationGraph.Curve curve = new GradationGraph.Curve();
                curve.addKnot(0, 0);
                curve.addKnot(0.5f, 0.7f);
                curve.addKnot(1, 1);
                 */
                javax.swing.JFrame frame = new javax.swing.JFrame("Gradation Curve");
                GradationGraph graph = new GradationGraph();
                //               paintCurve.setCurves(new GradationGraph.Curve[]{curve});
                frame.getContentPane().add(graph);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
    private static final Color EXTRA_LIGHT_GRAY = new Color(220, 220, 220);
    private static final int GRADIENT_BAR_WIDTH = 5;

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    static public class Curve implements Cloneable{

        public float[] x;
        public float[] y;

        public Curve() {
            x = new float[]{0, 1};
            y = new float[]{0, 1};
        }

        public Curve(String str) {
            x = new float[0];
            y = new float[0];
            this.fromString(str);
        }

        public Curve(Curve curve) {
            x = (float[]) curve.x.clone();
            y = (float[]) curve.y.clone();
        }

        
        public Curve clone() {
            return new Curve (this);
        }

        public int addKnot(float kx, float ky) {
            int pos = findKnot(kx, ky);
            if (pos >= 0) {
                return pos;
            }
            int numKnots = x.length;
            float[] nx = new float[numKnots + 1];
            float[] ny = new float[numKnots + 1];
            int j = 0;
            for (int i = 0; i < numKnots; i++) {
                if (pos == -1 && x[i] > kx) {
                    pos = j;
                    nx[j] = kx;
                    ny[j] = ky;
                    j++;
                }
                nx[j] = x[i];
                ny[j] = y[i];
                j++;
            }
            if (pos == -1) {
                pos = j;
                nx[j] = kx;
                ny[j] = ky;
            }
            x = nx;
            y = ny;
            return pos;
        }

        public void removeKnot(int n) {
            int numKnots = x.length;
            if (numKnots <= 2) {
                return;
            }
            float[] nx = new float[numKnots - 1];
            float[] ny = new float[numKnots - 1];
            int j = 0;
            for (int i = 0; i < numKnots - 1; i++) {
                if (i == n) {
                    j++;
                }
                nx[i] = x[j];
                ny[i] = y[j];
                j++;
            }
            x = nx;
            y = ny;
        }

        private void sortKnots() {
            int numKnots = x.length;
            for (int i = 1; i < numKnots - 1; i++) {
                for (int j = 1; j < i; j++) {
                    if (x[i] < x[j]) {
                        float t = x[i];
                        x[i] = x[j];
                        x[j] = t;
                        t = y[i];
                        y[i] = y[j];
                        y[j] = t;
                    }
                }
            }
        }

        private int findKnot(float kx, float ky) {
            for (int i = 0; i < this.x.length; i++) {
                if (kx == x[i] && ky == y[i]) {
                    return i;
                }
            }
            return -1;
        }

        public int[] makeTable() {
            int numKnots = x.length;
            float[] nx = new float[numKnots + 2];
            float[] ny = new float[numKnots + 2];
            System.arraycopy(x, 0, nx, 1, numKnots);
            System.arraycopy(y, 0, ny, 1, numKnots);
            nx[0] = nx[1];
            ny[0] = ny[1];
            nx[numKnots + 1] = nx[numKnots];
            ny[numKnots + 1] = ny[numKnots];

            int[] table = new int[256];
            for (int i = 0; i < 1024; i++) {
                float f = i / 1024.0f;
                int px = (int) (255 * spline(f, nx.length, nx) + 0.5f);
                int py = (int) (255 * spline(f, nx.length, ny) + 0.5f);
                px = clamp(px, 0, 255);
                py = clamp(py, 0, 255);
                table[px] = py;
            }
            return table;
        }

        public void applyToGrid(float[][] grid) {
            int[] table = this.makeTable();
            for (int r = 0; r < grid.length; r++) {
                final float[] row = grid[r];
                for (int c = 0; c < row.length; c++) {
                    final float v = row[c];
                    if (v < 0) {
                        row[c] = table[0];
                    } else if (v > 255) {
                        row[c] = table[255];
                    } else {
                        row[c] = table[(int)v];
                    }
                }
            }
        }

        public void applyToGrid(short[][] grid) {
            int[] table = this.makeTable();
            for (int r = 0; r < grid.length; r++) {
                final short[] row = grid[r];
                for (int c = 0; c < row.length; c++) {
                    final short v = row[c];
                    if (v < 0) {
                        row[c] = (short)table[0];
                    } else if (v > 255) {
                        row[c] = (short)table[255];
                    } else {
                        row[c] = (short)table[(int)v];
                    }
                }
            }
        }

        public void fromString(String str) {
            ArrayList<Float> xp = new ArrayList<Float>();
            ArrayList<Float> yp = new ArrayList<Float>();

            StringTokenizer tokenizer = new StringTokenizer(str, " ");
            while (tokenizer.hasMoreElements()) {
                xp.add(Float.parseFloat(tokenizer.nextToken()));
                yp.add(Float.parseFloat(tokenizer.nextToken()));
            }

            if (xp.size() >= 2) {
                this.x = new float[0];
                this.y = new float[0];
                for (int i = 0; i < xp.size(); i++) {
                    this.addKnot(xp.get(i), yp.get(i));
                }
            }
        }

        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.x.length; i++) {
                sb.append(x[i]);
                sb.append(" ");
                sb.append(y[i]);
                if (i < this.x.length - 1) {
                    sb.append(" ");
                }
            }
            return sb.toString();
        }
    }
    private static final int HANLDE_SIZE = 5;
    private Curve[] curves;
    private Curve curve;
    private int whichCurve = 0;
    private int selected = -1;
    private boolean showFirstCurveOnly = true;
    private int[] histogram = null;
    private int histogramHighlight = -1;
    private String label;

    public GradationGraph() {
        addMouseListener(this);
        addKeyListener(this);
        setCurves(new Curve[]{new Curve(), new Curve(), new Curve()});
    }

    public void applyToGrid(float[][] grid) {
        curves[0].applyToGrid(grid);
    }

    
    public Dimension getMinimumSize() {
        return new Dimension(257 + GRADIENT_BAR_WIDTH, 257 + GRADIENT_BAR_WIDTH);
    }

    
    public Dimension getPreferredSize() {
        return new Dimension(257 + GRADIENT_BAR_WIDTH, 257 + GRADIENT_BAR_WIDTH);
    }

    public void setCurves(Curve[] curves) {
        this.curves = curves;
        if (whichCurve > curves.length) {
            whichCurve = 0;
        }
        curve = curves[whichCurve];
        repaint();
    }

    public void setCurve(Curve curve) {
        this.setCurves(new Curve[]{curve});
    }

    public Curve getCurve(int i) {
        return this.curves[i];
    }

    public void setWhichCurve(int which) {
        whichCurve = which;
        if (whichCurve == -1) {
            whichCurve = 0;
            showFirstCurveOnly = true;
        } else {
            showFirstCurveOnly = false;
        }
        if (curves != null) {
            curve = curves[whichCurve];
            repaint();
        }
    }

    protected void paintLabel(Graphics2D g2d) {
        g2d.setColor(Color.DARK_GRAY);
        Dimension size = this.getSize();
        ika.utils.CenteredStringRenderer.drawCentered(g2d, this.label,
                size.width / 2, (int)(size.height * 0.4),
                ika.utils.CenteredStringRenderer.NOFLIP);
    }

    protected void paintHistogram(Graphics2D g) {
        if (histogram == null) {
            return;
        }
        Color histoColor = isEnabled() ? Color.lightGray : EXTRA_LIGHT_GRAY;
        Color highlightColor = isEnabled() ? Color.BLACK : Color.LIGHT_GRAY;
        g.setColor(histoColor);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (int i = 0; i < histogram.length; i++) {
            if (i == histogramHighlight) {
                g.setColor(highlightColor);
                g.drawLine(i, 255 - Math.max(1, histogram[i]), i, 255);
                g.setColor(histoColor);
            } else {
                g.drawLine(i, 255 - histogram[i], i, 255);
            }
        }
    }
    
    protected void paintGrid(Graphics2D g) {
        g.setColor(isEnabled() ? Color.lightGray : EXTRA_LIGHT_GRAY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawLine(0, 64, 255, 64);
        g.drawLine(0, 128, 255, 128);
        g.drawLine(0, 191, 255, 191);
        g.drawLine(64, 0, 64, 255);
        g.drawLine(128, 0, 128, 255);
        g.drawLine(191, 0, 191, 255);
    }

    protected void paintHandles(Graphics2D g, Curve curve) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (int i = 0; i < curve.x.length; i++) {
            int x = (int) (255 * curve.x[i]) + 1;
            int y = 255 - (int) (255 * curve.y[i]) + 1;
            //g.setColor(color);
            //g.fillRect(x - HANLDE_SIZE / 2, y - HANLDE_SIZE / 2, HANLDE_SIZE, HANLDE_SIZE);
            g.setColor(isEnabled() ? Color.BLACK : Color.LIGHT_GRAY);
            g.fillRect(x - HANLDE_SIZE / 2, y - HANLDE_SIZE / 2, HANLDE_SIZE, HANLDE_SIZE);
        }
    }

    protected void paintCurve(Graphics2D g, Curve curve, Color color) {
        GeneralPath p = new GeneralPath();
        int numKnots = curve.x.length;
        float[] nx = new float[numKnots + 2];
        float[] ny = new float[numKnots + 2];
        System.arraycopy(curve.x, 0, nx, 1, numKnots);
        System.arraycopy(curve.y, 0, ny, 1, numKnots);
        nx[0] = nx[1];
        ny[0] = ny[1];
        nx[numKnots + 1] = nx[numKnots];
        ny[numKnots + 1] = ny[numKnots];
        g.setColor(color);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < 255; i++) {
            float f = i / 255.0f;
            int x = (int) (255 * spline(f, nx.length, nx)) + 1;
            int y = 255 - (int) (255 * spline(f, nx.length, ny)) + 1;
            x = clamp(x, 0, 255);
            y = clamp(y, 0, 255);
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        g.draw(p);


    }

    private void paintGradientBars(Graphics2D g2d) {

        Color darkColor = isEnabled() ? Color.BLACK : Color.GRAY;
        
        // vertical bar
        GradientPaint vGrad = new GradientPaint(0, 1, Color.WHITE, 0, 255, darkColor);
        g2d.setPaint(vGrad);
        g2d.fillRect(0, 1, GRADIENT_BAR_WIDTH, 255);

        // horizontal bar
        int x1 = 1 + GRADIENT_BAR_WIDTH;
        int x2 = 1 + GRADIENT_BAR_WIDTH + 255;
        GradientPaint hGrad = new GradientPaint(x1, 0, darkColor, x2, 0, Color.WHITE);
        g2d.setPaint(hGrad);
        g2d.fillRect(x1, 256, 255, GRADIENT_BAR_WIDTH);

    }

    
    public void paintComponent(Graphics g) {
        if (curves == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create(); //copy g. Recomended by Sun tutorial.
        try {
            paintGradientBars(g2d);

            g2d.translate(GRADIENT_BAR_WIDTH, 0);

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            // paint background
            g.setColor(Color.white);
            g.fillRect(1 + GRADIENT_BAR_WIDTH, 1, 255, 255);

            // draw oblique baseline
            g.setColor(isEnabled() ? Color.lightGray : EXTRA_LIGHT_GRAY);
            g.drawLine(1 + GRADIENT_BAR_WIDTH, 255, 255 + GRADIENT_BAR_WIDTH, 1);

            paintGrid(g2d);

            // draw frame
            g.setColor(isEnabled() ? Color.black : Color.LIGHT_GRAY);
            g.drawRect(GRADIENT_BAR_WIDTH, 0, 255, 255);

            paintHistogram(g2d);

            paintHandles(g2d, curves[whichCurve]);
            Color grayCurveColor = isEnabled() ? Color.BLACK : EXTRA_LIGHT_GRAY;
            paintCurve(g2d, curves[0], showFirstCurveOnly ? grayCurveColor : Color.red);
            if (!showFirstCurveOnly && curves.length == 3) {
                paintCurve(g2d, curves[1], Color.green);
                paintCurve(g2d, curves[2], Color.blue);
            }

            paintLabel(g2d);

        } finally {
            g2d.dispose(); //release the copy's resources. Recomended by Sun tutorial.
        }
    }

    
    public void mousePressed(MouseEvent e) {
        if (!isEnabled()) {
            return;
        }
        requestFocus();
        int x = e.getX() - 1 - GRADIENT_BAR_WIDTH;
        int y = e.getY() - 1;
        int newSelected = -1;
        for (int i = 0; i < curve.x.length; i++) {
            int kx = (int) (255 * curve.x[i]);
            int ky = 255 - (int) (255 * curve.y[i]) + 1;
            if (Math.abs(x - kx) < 5 && Math.abs(y - ky) < 5) {
                newSelected = i;
                addMouseMotionListener(this);
                break;
            }
        }
        if (newSelected != selected) {
            selected = newSelected;
            repaint();
        }
        if (newSelected == -1) {
            selected = curve.addKnot(x / 255.0f, 1 - y / 255.0f);
            addMouseMotionListener(this);
            firePropertyChange("GradationGraph selection changed", null, null);
            repaint();
        }
    }

    
    public void mouseReleased(MouseEvent e) {
        if (!isEnabled()) {
            return;
        }
        addMouseMotionListener(this);
        if (selected != -1) {
            int x = e.getX() - 1 - GRADIENT_BAR_WIDTH;
            int y = e.getY() - 1;
            if (selected != 0 && selected != curve.x.length - 1 && (x < 0 || x >= getWidth() || y < 0 || y > getHeight())) {
                curve.removeKnot(selected);
                repaint();
            }
            firePropertyChange("GradationGraph curve changed", null, null);
            selected = -1;
        }
    }

    
    public void mouseEntered(MouseEvent e) {
    }

    
    public void mouseExited(MouseEvent e) {
    }

    
    public void mouseClicked(MouseEvent e) {
    }

    
    public void mouseMoved(MouseEvent e) {
    }

    
    public void mouseDragged(MouseEvent e) {
        if (!isEnabled()) {
            return;
        }
        if (selected != -1) {
            int x = e.getX() - 1 - GRADIENT_BAR_WIDTH;
            int y = e.getY() - 1;
            x = clamp(x, 0, 255);
            y = clamp(y, 0, 255);
            float fx = x / 255.0f;
            float fy = 1 - y / 255.0f;
            if (selected > 0) {
                if (fx < curve.x[selected - 1]) {
                    fx = curve.x[selected - 1];
                }
            } else {
                fx = 0;
            }
            if (selected < curve.x.length - 1) {
                if (fx > curve.x[selected + 1]) {
                    fx = curve.x[selected + 1];
                }
            } else {
                fx = 1;
            }
            curve.x[selected] = fx;
            curve.y[selected] = fy;
            repaint();
        }
    }

    
    public void keyPressed(KeyEvent e) {
        if (!isEnabled()) {
            return;
        }
        switch (e.getKeyChar()) {
            case '1':
            case '2':
            case '3':
                setWhichCurve(e.getKeyChar() - '1');
                break;
        }
    }

    
    public void keyReleased(KeyEvent e) {
    }

    
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Clamp a value to an interval.
     * @param a the lower clamp threshold
     * @param b the upper clamp threshold
     * @param x the input parameter
     * @return the clamped value
     */
    public static int clamp(int x, int a, int b) {
        return (x < a) ? a : (x > b) ? b : x;
    }

    /**
     * Clamp a value to an interval.
     * @param a the lower clamp threshold
     * @param b the upper clamp threshold
     * @param x the input parameter
     * @return the clamped value
     */
    public static float clamp(float x, float a, float b) {
        return (x < a) ? a : (x > b) ? b : x;
    }

    // Catmull-Rom splines
    private final static float m00 = -0.5f;
    private final static float m01 = 1.5f;
    private final static float m02 = -1.5f;
    private final static float m03 = 0.5f;
    private final static float m10 = 1.0f;
    private final static float m11 = -2.5f;
    private final static float m12 = 2.0f;
    private final static float m13 = -0.5f;
    private final static float m20 = -0.5f;
    private final static float m21 = 0.0f;
    private final static float m22 = 0.5f;
    private final static float m23 = 0.0f;
    private final static float m30 = 0.0f;
    private final static float m31 = 1.0f;
    private final static float m32 = 0.0f;
    private final static float m33 = 0.0f;

    /**
     * Compute a Catmull-Rom spline.
     * @param x the input parameter
     * @param numKnots the number of knots in the spline
     * @param knots the array of knots
     * @return the spline value
     */
    private static float spline(float x, int numKnots, float[] knots) {
        int span;
        int numSpans = numKnots - 3;
        float k0, k1, k2, k3;
        float c0, c1, c2, c3;

        if (numSpans < 1) {
            throw new IllegalArgumentException("Too few knots in spline");
        }

        x = clamp(x, 0, 1) * numSpans;
        span = (int) x;
        if (span > numKnots - 4) {
            span = numKnots - 4;
        }
        x -= span;

        k0 = knots[span];
        k1 = knots[span + 1];
        k2 = knots[span + 2];
        k3 = knots[span + 3];

        c3 = m00 * k0 + m01 * k1 + m02 * k2 + m03 * k3;
        c2 = m10 * k0 + m11 * k1 + m12 * k2 + m13 * k3;
        c1 = m20 * k0 + m21 * k1 + m22 * k2 + m23 * k3;
        c0 = m30 * k0 + m31 * k1 + m32 * k2 + m33 * k3;

        return ((c3 * x + c2) * x + c1) * x + c0;
    }

    void setHistogram(int[] histogram) {
        this.histogram = histogram;
        this.repaint();
    }

    /**
     * @param histogramHighlight the histogramHighlight to set
     */
    public void setHistogramHighlight(int histogramHighlight) {
        this.histogramHighlight = histogramHighlight;
        this.repaint();
    }
}