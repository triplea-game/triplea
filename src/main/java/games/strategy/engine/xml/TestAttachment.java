package games.strategy.engine.xml;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;

public class TestAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 4886924951201479496L;
  private String m_value;

  /** Creates new TestAttachment. */
  public TestAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  @Override
  public Attachable getAttachedTo() {
    return null;
  }

  @Override
  public void setAttachedTo(final Attachable unused) {}

  @Override
  public String getName() {
    return null;
  }

  @Override
  public void setName(final String aString) {}

  public void setValue(final String value) {
    m_value = value;
  }

  public String getValue() {
    return m_value;
  }

  @Override
  public void validate(final GameData data) {}
}
