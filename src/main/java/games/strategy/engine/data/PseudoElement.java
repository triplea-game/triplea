package games.strategy.engine.data;

import org.xml.sax.Attributes;

import com.google.common.base.Joiner;

/**
 * Data class for wrapping the important parameters of DefaultHandler methods into a single object.
 */
public final class PseudoElement {

  private final String qname;
  private final Attributes attributes;

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

  @Override
  public String toString() {
    return qname + ": " + attrToString(attributes);
  }

  /**
   * A Small utility method which nicely formats an attribute as a String.
   */
  public static String attrToString(Attributes attributes) {
    Joiner joiner = Joiner.on(' ');
    final String[] attrs = new String[attributes.getLength()];
    for (int i = 0; i < attributes.getLength(); i++) {
      attrs[i] = attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\"";
    }
    return joiner.join(attrs);
  }
}
