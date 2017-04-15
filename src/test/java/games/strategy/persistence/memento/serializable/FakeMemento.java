package games.strategy.persistence.memento.serializable;

import java.io.Serializable;
import java.util.Objects;

import games.strategy.util.memento.Memento;

final class FakeMemento implements Memento, Serializable {
  private static final long serialVersionUID = -7473561924533636022L;

  final String field;

  FakeMemento(final String field) {
    this.field = field;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof FakeMemento)) {
      return false;
    }

    final FakeMemento other = (FakeMemento) obj;
    return Objects.equals(field, other.field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field);
  }

  @Override
  public String toString() {
    return String.format("FakeMemento[field=%s]", field);
  }
}
