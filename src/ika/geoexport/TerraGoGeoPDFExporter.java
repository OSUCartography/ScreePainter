package ika.geoexport;

import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.PdfWriter;
import ika.gui.PageFormat;
import java.io.IOException;

/**
 *
 * @author Bernhard Jenny
 */
public class TerraGoGeoPDFExporter extends PDFExporter {

    @Override
    public String getFileFormatName() {
        return "TerraGo OGC GeoPDF";
    }   

    @Override
    protected void configurePDFWriter(PdfWriter writer) throws IOException {
        double leftMap = pageFormat.getPageLeft();
        double bottomMap = pageFormat.getPageBottom();
        
        PdfDictionary lgiDict = new PdfDictionary(new PdfName("LGIDict"));
        lgiDict.put(new PdfName("Version"), new PdfNumber("2.1"));
        
        /*
        // Registration (optional): not interpreted by GDAL
        
        double rightMap = pageFormat.getPageRight();
        double topMap = pageFormat.getPageTop();
        
        float leftPage = xToPagePx((float) leftMap);
        float bottomPage = yToPagePx((float) bottomMap);
        float rightPage = xToPagePx((float) rightMap);
        float topPage = yToPagePx((float) topMap);
        
        PdfArray lowerLeftPoint = new PdfArray();
        lowerLeftPoint.add(new PdfString(Double.toString(leftPage)));
        lowerLeftPoint.add(new PdfString(Double.toString(bottomPage)));
        lowerLeftPoint.add(new PdfString(Double.toString(leftMap)));
        lowerLeftPoint.add(new PdfString(Double.toString(bottomMap)));

        PdfArray upperRightPoint = new PdfArray();
        upperRightPoint.add(new PdfString(Double.toString(rightPage)));
        upperRightPoint.add(new PdfString(Double.toString(topPage)));
        upperRightPoint.add(new PdfString(Double.toString(rightMap)));
        upperRightPoint.add(new PdfString(Double.toString(topMap)));

        PdfArray registration = new PdfArray();
        registration.add(lowerLeftPoint);
        registration.add(upperRightPoint);

        lgiDict.put(new PdfName("Registration"), registration);
        */
        
        // FIXME usage of PageFormat.MM2PX
        double scale = pageFormat.getPageWidthWorldCoordinates() / pageFormat.getPageWidth() / PageFormat.MM2PX;
        PdfArray ctmArray = new PdfArray();
        ctmArray.add(new PdfString(Double.toString(scale)));
        ctmArray.add(new PdfString("0"));
        ctmArray.add(new PdfString("0"));
        ctmArray.add(new PdfString(Double.toString(scale)));
        ctmArray.add(new PdfString(Double.toString(leftMap)));
        ctmArray.add(new PdfString(Double.toString(bottomMap)));
        lgiDict.put(new PdfName("CTM"), ctmArray);
        
        // Projection
        PdfDictionary projectionDict = new PdfDictionary(new PdfName("Projection"));
        projectionDict.put(new PdfName("ProjectionType"), new PdfString("NONE"));
        lgiDict.put(new PdfName("Projection"), projectionDict);
        
        /*
        // Neatline (optional)
        PdfArray neatlinePoints = new PdfArray();
        neatlinePoints.add(new PdfString(Double.toString(leftPage)));
        neatlinePoints.add(new PdfString(Double.toString(bottomPage)));
        neatlinePoints.add(new PdfString(Double.toString(rightPage)));
        neatlinePoints.add(new PdfString(Double.toString(topPage)));
        lgiDict.put(new PdfName("Neatline"), neatlinePoints);
        */
        
        writer.addPageDictEntry(new PdfName("LGIDict"), lgiDict);
    }
}