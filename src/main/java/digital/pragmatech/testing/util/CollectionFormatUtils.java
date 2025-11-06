package digital.pragmatech.testing.util;

import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public final class CollectionFormatUtils {

  /**
   * Convert collection of objects to SortedSet of String. This can be helpful for visually
   * comparable representations.
   *
   * @param elements
   * @return
   */
  public static SortedSet<String> toStringSortedSet(Collection<?> elements) {
    var set = new TreeSet<String>();
    elements.forEach(e -> set.add(Objects.toString(e)));
    return set;
  }

  /**
   * Pretty format collection elements (new line for each element).
   *
   * @param elements
   * @return
   */
  public static String prettyPrintCollection(Collection<String> elements) {
    if (elements == null) {
      return "";
    }
    if (elements.isEmpty()) {
      return "[]";
    }
    if (elements.size() == 1) {
      return "[" + elements.iterator().next() + "]";
    }
    return "[\n" + String.join(",\n", elements) + "\n]";
  }

  private CollectionFormatUtils() {}
}
