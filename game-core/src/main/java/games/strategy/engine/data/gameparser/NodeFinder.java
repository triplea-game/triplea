package games.strategy.engine.data.gameparser;

import games.strategy.engine.data.GameParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class NodeFinder {

  Element getSingleChild(final String name, final Node node) throws GameParseException {
    final List<Element> children = getChildren(name, node);
    if (children.size() != 1) {
      throw new GameParseException(
          "Expected one child node named: " + name + ", found: " + children.size());
    }
    return children.get(0);
  }

  Element getOptionalSingleChild(final String name, final Node node) throws GameParseException {
    final List<Element> children = getChildren(name, node);
    if (children.size() > 1) {
      throw new GameParseException("Too many children named: " + name);
    }
    return children.isEmpty() ? null : children.get(0);
  }

  List<Element> getChildren(final String name, final Node node) {
    final NodeList children = node.getChildNodes();
    return IntStream.range(0, children.getLength())
        .mapToObj(children::item)
        .filter(current -> current.getNodeName().equals(name))
        .map(Element.class::cast)
        .collect(Collectors.toList());
  }
}
