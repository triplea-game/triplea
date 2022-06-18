package games.strategy.triplea.ai;

import games.strategy.engine.framework.startup.ui.PlayerTypes;

public interface AiProvider {
  AbstractBuiltInAi create(String name, PlayerTypes.AiType playerType);

  String getLabel();
}
