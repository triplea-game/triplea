package games.strategy.triplea.ai;

import games.strategy.engine.framework.startup.ui.PlayerTypes;

/** Base class for externally provided AIs */
public abstract class AbstractAi extends AbstractBuiltInAi {
  private final PlayerTypes.AiType playerType;

  public AbstractAi(final String name, final PlayerTypes.AiType playerType) {
    super(name);
    this.playerType = playerType;
  }

  @Override
  public PlayerTypes.Type getPlayerType() {
    return playerType;
  }
}
