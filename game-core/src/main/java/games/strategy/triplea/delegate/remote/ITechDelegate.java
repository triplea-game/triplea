package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.triplea.delegate.data.TechResults;
import java.io.Serializable;
import org.triplea.java.collections.IntegerMap;

/** Logic for spending tech tokens. */
public interface ITechDelegate extends IRemote, IDelegate {
  /**
   * Rolls for the specified tech.
   *
   * @param rollCount the number of tech rolls
   * @param techToRollFor the tech category to roll for, should be null if the game does not support
   *     rolling for certain techs
   * @param newTokens if WW2V3TechModel is used it set rollCount
   * @return TechResults. If the tech could not be rolled, then a message saying why.
   */
  @RemoteActionCode(8)
  TechResults rollTech(
      int rollCount,
      TechnologyFrontier techToRollFor,
      int newTokens,
      IntegerMap<GamePlayer> whoPaysHowMuch);

  @RemoteActionCode(6)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(10)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(11)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(4)
  @Override
  String getName();

  @RemoteActionCode(3)
  @Override
  String getDisplayName();

  @RemoteActionCode(2)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(9)
  @Override
  Serializable saveState();

  @RemoteActionCode(7)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(5)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
