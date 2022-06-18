package games.strategy.triplea.ai;

public interface AiProvider {
  AbstractAi create(String name, String playerLabel);

  String getLabel();
}
