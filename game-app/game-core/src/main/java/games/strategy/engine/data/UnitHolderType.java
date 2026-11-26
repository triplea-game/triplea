package games.strategy.engine.data;

import java.util.Arrays;

public enum UnitHolderType {
  TERRITORY("T"),
  PLAYER("P");

  private final String id;

  UnitHolderType(String id) {
    this.id = id;
  }

  public static UnitHolderType fromId(String id) {
    return Arrays.stream(values())
        .filter(type -> type.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown UnitHolderType: " + id));
  }

  public String id() {
    return id;
  }
}
