package games.strategy.engine.data;

public class Resource extends NamedAttachable {
  private static final long serialVersionUID = 7471431759007499935L;

  /**
   * Creates new Resource
   *
   * @param name
   *        name of the resource
   * @param data
   *        game data
   */
  public Resource(final String name, final GameData data) {
    super(name, data);
  }
}
