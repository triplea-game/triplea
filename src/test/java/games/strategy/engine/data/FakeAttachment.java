package games.strategy.engine.data;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Fake implementation of {@link IAttachment} useful for testing.
 */
public final class FakeAttachment implements IAttachment {
  private static final long serialVersionUID = 3686559484645729844L;

  private final String name;

  public FakeAttachment(final String name) {
    this.name = name;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof FakeAttachment)) {
      return false;
    }

    final FakeAttachment other = (FakeAttachment) obj;
    return Objects.equals(name, other.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .toString();
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
  public void validate(final GameData data) {
    throw new UnsupportedOperationException();
  }
}
