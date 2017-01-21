package games.strategy.engine.random;

/**
 * A source for random numbers.
 */
public interface IRandomSource {

  /**
   * @param annotation Used by dice servers as the email subject when dice roll results are emailed. Active for
   *        PBEM games. @see HttpDiceRollerDialog
   */
  int getRandom(int max, String annotation);

  int[] getRandom(int max, int count, String annotation);
}
