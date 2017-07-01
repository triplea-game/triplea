package games.strategy.engine.data;

import org.xml.sax.Attributes;

public class PseudoElement {

  private final String qname;
  private final Attributes attributes;
  private String innerValue = null;

  public PseudoElement(String qname, Attributes attributes) {
    this.qname = qname;
    this.attributes = attributes;
  }

  public String getName() {
    return qname;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public void setInnerValue(String value) {
    if (innerValue == null) {
      innerValue = value;
    } else {
      throw new IllegalStateException("Inner value can only be set once");
    }
  }

  public String getInnerValue() {
    return innerValue;
  }
}
