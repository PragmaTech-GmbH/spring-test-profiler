package digital.pragmatech.testing.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionInfo {

  private static final Logger LOG = LoggerFactory.getLogger(VersionInfo.class);
  private static final Properties props = new Properties();

  static {
    try (InputStream is = VersionInfo.class.getResourceAsStream("/version.properties")) {
      props.load(is);
    }
    catch (IOException e) {
      LOG.warn("Failed to load version.properties file", e);
      props.setProperty("version", "unknown");
    }
  }

  public static String getVersion() {
    return props.getProperty("version");
  }
}
