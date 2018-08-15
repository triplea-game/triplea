package games.strategy.engine.message;

import lombok.Getter;

/**
 * Description for a Channel or a Remote end point.
 */
@Getter
public class RemoteName {
  private final String name;
  private final Class<?> clazz;

  public RemoteName(final String name, final Class<?> clazz) {
    if (clazz == null) {
      throw new IllegalArgumentException("Class cannot be null. Remote Name: " + name);
    }
    if (!clazz.isInterface()) {
      throw new IllegalArgumentException("Not an interface. Remote Name: " + name);
    }
    this.name = name;
    this.clazz = clazz;
  }

  @Override
  public String toString() {
    return name + ":" + clazz.getSimpleName();
  }
}
