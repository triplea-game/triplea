package games.strategy.triplea.ui;

import games.strategy.engine.framework.IGame;
import javax.swing.JFrame;

/** Helper interface to ease the transition of the implementation to the game-headed submodule. */
public interface ITripleAFrame {
  JFrame getFrame();

  IGame getGame();
}
