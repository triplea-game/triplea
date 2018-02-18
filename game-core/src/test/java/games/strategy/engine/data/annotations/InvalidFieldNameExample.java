package games.strategy.engine.data.annotations;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;

/**
 * Class with an invalid field that doesn't match the setter annotated with @GameProperty.
 */
public class InvalidFieldNameExample extends DefaultAttachment {
  private static final long serialVersionUID = 2902170223595163219L;

  protected InvalidFieldNameExample(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  // This attribute with another name than 'attribute' should cause tests to fail.
  private String notAnAttribute;

  public String getAttribute() {
    return notAnAttribute;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttribute(final String attribute) {
    this.notAnAttribute = attribute;
  }

  public void resetAttribute() {}

  @Override
  public void validate(final GameData data) {}
}
