package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.util.IntegerMap;

public interface ITechDelegate extends IRemote, IDelegate {
  /**
   * @param rollCount
   *        the number of tech rolls
   * @param techToRollFor
   *        the tech category to roll for, should be null if the game does not support
   *        rolling for certain techs
   * @param newTokens
   *        if WW2V3TechModel is used it set rollCount
   * @return TechResults. If the tech could not be rolled, then a message saying why.
   */
  TechResults rollTech(int rollCount, TechnologyFrontier techToRollFor, int newTokens,
      IntegerMap<PlayerID> whoPaysHowMuch);
}
