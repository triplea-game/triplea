package games.strategy.engine.framework;

import org.jetbrains.annotations.Nls;

public class HtmlBuilder {
  final StringBuilder s = new StringBuilder();

  public HtmlBuilder() {
    s.append("<html>");
  }

  public HtmlBuilder addText(@Nls String text) {
    s.append(text);
    return this;
  }

  public HtmlBuilder lineBreak() {
    s.append("</br>");
    return this;
  }

  @Override
  public String toString() {
    return s.toString() + "</html>";
  }
}
