package digital.pragmatech.springtestinsight.pdf;

import com.lowagie.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service class for generating PDF reports from HTML content.
 * Uses Flying Saucer and OpenPDF for HTML-to-PDF conversion.
 */
public class PdfReportGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfReportGenerator.class);
    
    private final PdfConfiguration configuration;
    
    public PdfReportGenerator() {
        this.configuration = new PdfConfiguration();
    }
    
    public PdfReportGenerator(PdfConfiguration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Generates a PDF report from HTML content and saves it to the specified path.
     *
     * @param htmlContent The HTML content to convert to PDF
     * @param outputPath The path where the PDF should be saved
     * @throws PdfGenerationException If PDF generation fails
     */
    public void generatePdfReport(String htmlContent, Path outputPath) throws PdfGenerationException {
        if (!configuration.isPdfEnabled()) {
            logger.debug("PDF generation is disabled, skipping PDF report generation");
            return;
        }
        
        try {
            logger.debug("Generating PDF report at: {}", outputPath);
            
            // Ensure parent directory exists
            Files.createDirectories(outputPath.getParent());
            
            // Preprocess HTML for PDF rendering
            String processedHtml = preprocessHtmlForPdf(htmlContent);
            
            // Generate PDF using Flying Saucer
            byte[] pdfBytes = convertHtmlToPdf(processedHtml);
            
            // Write PDF to file
            Files.write(outputPath, pdfBytes);
            
            logger.info("PDF report generated successfully: {}", outputPath.toAbsolutePath());
            
        } catch (IOException e) {
            throw new PdfGenerationException("Failed to write PDF file: " + outputPath, e);
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to generate PDF report", e);
        }
    }
    
    /**
     * Converts HTML content to PDF bytes using Flying Saucer and OpenPDF.
     *
     * @param htmlContent The HTML content to convert
     * @return PDF content as byte array
     * @throws DocumentException If PDF generation fails
     * @throws IOException If I/O operation fails
     */
    private byte[] convertHtmlToPdf(String htmlContent) throws DocumentException, IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            
            // Configure page size and orientation
            configureRenderer(renderer);
            
            // Set HTML content
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            
            // Generate PDF
            renderer.createPDF(outputStream);
            
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Configures the ITextRenderer with page size, orientation, and other settings.
     */
    private void configureRenderer(ITextRenderer renderer) {
        // Note: Flying Saucer with OpenPDF uses different configuration approach
        // Page size and margins are typically controlled via CSS @page rules
        // We'll inject CSS for page configuration
        logger.debug("Configuring PDF renderer with: {}", configuration);
    }
    
    /**
     * Preprocesses HTML content for PDF rendering.
     * This includes adding PDF-specific CSS and adjusting content for print layout.
     *
     * @param htmlContent Original HTML content
     * @return Processed HTML content optimized for PDF
     */
    private String preprocessHtmlForPdf(String htmlContent) {
        // Add PDF-specific CSS
        String pdfCss = generatePdfCss();
        
        // Insert PDF CSS before closing </head> tag
        String processedHtml = htmlContent.replace("</head>", 
            "<style type=\"text/css\">\n" + pdfCss + "\n</style>\n</head>");
        
        // Ensure proper DOCTYPE for PDF rendering
        if (!processedHtml.startsWith("<!DOCTYPE")) {
            processedHtml = "<!DOCTYPE html>\n" + processedHtml;
        }
        
        return processedHtml;
    }
    
    /**
     * Generates PDF-specific CSS based on configuration.
     * This includes @page rules for margins, size, and orientation.
     */
    private String generatePdfCss() {
        StringBuilder css = new StringBuilder();
        
        // @page rule for page configuration
        css.append("@page {\n");
        css.append("  size: ").append(getPageSizeCss()).append(";\n");
        css.append("  margin: ").append(configuration.getMarginsInPoints()).append("pt;\n");
        css.append("}\n\n");
        
        // Font configuration
        css.append("body {\n");
        css.append("  font-family: ").append(configuration.getFontFamily()).append(";\n");
        css.append("  font-size: 12pt;\n");
        css.append("  line-height: 1.4;\n");
        css.append("}\n\n");
        
        // Print-specific adjustments
        css.append("/* Print-specific styles */\n");
        css.append(".no-print { display: none !important; }\n");
        css.append("a { color: #000 !important; text-decoration: none !important; }\n");
        css.append("table { border-collapse: collapse; width: 100%; }\n");
        css.append("th, td { border: 1px solid #ddd; padding: 8px; }\n");
        css.append("h1, h2, h3 { page-break-after: avoid; }\n");
        css.append("tr { page-break-inside: avoid; }\n");
        
        return css.toString();
    }
    
    /**
     * Converts configuration page size and orientation to CSS format.
     */
    private String getPageSizeCss() {
        String size = configuration.getPageSize().name().toLowerCase();
        
        if (configuration.getOrientation() == PdfConfiguration.PageOrientation.LANDSCAPE) {
            size += " landscape";
        }
        
        return size;
    }
    
    /**
     * Exception thrown when PDF generation fails.
     */
    public static class PdfGenerationException extends Exception {
        public PdfGenerationException(String message) {
            super(message);
        }
        
        public PdfGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Returns the PDF configuration being used.
     */
    public PdfConfiguration getConfiguration() {
        return configuration;
    }
}