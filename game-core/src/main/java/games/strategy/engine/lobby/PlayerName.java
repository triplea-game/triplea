package games.strategy.engine.lobby;

import lombok.AllArgsConstructor;

/** Simple value object to encapsulate a player name and provide strong typing. */
@AllArgsConstructor(staticName = "of")
public class PlayerName {
  private final String name;

  public String getValue() {
    return name;
  }
}
