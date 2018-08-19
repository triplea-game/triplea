package games.strategy.engine.xml;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;

/**
 * Fake attachment used for testing.
 *
 * <p>
 * Although this attachment is only ever used by test code, it is located in production code because it must be
 * available to the game parser.
 * </p>
 */
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

  @InternalDoNotExport
  @Override
  public void setAttachedTo(final Attachable unused) {}

  @Override
  public String getName() {
    return null;
  }

  @InternalDoNotExport
  @Override
  public void setName(final String name) {}

  public void setValue(final String value) {
    m_value = value;
  }

  public String getValue() {
    return m_value;
  }

  public void resetValue() {
    m_value = null;
  }

  @Override
  public void validate(final GameData data) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("value", MutableProperty.ofString(this::setValue, this::getValue, this::resetValue))
        .build();
  }
}
