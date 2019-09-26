package games.strategy.engine.lobby;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Simple value object to encapsulate a player name and provide strong typing. */
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class PlayerName {
  @Getter private final String value;

  @Override
  public String toString() {
    return getValue();
  }
}
