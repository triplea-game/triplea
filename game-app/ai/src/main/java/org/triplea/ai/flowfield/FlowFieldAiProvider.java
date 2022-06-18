package org.triplea.ai.flowfield;

import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.AiProvider;

public class FlowFieldAiProvider implements AiProvider {
  @Override
  public AbstractAi create(final String name, final String playerLabel) {
    return new FlowFieldAi(name, playerLabel);
  }

  @Override
  public String getLabel() {
    return "FlowField (AI)";
  }
}
