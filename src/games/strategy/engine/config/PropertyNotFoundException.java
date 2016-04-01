package games.strategy.engine.config;

public class PropertyNotFoundException extends IllegalStateException {
  private static final long serialVersionUID = -7834937010739816090L;

  public PropertyNotFoundException(GameEngineProperty property) {
    super("Could not find property: " + property.toString() + ", in game engine configuration file: "+ GameEnginePropertyFileReader.getConfigFilePath());
  }
}
