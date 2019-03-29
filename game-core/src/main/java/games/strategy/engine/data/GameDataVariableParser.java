package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Element;

class GameDataVariableParser {

  private final NodeFinder nodeFinder = new NodeFinder();

  Map<String, List<String>> parseVariables(final Element root) throws GameParseException {
    final Element variableList = nodeFinder.getOptionalSingleChild("variableList", root);
    return variableList != null ? parseVariableElement(variableList) : Collections.emptyMap();
  }

  private Map<String, List<String>> parseVariableElement(final Element root) {
    checkNotNull(root);
    final Map<String, List<String>> variables = new HashMap<>();
    for (final Element current : nodeFinder.getChildren("variable", root)) {
      final String name = current.getAttribute("name");
      final List<String> values = nodeFinder.getChildren("element", current).stream()
          .map(element -> element.getAttribute("name"))
          .flatMap(value -> findNestedVariables(value, variables))
          .collect(Collectors.toList());
      variables.put(name, values);
    }
    return variables;
  }

  private Stream<String> findNestedVariables(final String value, final Map<String, List<String>> variables) {
    if (!variables.containsKey(value)) {
      return Stream.of(value);
    }
    return variables.get(value).stream()
        .flatMap(s -> findNestedVariables(s, variables));
  }

}
