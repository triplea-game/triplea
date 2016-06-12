package games.strategy.triplea.player.ai;

import java.util.Collection;

/**
 * Abstract class representing game state, for use by AI game algorithms.
 *
 * @param <Play>
 *        class capable of representing a game play
 */
public abstract class GameState<Play> {
  /**
   * Get the state which will result from performing the specified play.
   *
   * @param play
   *        a legal game play
   * @return the state which will result from performing the specified play
   */
  public abstract GameState<Play> getSuccessor(Play play);

  /**
   * Get the play which resulted in this state.
   *
   * @return the play which resulted in this state
   */
  public abstract Play getMove();

  /**
   * Get the collection of all states which can be reached from this state by performing a legal play.
   *
   * @return <code>Collection</code> of successor states
   */
  public abstract Collection<GameState<Play>> successors();

  /**
   * Get the utility (or heuristic evaluation score) for this state.
   *
   * @return the utility (or heuristic evaluation score) for this state
   */
  public abstract float getUtility();

  /**
   * Test to see if the current state represents an endgame state.
   *
   * @return <code>true</code> this state represents an endgame state, <code>false</code> otherwise.
   */
  public abstract boolean gameIsOver();

  /**
   * Test to see if the current state represents a pseudu-terminal state.
   * This method is used during alpha-beta pruning.
   * <p>
   * If this method returns <code>true</code>, then <code>successors()</code> must return a non-empty
   * <code>Collection</code>.
   * <p>
   * Likewise, if this method returns <code>false</code>, then <code>successors()</code> must return an empty
   * <code>Collection</code>.
   * <p>
   * All endgame states are pseudo-terminal states. Additionally, any state which the AI search algorithm should not
   * search beyond are
   * pseudo-terminal states.
   *
   * @return <code>true</code> this state represents a pseudo-terminal state, <code>false</code> otherwise.
   */
  public abstract boolean cutoffTest();
}
