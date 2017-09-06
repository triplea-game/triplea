package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.MoreObjects;

/**
 * Fake implementation of {@link NamedUnitHolder} useful for testing.
 */
@Immutable
public final class FakeNamedUnitHolder implements NamedUnitHolder, Serializable {
  private static final long serialVersionUID = -1802836924862350594L;

  private final String name;
  private final String type;

  public FakeNamedUnitHolder(final String name, final String type) {
    checkNotNull(name);
    checkNotNull(type);

    this.name = name;
    this.type = type;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof FakeNamedUnitHolder)) {
      return false;
    }

    final FakeNamedUnitHolder other = (FakeNamedUnitHolder) obj;
    return Objects.equals(name, other.name)
        && Objects.equals(type, other.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("type", type)
        .toString();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public UnitCollection getUnits() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void notifyChanged() {}
}
