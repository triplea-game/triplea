package org.triplea.ai.flowfield;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.ai.AbstractBuiltInAi;
import games.strategy.triplea.ai.AiProvider;

public class FlowFieldAiProvider implements AiProvider {
  @Override
  public AbstractBuiltInAi create(final String name, final PlayerTypes.AiType playerType) {
    return new FlowFieldAi(name, playerType);
  }

  @Override
  public String getLabel() {
    return "FlowField (AI)";
  }
}
