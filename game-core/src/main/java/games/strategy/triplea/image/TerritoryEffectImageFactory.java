package games.strategy.triplea.image;

/**
 * Used to manage territory effect images.
 */
public class TerritoryEffectImageFactory extends AbstractImageFactory {

  static final String FILE_NAME_BASE = "territoryEffects/";

  public TerritoryEffectImageFactory() {}

  @Override
  protected String getFileNameBase() {
    return FILE_NAME_BASE;
  }

}
