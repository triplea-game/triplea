package games.strategy.engine.data.gameparser;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.map.data.elements.Game;
import org.triplea.map.data.elements.VariableList;

class GameDataVariableParserTest {

  private static final String FOLDER = "src/test/resources/variable_parsing/";
  private static final String EMPTY_LIST = FOLDER + "empty_list.xml";
  private static final String SINGLE_ELEMENT_LIST = FOLDER + "single_element_list.xml";
  private static final String MANY_ELEMENT_LIST = FOLDER + "many_element_list.xml";
  private static final String NESTED_VARIABLE = FOLDER + "nested_variable.xml";

  private final GameDataVariableParser parser = new GameDataVariableParser();

  @Test
  void emptyList() throws Exception {
    final VariableList xmlSample = readFile(EMPTY_LIST);

    final Map<String, List<String>> result = parser.parseVariables(xmlSample);

    assertThat(result.keySet(), empty());
  }

  private static VariableList readFile(final String fileName) throws Exception {
    final File file = new File(fileName);
    checkState(file.isFile());

    try (InputStream inputStream = new DataInputStream(new FileInputStream(file))) {

      return new XmlMapper(inputStream).mapXmlToObject(Game.class).getVariableList();
    }
  }

  @Test
  void singleElementList() throws Exception {
    final VariableList xmlSample = readFile(SINGLE_ELEMENT_LIST);

    final Map<String, List<String>> result = parser.parseVariables(xmlSample);

    assertThat(result, hasEntry("$key$", List.of("value")));
    assertThat(result.keySet(), hasSize(1));
  }

  @Test
  void manyElementList() throws Exception {
    final VariableList xmlSample = readFile(MANY_ELEMENT_LIST);

    final Map<String, List<String>> result = parser.parseVariables(xmlSample);

    assertThat(result, hasEntry("$key1$", List.of("value1", "value2")));
    assertThat(result, hasEntry("$key2$", List.of("value3", "value4")));
    assertThat(result, hasEntry("$no-values$", List.of()));
    assertThat(result, hasEntry("$empty-value$", List.of("")));
    assertThat(result.keySet(), hasSize(4));
  }

  @Test
  void nestedVariable() throws Exception {
    final VariableList xmlSample = readFile(NESTED_VARIABLE);

    final Map<String, List<String>> result = parser.parseVariables(xmlSample);

    assertThat(result, hasEntry("$nested$", List.of("nested-value")));
    assertThat(result, hasEntry("$contains-nested$", List.of("nested-value")));
    assertThat(result, hasEntry("$many-nested$", List.of("nested-value", "nested-value")));
    assertThat(result.keySet(), hasSize(3));
  }
}
