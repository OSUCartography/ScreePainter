package ika.geoexport;

import com.itextpdf.text.pdf.ByteBuffer;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfIndirectObject;
import com.itextpdf.text.pdf.PdfIndirectReference;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfRectangle;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.IOException;

/**
 *
 * @author Bernhard Jenny
 */
public class GeospatialPDFExporter extends PDFExporter {

    // Well Known Text format for coordinate reference system
    private String wkt = "";

    // geographic coordinates of corner points as lat/lon pairs
    private float[] lonLatCornerPoints = null;

    @Override
    public String getFileFormatName() {
        return "Geospatial PDF (Swiss CH1903+ / LV95)";
    }

    @Override
    protected void configurePDFWriter(PdfWriter writer) throws IOException {

        final boolean initialPrecisionFlat = ByteBuffer.HIGH_PRECISION;
        try {
            ByteBuffer.HIGH_PRECISION = true;

            PdfDictionary dicMeasure = new PdfDictionary(new PdfName("Measure"));
            dicMeasure.put(PdfName.SUBTYPE, new PdfName("GEO"));

            PdfArray bounds = new PdfArray();
            // lower left, upper left, upper right, lower right
            bounds.add(new float[]{0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f});

            // geographic coordinates of corner points as lat/lon pairs
            PdfArray gpts = new PdfArray(lonLatCornerPoints);
            // lower left, upper left, upper right, lower right
            PdfArray lpts = new PdfArray(new float[]{0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f});

            dicMeasure.put(new PdfName("Bounds"), bounds);
            dicMeasure.put(new PdfName("LPTS"), lpts);
            dicMeasure.put(new PdfName("GPTS"), gpts);

            PdfDictionary dicGCS = new PdfDictionary(new PdfName("PROJCS"));
            dicGCS.put(new PdfName("WKT"), new PdfString(wkt));

            PdfIndirectObject indObjGCS = writer.addToBody(dicGCS);
            PdfIndirectReference indRefGCS = indObjGCS.getIndirectReference();
            dicMeasure.put(new PdfName("GCS"), indRefGCS);

            PdfDictionary viewport = new PdfDictionary(new PdfName("Viewport"));

            viewport.put(new PdfName("Name"), new PdfString("Scree"));

            float left = (float) xToPagePx(pageFormat.getPageLeft());
            float lower = (float) yToPagePx(pageFormat.getPageBottom());
            float right = (float) xToPagePx(pageFormat.getPageRight());
            float upper = (float) yToPagePx(pageFormat.getPageTop());
            viewport.put(new PdfName("BBox"), new PdfRectangle(left, lower, right, upper));

            PdfIndirectObject indObjMeasure = writer.addToBody(dicMeasure);
            PdfIndirectReference indRefMeasure = indObjMeasure.getIndirectReference();
            viewport.put(new PdfName("Measure"), indRefMeasure);

            writer.setPageViewport(new PdfArray(viewport));

        } finally {
            ByteBuffer.HIGH_PRECISION = initialPrecisionFlat;
        }
    }

    /**
     * @return the wkt
     */
    public String getWKT() {
        return wkt;
    }

    /**
     * @param wkt the wkt to set
     */
    public void setWKT(String wkt) {
        this.wkt = wkt;
    }

    /**
     * @return the lonLatCornerPoints
     */
    public float[] getLonLatCornerPoints() {
        return lonLatCornerPoints;
    }

    /**
     * @param lonLatCornerPoints the lonLatCornerPoints to set
     */
    public void setLonLatCornerPoints(float[] lonLatCornerPoints) {
        this.lonLatCornerPoints = lonLatCornerPoints;
    }
}
