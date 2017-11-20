package games.strategy.engine.message;

/**
 * Description for a Channel or a Remote end point.
 */
public class RemoteName {
  private final String name;
  private final Class<?> clazz;

  public RemoteName(final Class<?> class1, final String name) {
    this(name, class1);
  }

  public RemoteName(final String name, final Class<?> class1) {
    if (class1 == null) {
      throw new IllegalArgumentException("Class cannot be null. Remote Name: " + name);
    }
    if (!class1.isInterface()) {
      throw new IllegalArgumentException("Not an interface. Remote Name: " + name);
    }
    this.name = name;
    clazz = class1;
  }

  public Class<?> getClazz() {
    return clazz;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name + ":" + clazz.getSimpleName();
  }
}
