package games.strategy.engine.data;

/**
 * A factory for creating game data component instances for use in tests.
 */
public final class TestGameDataComponentFactory {
  private TestGameDataComponentFactory() {}

  /**
   * Creates a new {@link Resource} instance.
   *
   * @param gameData The game data associated with the resource.
   * @param name The resource name.
   *
   * @return A new {@link Resource} instance.
   */
  public static Resource newResource(final GameData gameData, final String name) {
    final Resource resource = new Resource(name, gameData);
    resource.addAttachment("key1", new FakeAttachment("attachment1"));
    resource.addAttachment("key2", new FakeAttachment("attachment2"));
    return resource;
  }
}
