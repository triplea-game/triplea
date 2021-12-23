package games.strategy.engine.data;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** Fake attachment used for testing. */
public class TestAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 4886924951201479496L;

  private String value;

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
  public void setName(final String name) {}

  public void setValue(final String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public void resetValue() {
    value = null;
  }

  @Override
  public void validate(final GameState data) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("value", MutableProperty.ofString(this::setValue, this::getValue, this::resetValue))
        .build();
  }
}
