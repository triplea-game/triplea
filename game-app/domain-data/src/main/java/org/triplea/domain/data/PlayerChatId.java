package org.triplea.domain.data;

import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * PlayerChatId is an identifier assigned to players when they join chat. A playerId is 1:1 to a
 * chat session, players joining chat multiple times with multiple sessions will have multiple
 * playerIds. // TODO: should playerId be one to many for players in chat to their chat sessions?
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerChatId implements Serializable {
  private static final long serialVersionUID = 8960403951203338400L;

  private final String value;

  public static PlayerChatId newId() {
    return of(UUID.randomUUID().toString());
  }

  public static PlayerChatId of(final String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Invalid player chat id: " + value);
    }
    return new PlayerChatId(value);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof PlayerChatId)) return false;
    return value.equals(((PlayerChatId) other).value);
  }

  public boolean equals(String other) {
    return value.equals(other);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
