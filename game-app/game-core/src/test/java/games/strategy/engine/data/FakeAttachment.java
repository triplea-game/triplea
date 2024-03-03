package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.Runnables;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** Fake implementation of {@link IAttachment} useful for testing. */
@Immutable
public final class FakeAttachment implements IAttachment {
  private static final long serialVersionUID = 3686559484645729844L;

  private final String name;

  public FakeAttachment(final String name) {
    this.name = checkNotNull(name);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof FakeAttachment)) {
      return false;
    }
    return Objects.equals(name, ((FakeAttachment) obj).name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).toString();
  }

  @Override
  public Attachable getAttachedTo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setAttachedTo(final Attachable attachable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setName(final String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void validate(final GameState data) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "name":
        return MutableProperty.ofString(this::setName, this::getName, Runnables.doNothing());
      default:
        return null;
    }
  }
}
