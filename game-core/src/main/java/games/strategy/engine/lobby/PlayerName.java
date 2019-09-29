package games.strategy.engine.lobby;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Simple value object to encapsulate a player name and provide strong typing. */
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class PlayerName implements Serializable {
  private static final long serialVersionUID = 8356372044000232198L;

  @Getter private final String value;

  @Override
  public String toString() {
    return getValue();
  }
}
