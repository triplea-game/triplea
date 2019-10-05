package games.strategy.engine.data;

/** An effect that may provide a bonus or penalty to units within a territory. */
public class TerritoryEffect extends NamedAttachable {
  private static final long serialVersionUID = 7574312162462968921L;

  public TerritoryEffect(final String name, final GameData data) {
    super(name, data);
  }
}
