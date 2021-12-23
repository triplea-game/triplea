package games.strategy.engine.data.gameparser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.triplea.map.data.elements.VariableList;

class GameDataVariableParser {

  Map<String, List<String>> parseVariables(final VariableList root) {
    return root == null ? Map.of() : parseVariableElement(root.getVariables());
  }

  private Map<String, List<String>> parseVariableElement(
      final List<VariableList.Variable> variableList) {
    final Map<String, List<String>> variables = new HashMap<>();

    for (final VariableList.Variable current : variableList) {
      final String name = "$" + current.getName() + "$";
      final List<String> values =
          current.getElements().stream()
              .map(VariableList.Variable.Element::getName)
              .flatMap(value -> findNestedVariables(value, variables))
              .collect(Collectors.toList());
      variables.put(name, values);
    }
    return variables;
  }

  private Stream<String> findNestedVariables(
      final String value, final Map<String, List<String>> variables) {
    if (!variables.containsKey(value)) {
      return Stream.of(value);
    }
    return variables.get(value).stream().flatMap(s -> findNestedVariables(s, variables));
  }
}
