package games.strategy.engine.config;

public interface PropertyReader {
  String readProperty(GameEngineProperty propertyKey);

  String readProperty(GameEngineProperty propertyKey, String defaultValue);
}
