package digital.pragmatech.springtestinsight.pdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for PDF report generation.
 * Uses pragmatech.* namespace for system properties.
 */
public class PdfConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfConfiguration.class);
    
    // Property names using pragmatech namespace
    private static final String PDF_ENABLED_PROPERTY = "pragmatech.spring.test.insight.pdf.enabled";
    private static final String PDF_PAGE_SIZE_PROPERTY = "pragmatech.spring.test.insight.pdf.pageSize";
    private static final String PDF_MARGINS_PROPERTY = "pragmatech.spring.test.insight.pdf.margins";
    private static final String PDF_FONT_FAMILY_PROPERTY = "pragmatech.spring.test.insight.pdf.fontFamily";
    private static final String PDF_ORIENTATION_PROPERTY = "pragmatech.spring.test.insight.pdf.orientation";
    
    // Default values
    private static final boolean DEFAULT_PDF_ENABLED = false;
    private static final PageSize DEFAULT_PAGE_SIZE = PageSize.A4;
    private static final String DEFAULT_MARGINS = "20mm";
    private static final String DEFAULT_FONT_FAMILY = "Arial, sans-serif";
    private static final PageOrientation DEFAULT_ORIENTATION = PageOrientation.PORTRAIT;
    
    public enum PageSize {
        A4(595, 842),
        LETTER(612, 792),
        LEGAL(612, 1008),
        A3(842, 1191);
        
        private final int width;
        private final int height;
        
        PageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
    }
    
    public enum PageOrientation {
        PORTRAIT,
        LANDSCAPE
    }
    
    private final boolean pdfEnabled;
    private final PageSize pageSize;
    private final String margins;
    private final String fontFamily;
    private final PageOrientation orientation;
    
    public PdfConfiguration() {
        this.pdfEnabled = getBooleanProperty(PDF_ENABLED_PROPERTY, DEFAULT_PDF_ENABLED);
        this.pageSize = getPageSizeProperty(PDF_PAGE_SIZE_PROPERTY, DEFAULT_PAGE_SIZE);
        this.margins = getStringProperty(PDF_MARGINS_PROPERTY, DEFAULT_MARGINS);
        this.fontFamily = getStringProperty(PDF_FONT_FAMILY_PROPERTY, DEFAULT_FONT_FAMILY);
        this.orientation = getOrientationProperty(PDF_ORIENTATION_PROPERTY, DEFAULT_ORIENTATION);
        
        if (logger.isDebugEnabled()) {
            logger.debug("PDF Configuration: enabled={}, pageSize={}, margins={}, fontFamily={}, orientation={}", 
                pdfEnabled, pageSize, margins, fontFamily, orientation);
        }
    }
    
    public boolean isPdfEnabled() {
        return pdfEnabled;
    }
    
    public PageSize getPageSize() {
        return pageSize;
    }
    
    public String getMargins() {
        return margins;
    }
    
    public String getFontFamily() {
        return fontFamily;
    }
    
    public PageOrientation getOrientation() {
        return orientation;
    }
    
    /**
     * Gets the actual page width considering orientation.
     */
    public int getEffectiveWidth() {
        return orientation == PageOrientation.LANDSCAPE ? pageSize.getHeight() : pageSize.getWidth();
    }
    
    /**
     * Gets the actual page height considering orientation.
     */
    public int getEffectiveHeight() {
        return orientation == PageOrientation.LANDSCAPE ? pageSize.getWidth() : pageSize.getHeight();
    }
    
    /**
     * Converts margin string (e.g., "20mm", "1in") to points.
     * For simplicity, assumes mm and converts to points (1mm = 2.83 points).
     */
    public float getMarginsInPoints() {
        try {
            String marginStr = margins.toLowerCase().trim();
            if (marginStr.endsWith("mm")) {
                float mm = Float.parseFloat(marginStr.substring(0, marginStr.length() - 2));
                return mm * 2.83f; // Convert mm to points
            } else if (marginStr.endsWith("in")) {
                float inches = Float.parseFloat(marginStr.substring(0, marginStr.length() - 2));
                return inches * 72f; // Convert inches to points
            } else if (marginStr.endsWith("pt")) {
                return Float.parseFloat(marginStr.substring(0, marginStr.length() - 2));
            } else {
                // Assume points if no unit specified
                return Float.parseFloat(marginStr);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid margin format '{}', using default 20mm", margins);
            return 56.7f; // 20mm in points
        }
    }
    
    private boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        String value = System.getProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    private String getStringProperty(String propertyName, String defaultValue) {
        return System.getProperty(propertyName, defaultValue);
    }
    
    private PageSize getPageSizeProperty(String propertyName, PageSize defaultValue) {
        String value = System.getProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return PageSize.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid page size '{}', using default {}", value, defaultValue);
            return defaultValue;
        }
    }
    
    private PageOrientation getOrientationProperty(String propertyName, PageOrientation defaultValue) {
        String value = System.getProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return PageOrientation.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid orientation '{}', using default {}", value, defaultValue);
            return defaultValue;
        }
    }
    
    @Override
    public String toString() {
        return String.format("PdfConfiguration{enabled=%s, pageSize=%s, margins='%s', fontFamily='%s', orientation=%s}", 
            pdfEnabled, pageSize, margins, fontFamily, orientation);
    }
}