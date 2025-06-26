package digital.pragmatech.springtestinsight.pdf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class PdfConfigurationTest {
    
    private String originalPdfEnabled;
    private String originalPageSize;
    private String originalMargins;
    private String originalFontFamily;
    private String originalOrientation;
    
    @BeforeEach
    void saveOriginalProperties() {
        // Save original values to restore after tests
        originalPdfEnabled = System.getProperty("pragmatech.spring.test.insight.pdf.enabled");
        originalPageSize = System.getProperty("pragmatech.spring.test.insight.pdf.pageSize");
        originalMargins = System.getProperty("pragmatech.spring.test.insight.pdf.margins");
        originalFontFamily = System.getProperty("pragmatech.spring.test.insight.pdf.fontFamily");
        originalOrientation = System.getProperty("pragmatech.spring.test.insight.pdf.orientation");
    }
    
    @AfterEach
    void restoreOriginalProperties() {
        // Restore original values
        setPropertyOrClear("pragmatech.spring.test.insight.pdf.enabled", originalPdfEnabled);
        setPropertyOrClear("pragmatech.spring.test.insight.pdf.pageSize", originalPageSize);
        setPropertyOrClear("pragmatech.spring.test.insight.pdf.margins", originalMargins);
        setPropertyOrClear("pragmatech.spring.test.insight.pdf.fontFamily", originalFontFamily);
        setPropertyOrClear("pragmatech.spring.test.insight.pdf.orientation", originalOrientation);
    }
    
    private void setPropertyOrClear(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    void shouldUseDefaultValuesWhenNoPropertiesSet() {
        // Given - no system properties set
        clearAllProperties();
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertFalse(config.isPdfEnabled());
        assertEquals(PdfConfiguration.PageSize.A4, config.getPageSize());
        assertEquals("20mm", config.getMargins());
        assertEquals("Arial, sans-serif", config.getFontFamily());
        assertEquals(PdfConfiguration.PageOrientation.PORTRAIT, config.getOrientation());
    }
    
    @Test
    void shouldReadPdfEnabledProperty() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.enabled", "true");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertTrue(config.isPdfEnabled());
    }
    
    @Test
    void shouldReadPageSizeProperty() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.pageSize", "LETTER");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertEquals(PdfConfiguration.PageSize.LETTER, config.getPageSize());
    }
    
    @Test
    void shouldHandleInvalidPageSizeProperty() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.pageSize", "INVALID");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertEquals(PdfConfiguration.PageSize.A4, config.getPageSize()); // Should fall back to default
    }
    
    @Test
    void shouldReadOrientationProperty() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.orientation", "LANDSCAPE");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertEquals(PdfConfiguration.PageOrientation.LANDSCAPE, config.getOrientation());
    }
    
    @Test
    void shouldHandleInvalidOrientationProperty() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.orientation", "INVALID");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertEquals(PdfConfiguration.PageOrientation.PORTRAIT, config.getOrientation()); // Should fall back to default
    }
    
    @Test
    void shouldReadMarginsProperty() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.margins", "1in");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertEquals("1in", config.getMargins());
    }
    
    @Test
    void shouldReadFontFamilyProperty() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.fontFamily", "Times New Roman, serif");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertEquals("Times New Roman, serif", config.getFontFamily());
    }
    
    @Test
    void shouldCalculateEffectiveDimensionsForPortrait() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.pageSize", "A4");
        System.setProperty("pragmatech.spring.test.insight.pdf.orientation", "PORTRAIT");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertEquals(595, config.getEffectiveWidth());  // A4 width in portrait
        assertEquals(842, config.getEffectiveHeight()); // A4 height in portrait
    }
    
    @Test
    void shouldCalculateEffectiveDimensionsForLandscape() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.pageSize", "A4");
        System.setProperty("pragmatech.spring.test.insight.pdf.orientation", "LANDSCAPE");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        
        // Then
        assertEquals(842, config.getEffectiveWidth());  // A4 height becomes width in landscape
        assertEquals(595, config.getEffectiveHeight()); // A4 width becomes height in landscape
    }
    
    @Test
    void shouldConvertMillimetersToPoints() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.margins", "10mm");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        float marginsInPoints = config.getMarginsInPoints();
        
        // Then
        assertEquals(28.3f, marginsInPoints, 0.1f); // 10mm * 2.83 ≈ 28.3 points
    }
    
    @Test
    void shouldConvertInchesToPoints() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.margins", "1in");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        float marginsInPoints = config.getMarginsInPoints();
        
        // Then
        assertEquals(72f, marginsInPoints, 0.1f); // 1 inch = 72 points
    }
    
    @Test
    void shouldHandlePointsDirectly() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.margins", "36pt");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        float marginsInPoints = config.getMarginsInPoints();
        
        // Then
        assertEquals(36f, marginsInPoints, 0.1f);
    }
    
    @Test
    void shouldHandleNoUnitAsPoints() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.margins", "24");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        float marginsInPoints = config.getMarginsInPoints();
        
        // Then
        assertEquals(24f, marginsInPoints, 0.1f);
    }
    
    @Test
    void shouldHandleInvalidMarginFormat() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.margins", "invalid");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        float marginsInPoints = config.getMarginsInPoints();
        
        // Then
        assertEquals(56.7f, marginsInPoints, 0.1f); // Should fall back to 20mm default
    }
    
    @Test
    void shouldIncludeAllPropertiesInToString() {
        // Given
        System.setProperty("pragmatech.spring.test.insight.pdf.enabled", "true");
        System.setProperty("pragmatech.spring.test.insight.pdf.pageSize", "LETTER");
        System.setProperty("pragmatech.spring.test.insight.pdf.orientation", "LANDSCAPE");
        
        // When
        PdfConfiguration config = new PdfConfiguration();
        String toString = config.toString();
        
        // Then
        assertTrue(toString.contains("enabled=true"));
        assertTrue(toString.contains("pageSize=LETTER"));
        assertTrue(toString.contains("orientation=LANDSCAPE"));
        assertTrue(toString.contains("margins="));
        assertTrue(toString.contains("fontFamily="));
    }
    
    private void clearAllProperties() {
        System.clearProperty("pragmatech.spring.test.insight.pdf.enabled");
        System.clearProperty("pragmatech.spring.test.insight.pdf.pageSize");
        System.clearProperty("pragmatech.spring.test.insight.pdf.margins");
        System.clearProperty("pragmatech.spring.test.insight.pdf.fontFamily");
        System.clearProperty("pragmatech.spring.test.insight.pdf.orientation");
    }
}