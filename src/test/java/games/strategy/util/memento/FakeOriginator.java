package games.strategy.util.memento;

import java.util.Objects;

final class FakeOriginator {
  final int field1;

  final String field2;

  FakeOriginator(final int field1, final String field2) {
    this.field1 = field1;
    this.field2 = field2;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof FakeOriginator)) {
      return false;
    }

    final FakeOriginator other = (FakeOriginator) obj;
    return (field1 == other.field1) && Objects.equals(field2, other.field2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field1, field2);
  }

  @Override
  public String toString() {
    return String.format("FakeOriginator[field1=%d, field2=%s]", field1, field2);
  }
}
