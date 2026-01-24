package digital.pragmatech.testing.util;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleJsonWriterTest {

  @Test
  void shouldSerializeNull() {
    assertThat(SimpleJsonWriter.toJson(null)).isEqualTo("null");
  }

  @Test
  void shouldSerializeString() {
    assertThat(SimpleJsonWriter.toJson("hello")).isEqualTo("\"hello\"");
  }

  @Test
  void shouldEscapeSpecialCharactersInStrings() {
    assertThat(SimpleJsonWriter.toJson("hello\"world")).isEqualTo("\"hello\\\"world\"");
    assertThat(SimpleJsonWriter.toJson("back\\slash")).isEqualTo("\"back\\\\slash\"");
    assertThat(SimpleJsonWriter.toJson("new\nline")).isEqualTo("\"new\\nline\"");
    assertThat(SimpleJsonWriter.toJson("tab\there")).isEqualTo("\"tab\\there\"");
    assertThat(SimpleJsonWriter.toJson("carriage\rreturn")).isEqualTo("\"carriage\\rreturn\"");
  }

  @Test
  void shouldSerializeNumbers() {
    assertThat(SimpleJsonWriter.toJson(42)).isEqualTo("42");
    assertThat(SimpleJsonWriter.toJson(3.14)).isEqualTo("3.14");
    assertThat(SimpleJsonWriter.toJson(123456789L)).isEqualTo("123456789");
    assertThat(SimpleJsonWriter.toJson(-99)).isEqualTo("-99");
  }

  @Test
  void shouldSerializeBooleans() {
    assertThat(SimpleJsonWriter.toJson(true)).isEqualTo("true");
    assertThat(SimpleJsonWriter.toJson(false)).isEqualTo("false");
  }

  @Test
  void shouldSerializeInstantAsEpochSeconds() {
    Instant instant = Instant.ofEpochSecond(1700000000);
    assertThat(SimpleJsonWriter.toJson(instant)).isEqualTo("1700000000");
  }

  @Test
  void shouldSerializeEmptyList() {
    assertThat(SimpleJsonWriter.toJson(List.of())).isEqualTo("[]");
  }

  @Test
  void shouldSerializeListOfStrings() {
    List<String> list = List.of("a", "b", "c");
    assertThat(SimpleJsonWriter.toJson(list)).isEqualTo("[\"a\",\"b\",\"c\"]");
  }

  @Test
  void shouldSerializeListOfNumbers() {
    List<Integer> list = List.of(1, 2, 3);
    assertThat(SimpleJsonWriter.toJson(list)).isEqualTo("[1,2,3]");
  }

  @Test
  void shouldSerializeEmptyMap() {
    assertThat(SimpleJsonWriter.toJson(Map.of())).isEqualTo("{}");
  }

  @Test
  void shouldSerializeMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", "test");
    map.put("value", 42);
    assertThat(SimpleJsonWriter.toJson(map)).isEqualTo("{\"name\":\"test\",\"value\":42}");
  }

  @Test
  void shouldSerializeNestedStructures() {
    Map<String, Object> inner = new LinkedHashMap<>();
    inner.put("x", 1);
    inner.put("y", 2);

    Map<String, Object> outer = new LinkedHashMap<>();
    outer.put("point", inner);
    outer.put("items", List.of("a", "b"));

    String json = SimpleJsonWriter.toJson(outer);
    assertThat(json).isEqualTo("{\"point\":{\"x\":1,\"y\":2},\"items\":[\"a\",\"b\"]}");
  }

  @Test
  void shouldSerializeArray() {
    String[] array = {"one", "two", "three"};
    assertThat(SimpleJsonWriter.toJson(array)).isEqualTo("[\"one\",\"two\",\"three\"]");
  }

  @Test
  void shouldSerializeRecord() {
    record TestRecord(String name, int value) {}
    TestRecord record = new TestRecord("test", 123);
    assertThat(SimpleJsonWriter.toJson(record)).isEqualTo("{\"name\":\"test\",\"value\":123}");
  }

  @Test
  void shouldSerializeRecordWithNullField() {
    record TestRecord(String name, Integer value) {}
    TestRecord record = new TestRecord("test", null);
    assertThat(SimpleJsonWriter.toJson(record)).isEqualTo("{\"name\":\"test\",\"value\":null}");
  }

  @Test
  void shouldSerializeNestedRecords() {
    record Inner(int x, int y) {}
    record Outer(String name, Inner position) {}

    Outer outer = new Outer("point", new Inner(10, 20));
    assertThat(SimpleJsonWriter.toJson(outer))
        .isEqualTo("{\"name\":\"point\",\"position\":{\"x\":10,\"y\":20}}");
  }

  @Test
  void shouldProducePrettyPrintedJson() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", "test");
    map.put("values", List.of(1, 2));

    String pretty = SimpleJsonWriter.toJsonPretty(map);
    assertThat(pretty)
        .contains("{\n")
        .contains("  \"name\": \"test\"")
        .contains("  \"values\": [\n")
        .contains("    1,\n")
        .contains("    2\n");
  }

  @Test
  void shouldHandleControlCharacters() {
    String withControlChars = "line1\u0000line2";
    String json = SimpleJsonWriter.toJson(withControlChars);
    assertThat(json).isEqualTo("\"line1\\u0000line2\"");
  }

  @Test
  void shouldSerializeMapWithNullValue() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("key", null);
    assertThat(SimpleJsonWriter.toJson(map)).isEqualTo("{\"key\":null}");
  }

  @Test
  void shouldSerializeListWithNullValues() {
    List<String> list = Arrays.asList("a", null, "b");
    assertThat(SimpleJsonWriter.toJson(list)).isEqualTo("[\"a\",null,\"b\"]");
  }
}
