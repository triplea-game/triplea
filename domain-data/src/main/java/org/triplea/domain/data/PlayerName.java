package org.triplea.domain.data;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * AKA username, represents the display name of a player. This is the name used when taking a
 * game-seat or when chatting.
 */
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
