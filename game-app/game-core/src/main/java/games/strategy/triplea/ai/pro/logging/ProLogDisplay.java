package games.strategy.triplea.ai.pro.logging;

/** Shows Pro AI log output to the user. Both methods are called from AI threads. */
public interface ProLogDisplay {
  void addMessage(String message);

  void notifyNewRound(int round, String playerName);
}
