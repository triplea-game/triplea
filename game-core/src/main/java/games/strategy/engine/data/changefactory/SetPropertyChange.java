package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;

class SetPropertyChange extends Change {
  private static final long serialVersionUID = -1377597975513821508L;
  private final String m_property;
  private final Object m_value;
  private final Object m_oldValue;

  SetPropertyChange(final String property, final Object value, final GameProperties properties) {
    m_property = property;
    m_value = value;
    m_oldValue = properties.get(property);
  }

  private SetPropertyChange(final String property, final Object value, final Object oldValue) {
    m_property = property;
    m_value = value;
    m_oldValue = oldValue;
  }

  @Override
  public Change invert() {
    return new SetPropertyChange(m_property, m_oldValue, m_value);
  }

  @Override
  protected void perform(final GameData data) {
    data.getProperties().set(m_property, m_value);
  }
}
