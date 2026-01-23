package digital.pragmatech.testing.util;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Lightweight JSON serialization utility with zero external dependencies. Supports serialization of
 * common Java types including records, collections, maps, and primitives.
 */
public final class SimpleJsonWriter {

  private SimpleJsonWriter() {}

  /**
   * Serializes an object to a compact JSON string.
   *
   * @param obj the object to serialize
   * @return JSON string representation
   */
  public static String toJson(Object obj) {
    StringBuilder sb = new StringBuilder();
    writeValue(obj, sb, 0, false);
    return sb.toString();
  }

  /**
   * Serializes an object to a pretty-printed JSON string with indentation.
   *
   * @param obj the object to serialize
   * @return pretty-printed JSON string
   */
  public static String toJsonPretty(Object obj) {
    StringBuilder sb = new StringBuilder();
    writeValue(obj, sb, 0, true);
    return sb.toString();
  }

  private static void writeValue(Object obj, StringBuilder sb, int indent, boolean pretty) {
    if (obj == null) {
      sb.append("null");
    } else if (obj instanceof String s) {
      writeString(s, sb);
    } else if (obj instanceof Number) {
      sb.append(obj);
    } else if (obj instanceof Boolean) {
      sb.append(obj);
    } else if (obj instanceof Instant instant) {
      sb.append(instant.getEpochSecond());
    } else if (obj instanceof Collection<?> collection) {
      writeCollection(collection, sb, indent, pretty);
    } else if (obj instanceof Map<?, ?> map) {
      writeMap(map, sb, indent, pretty);
    } else if (obj.getClass().isRecord()) {
      writeRecord(obj, sb, indent, pretty);
    } else if (obj.getClass().isArray()) {
      writeArray(obj, sb, indent, pretty);
    } else {
      writeString(obj.toString(), sb);
    }
  }

  private static void writeString(String s, StringBuilder sb) {
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    sb.append('"');
  }

  private static void writeCollection(
      Collection<?> collection, StringBuilder sb, int indent, boolean pretty) {
    sb.append('[');
    if (!collection.isEmpty()) {
      if (pretty) {
        sb.append('\n');
      }
      Iterator<?> it = collection.iterator();
      while (it.hasNext()) {
        if (pretty) {
          appendIndent(sb, indent + 1);
        }
        writeValue(it.next(), sb, indent + 1, pretty);
        if (it.hasNext()) {
          sb.append(',');
        }
        if (pretty) {
          sb.append('\n');
        }
      }
      if (pretty) {
        appendIndent(sb, indent);
      }
    }
    sb.append(']');
  }

  @SuppressWarnings("unchecked")
  private static void writeMap(Map<?, ?> map, StringBuilder sb, int indent, boolean pretty) {
    sb.append('{');
    if (!map.isEmpty()) {
      if (pretty) {
        sb.append('\n');
      }
      Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<?, ?> entry = it.next();
        if (pretty) {
          appendIndent(sb, indent + 1);
        }
        writeString(String.valueOf(entry.getKey()), sb);
        sb.append(':');
        if (pretty) {
          sb.append(' ');
        }
        writeValue(entry.getValue(), sb, indent + 1, pretty);
        if (it.hasNext()) {
          sb.append(',');
        }
        if (pretty) {
          sb.append('\n');
        }
      }
      if (pretty) {
        appendIndent(sb, indent);
      }
    }
    sb.append('}');
  }

  private static void writeRecord(Object record, StringBuilder sb, int indent, boolean pretty) {
    sb.append('{');
    RecordComponent[] components = record.getClass().getRecordComponents();
    if (components.length > 0) {
      if (pretty) {
        sb.append('\n');
      }
      for (int i = 0; i < components.length; i++) {
        RecordComponent component = components[i];
        if (pretty) {
          appendIndent(sb, indent + 1);
        }
        writeString(component.getName(), sb);
        sb.append(':');
        if (pretty) {
          sb.append(' ');
        }
        try {
          Object value = component.getAccessor().invoke(record);
          writeValue(value, sb, indent + 1, pretty);
        } catch (Exception e) {
          sb.append("null");
        }
        if (i < components.length - 1) {
          sb.append(',');
        }
        if (pretty) {
          sb.append('\n');
        }
      }
      if (pretty) {
        appendIndent(sb, indent);
      }
    }
    sb.append('}');
  }

  private static void writeArray(Object array, StringBuilder sb, int indent, boolean pretty) {
    sb.append('[');
    int length = java.lang.reflect.Array.getLength(array);
    if (length > 0) {
      if (pretty) {
        sb.append('\n');
      }
      for (int i = 0; i < length; i++) {
        if (pretty) {
          appendIndent(sb, indent + 1);
        }
        writeValue(java.lang.reflect.Array.get(array, i), sb, indent + 1, pretty);
        if (i < length - 1) {
          sb.append(',');
        }
        if (pretty) {
          sb.append('\n');
        }
      }
      if (pretty) {
        appendIndent(sb, indent);
      }
    }
    sb.append(']');
  }

  private static void appendIndent(StringBuilder sb, int level) {
    for (int i = 0; i < level; i++) {
      sb.append("  ");
    }
  }
}
