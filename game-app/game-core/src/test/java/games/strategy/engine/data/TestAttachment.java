package games.strategy.engine.data;

import javax.annotation.Nullable;
import lombok.Getter;

/** Fake attachment used for testing. */
@Getter
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

  public void resetValue() {
    value = null;
  }

  @Override
  public void validate(final GameState data) {}

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "value":
        return MutableProperty.ofString(this::setValue, this::getValue, this::resetValue);
      default:
        return null;
    }
  }
}
