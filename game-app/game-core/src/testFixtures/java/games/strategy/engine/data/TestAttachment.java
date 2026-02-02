package games.strategy.engine.data;

import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

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
  public @Nullable String getName() {
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
  public Optional<MutableProperty<?>> getPropertyOrEmpty(final @NonNls String propertyName) {
    return switch (propertyName) {
      case "value" ->
          Optional.of(MutableProperty.ofString(this::setValue, this::getValue, this::resetValue));
      default -> Optional.empty();
    };
  }
}
