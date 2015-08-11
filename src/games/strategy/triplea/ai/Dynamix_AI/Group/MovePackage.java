package games.strategy.triplea.ai.Dynamix_AI.Group;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.ai.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.delegate.remote.IMoveDelegate;

public class MovePackage {
  public Dynamix_AI AI = null;
  public GameData Data = null;
  public IMoveDelegate Mover = null;
  public PlayerID Player = null;
  public Object Obj1 = null;
  public Object Obj2 = null;
  public Object Obj3 = null;

  public MovePackage(final Dynamix_AI ai, final GameData data, final IMoveDelegate mover, final PlayerID player,
      final Object obj1, final Object obj2, final Object obj3) {
    AI = ai;
    Data = data;
    Mover = mover;
    Player = player;
    Obj1 = obj1;
    Obj2 = obj2;
    Obj3 = obj3;
  }

  public MovePackage SetObj1To(final Object obj1) {
    Obj1 = obj1;
    return this;
  }

  public MovePackage SetObjectsTo(final Object obj1, final Object obj2, final Object obj3) {
    Obj1 = obj1;
    Obj2 = obj2;
    Obj3 = obj3;
    return this;
  }
}
