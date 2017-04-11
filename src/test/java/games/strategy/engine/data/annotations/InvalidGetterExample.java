package games.strategy.engine.data.annotations;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;

/**
 * An example where the @GameProperty is used on a non-setter.
 */
public class InvalidGetterExample extends DefaultAttachment {
  private static final long serialVersionUID = 8284101951970184012L;

  protected InvalidGetterExample(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  private String m_attribute;

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public String getAttribute() { // annotation put on a getter instead of a setter, should cause test to fail
    return m_attribute;
  }

  public void setAttribute(final String attribute) {
    m_attribute = attribute;
  }

  public void resetAttribute() {}

  @Override
  public void validate(final GameData data) throws GameParseException {}
}
