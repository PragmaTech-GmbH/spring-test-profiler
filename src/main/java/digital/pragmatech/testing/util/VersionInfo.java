package digital.pragmatech.testing.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionInfo {

  private static final Logger LOG = LoggerFactory.getLogger(VersionInfo.class);
  private static final String version;

  static {
    version = loadVersion();
  }

  private static String loadVersion() {
    String manifestVersion = loadVersionFromManifest();
    if (manifestVersion != null && !manifestVersion.isEmpty()) {
      LOG.debug("Loaded version from MANIFEST.MF: {}", manifestVersion);
      return manifestVersion;
    }

    String propertiesVersion = loadVersionFromProperties();
    if (propertiesVersion != null && !propertiesVersion.isEmpty()) {
      LOG.debug("Loaded version from properties file: {}", propertiesVersion);
      return propertiesVersion;
    }

    LOG.warn("Failed to load version information from MANIFEST.MF or properties file");
    return "unknown";
  }

  private static String loadVersionFromManifest() {
    try {
      Package pkg = VersionInfo.class.getPackage();
      if (pkg != null) {
        String implementationVersion = pkg.getImplementationVersion();
        if (implementationVersion != null && !implementationVersion.isEmpty()) {
          return implementationVersion;
        }
      }
    } catch (Exception e) {
      LOG.debug("Failed to load version from MANIFEST.MF", e);
    }
    return null;
  }

  private static String loadVersionFromProperties() {
    Properties props = new Properties();
    try (InputStream is =
        VersionInfo.class.getResourceAsStream("/spring-test-profiler-version.properties")) {
      if (is != null) {
        props.load(is);
        return props.getProperty("version");
      }
    } catch (IOException e) {
      LOG.debug("Failed to load version from properties file", e);
    }
    return null;
  }

  public static String getVersion() {
    return version;
  }
}
