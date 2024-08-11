package games.strategy.engine.data.gameparser;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.map.data.elements.Game;
import org.triplea.map.data.elements.VariableList;

class GameDataVariablesTest {

  private static final String FOLDER = "src/test/resources/variable_parsing/";
  @NonNls private static final String EMPTY_LIST = FOLDER + "empty_list.xml";
  @NonNls private static final String SINGLE_ELEMENT_LIST = FOLDER + "single_element_list.xml";
  @NonNls private static final String MANY_ELEMENT_LIST = FOLDER + "many_element_list.xml";
  @NonNls private static final String NESTED_VARIABLE = FOLDER + "nested_variable.xml";

  @Test
  void emptyList() throws Exception {
    final VariableList xmlSample = readFile(EMPTY_LIST);

    final Map<String, List<String>> result = GameDataVariables.parse(xmlSample).getVariables();

    assertThat(result.keySet(), empty());
  }

  private static VariableList readFile(final String fileName) throws Exception {
    final Path file = Path.of(fileName);
    checkState(!Files.isDirectory(file));

    try (InputStream inputStream = new DataInputStream(Files.newInputStream(file))) {

      return new XmlMapper(inputStream).mapXmlToObject(Game.class).getVariableList();
    }
  }

  @Test
  void singleElementList() throws Exception {
    final VariableList xmlSample = readFile(SINGLE_ELEMENT_LIST);

    final Map<String, List<String>> result = GameDataVariables.parse(xmlSample).getVariables();

    assertThat(result, hasEntry("$key$", List.of("value")));
    assertThat(result.keySet(), hasSize(1));
  }

  @Test
  void manyElementList() throws Exception {
    final VariableList xmlSample = readFile(MANY_ELEMENT_LIST);

    final Map<String, List<String>> result = GameDataVariables.parse(xmlSample).getVariables();

    assertThat(result, hasEntry("$key1$", List.of("value1", "value2")));
    assertThat(result, hasEntry("$key2$", List.of("value3", "value4")));
    assertThat(result, hasEntry("$no-values$", List.of()));
    assertThat(result, hasEntry("$empty-value$", List.of("")));
    assertThat(result.keySet(), hasSize(4));
  }

  @Test
  void nestedVariable() throws Exception {
    final VariableList xmlSample = readFile(NESTED_VARIABLE);

    Map<String, List<String>> result = GameDataVariables.parse(xmlSample).getVariables();
    assertThat(result, hasEntry("$nested$", List.of("nested-value")));
    assertThat(result, hasEntry("$contains-nested$", List.of("nested-value")));
    assertThat(result, hasEntry("$many-nested$", List.of("nested-value", "nested-value")));
    assertThat(result.keySet(), hasSize(3));
  }

  @Test
  void expandVariableCombinations() throws Exception {
    GameDataVariables variables = GameDataVariables.parse(readFile(MANY_ELEMENT_LIST));
    var key1 = variables.expandVariableCombinations("$key1$");
    assertThat(key1, hasSize(2));
    assertThat(key1.get(0), equalTo(Map.of("@key1@", "value1")));
    assertThat(key1.get(1), equalTo(Map.of("@key1@", "value2")));

    var key1Key2 = variables.expandVariableCombinations("$key1$^$key2$");
    assertThat(key1Key2, hasSize(4));
    // Note: We explicitly test that the 4 elements are ordered according to variable list order.
    assertThat(key1Key2.get(0), equalTo(Map.of("@key1@", "value1", "@key2@", "value3")));
    assertThat(key1Key2.get(1), equalTo(Map.of("@key1@", "value1", "@key2@", "value4")));
    assertThat(key1Key2.get(2), equalTo(Map.of("@key1@", "value2", "@key2@", "value3")));
    assertThat(key1Key2.get(3), equalTo(Map.of("@key1@", "value2", "@key2@", "value4")));

    var key2Key1 = variables.expandVariableCombinations("$key2$^$key1$");
    assertThat(key2Key1, hasSize(4));
    assertThat(key2Key1.get(0), equalTo(Map.of("@key1@", "value1", "@key2@", "value3")));
    assertThat(key2Key1.get(1), equalTo(Map.of("@key1@", "value2", "@key2@", "value3")));
    assertThat(key2Key1.get(2), equalTo(Map.of("@key1@", "value1", "@key2@", "value4")));
    assertThat(key2Key1.get(3), equalTo(Map.of("@key1@", "value2", "@key2@", "value4")));
  }

  @Test
  void replaceForeachVariables() throws Exception {
    GameDataVariables variables = GameDataVariables.parse(readFile(MANY_ELEMENT_LIST));
    var mapping = Map.of("@key1@", "value1", "@key2@", "value3");

    assertThat(variables.replaceForeachVariables("@key1@", mapping), is("value1"));
    assertThat(variables.replaceForeachVariables("@key2@", mapping), is("value3"));
    assertThat(
        variables.replaceForeachVariables("x_@key1@_y_@key2@_z", mapping),
        is("x_value1_y_value3_z"));
  }

  @Test
  void replaceVariables() throws Exception {
    GameDataVariables variables = GameDataVariables.parse(readFile(MANY_ELEMENT_LIST));
    assertThat(variables.replaceVariables("$key1$:foo"), is("value1:value2:foo"));
  }
}
