package org.triplea.ai.doesnothing;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.AiProvider;

public class DoesNothingAiProvider implements AiProvider {
  @Override
  public AbstractAi create(final String name, final PlayerTypes.AiType playerType) {
    return new DoesNothingAi(name, playerType);
  }

  @Override
  public String getLabel() {
    return PlayerTypes.DOES_NOTHING_PLAYER;
  }
}
