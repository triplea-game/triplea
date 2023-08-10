package games.strategy.engine.data.gameparser;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.triplea.map.data.elements.VariableList;

class GameDataVariables {
  @Getter private final Map<String, List<String>> variables;

  private GameDataVariables(Map<String, List<String>> variables) {
    this.variables = variables;
  }

  static GameDataVariables parse(final VariableList root) {
    if (root == null) {
      return new GameDataVariables(Map.of());
    }
    final Map<String, List<String>> variables = new HashMap<>();

    for (final VariableList.Variable current : root.getVariables()) {
      final String name = "$" + current.getName() + "$";
      final List<String> values =
          current.getElements().stream()
              .map(VariableList.Variable.Element::getName)
              .flatMap(value -> findNestedVariables(value, variables))
              .collect(Collectors.toList());
      variables.put(name, values);
    }
    return new GameDataVariables(Collections.unmodifiableMap(variables));
  }

  private static Stream<String> findNestedVariables(
      final String value, final Map<String, List<String>> variables) {
    if (!variables.containsKey(value)) {
      return Stream.of(value);
    }
    return variables.get(value).stream().flatMap(s -> findNestedVariables(s, variables));
  }

  public List<Map<String, String>> expandVariableCombinations(final String foreach)
      throws GameParseException {
    final List<Map<String, String>> combinations = new ArrayList<>();
    final List<String> nestedForeach = Splitter.on("^").splitToList(foreach);
    if (nestedForeach.isEmpty() || nestedForeach.size() > 2) {
      throw new GameParseException(
          "Invalid foreach expression, can only use variables, ':', and at most 1 '^': " + foreach);
    }
    final List<String> foreachVariables1 = Splitter.on(":").splitToList(nestedForeach.get(0));
    final List<String> foreachVariables2 =
        nestedForeach.size() == 2 ? Splitter.on(":").splitToList(nestedForeach.get(1)) : List.of();
    GameParsingValidation.validateForeachVariables(foreachVariables1, variables, foreach);
    GameParsingValidation.validateForeachVariables(foreachVariables2, variables, foreach);
    final int length1 = variables.get(foreachVariables1.get(0)).size();
    for (int i = 0; i < length1; i++) {
      final Map<String, String> foreachMap1 =
          createForeachVariablesMap(foreachVariables1, i, variables);
      if (foreachVariables2.isEmpty()) {
        combinations.add(foreachMap1);
      } else {
        final int length2 = variables.get(foreachVariables2.get(0)).size();
        for (int j = 0; j < length2; j++) {
          final Map<String, String> foreachMap2 =
              createForeachVariablesMap(foreachVariables2, j, variables);
          foreachMap2.putAll(foreachMap1);
          combinations.add(foreachMap2);
        }
      }
    }
    return combinations;
  }

  private Map<String, String> createForeachVariablesMap(
      final List<String> foreachVariables,
      final int currentIndex,
      final Map<String, List<String>> variables) {
    final Map<String, String> foreachMap = new HashMap<>();
    for (final String foreachVariable : foreachVariables) {
      final List<String> foreachValue = variables.get(foreachVariable);
      foreachMap.put("@" + foreachVariable.replace("$", "") + "@", foreachValue.get(currentIndex));
    }
    return foreachMap;
  }

  public String replaceForeachVariables(final String s, final Map<String, String> foreach) {
    String result = s;
    for (final Map.Entry<String, String> entry : foreach.entrySet()) {
      result = result.replace(entry.getKey(), Optional.ofNullable(entry.getValue()).orElse(""));
    }
    return result;
  }

  public String replaceVariables(final String s) {
    String result = s;
    for (final Map.Entry<String, List<String>> entry : variables.entrySet()) {
      // Avoid doing the expensive String.join() if there's nothing to replace.
      if (result.contains(entry.getKey())) {
        result = result.replace(entry.getKey(), String.join(":", entry.getValue()));
      }
    }
    return result;
  }
}
