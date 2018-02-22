package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.annotations.InternalDoNotExport;

/**
 * Fake implementation of {@link IAttachment} useful for testing.
 */
@Immutable
public final class FakeAttachment implements IAttachment {
  private static final long serialVersionUID = 3686559484645729844L;

  private final String name;

  public FakeAttachment(final String name) {
    checkNotNull(name);

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
  @InternalDoNotExport
  public void setAttachedTo(final Attachable attachable) {
    throw new UnsupportedOperationException();
  }

  @Override
  @InternalDoNotExport
  public void setName(final String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void validate(final GameData data) {
    throw new UnsupportedOperationException();
  }

  private Map<String, ModifiableProperty<?>> createPropertyMap() {
    return ImmutableMap.<String, ModifiableProperty<?>>builder()
        .put("name", ModifiableProperty.of(this::setName, this::getName, () -> {
        }))
        .build();
  }

  @Override
  public Map<String, ModifiableProperty<?>> getPropertyMap() {
    return createPropertyMap();
  }
}
