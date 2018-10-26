package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import games.strategy.engine.data.PlayerID;

class TechnologyExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = -1375328472343199099L;
  Serializable superState;
  // add other variables here:
  public boolean m_needToInitialize;
  Map<PlayerID, Collection<TechAdvance>> m_techs;
}
