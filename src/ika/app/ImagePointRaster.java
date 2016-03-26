package ika.app;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

class ImagePointRaster implements PointRaster {

    private final int cols;
    private final int rows;
    private final double west;
    private final double north;
    private final double cellSize;
    private final WritableRaster raster;

    public ImagePointRaster(Rectangle2D boundingBox, double cellSize) {
        super();
        this.cols = (int) Math.ceil(boundingBox.getWidth() / cellSize);
        this.rows = (int) Math.ceil(boundingBox.getHeight() / cellSize);
        this.west = boundingBox.getMinX();
        this.north = boundingBox.getMaxY();
        this.cellSize = cellSize;

        BufferedImage bi;
        bi = new BufferedImage(cols, rows, BufferedImage.TYPE_BYTE_BINARY);
        this.raster = bi.getRaster();
    }

    @Override
    public void addCircle(double x, double y, double rad) {
        final int cc = (int) ((x - west) / cellSize);
        final int rc = (int) ((north - y) / cellSize);
        final int radi = (int) Math.ceil(rad / cellSize);
        final int radsq = radi * radi;
        for (int r = -radi; r <= radi; r++) {
            final int row = r + rc;
            if (row >= 0 && row < rows) {
                for (int c = -radi; c <= radi; c++) {
                    final int col = c + cc;
                    if ((c * c + r * r) < radsq && col >= 0 && col < cols) {
                        raster.setSample(col, row, 0, 1);
                    }
                }
            }
        }
    }

    @Override
    public boolean isCircleOverlaying(double x, double y, double rad) {
        final int cc = (int) ((x - west) / cellSize);
        final int rc = (int) ((north - y) / cellSize);
        final int radi = (int) Math.ceil(rad / cellSize);
        final int radsq = (int) Math.round(radi * radi);
        for (int r = -radi; r <= radi; r++) {
            final int row = r + rc;
            if (row >= 0 && row < rows) {
                for (int c = -radi; c <= radi; c++) {
                    final int col = c + cc;
                    if ((c * c + r * r) < radsq && col >= 0 && col < cols) {
                        if (this.raster.getSample(col, row, 0) == 1) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public BufferedImage toImage() {
        BufferedImage image = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
        int red = Color.RED.getRGB();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (this.raster.getSample(c, r, 0) == 1) {
                    image.setRGB(c, r, red);
                }
            }
        }
        return image;
    }
}
