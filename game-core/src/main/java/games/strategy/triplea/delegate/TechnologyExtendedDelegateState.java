package games.strategy.triplea.delegate;

import games.strategy.engine.data.GamePlayer;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

class TechnologyExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = -1375328472343199099L;

  Serializable superState;
  // add other variables here:
  boolean needToInitialize;
  Map<GamePlayer, Collection<TechAdvance>> techs;
}
