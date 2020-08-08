package org.triplea.domain.data;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * PlayerChatId is an identifier assigned to players when they join chat. A playerId is 1:1 to a
 * chat session, players joining chat multiple times with multiple sessions will have multiple
 * playerIds. // TODO: should playerId be one to many for players in chat to their chat sessions?
 */
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerChatId implements Serializable {
  private static final long serialVersionUID = 8960403951203338400L;

  private final String value;

  public static PlayerChatId newId() {
    return of(UUID.randomUUID().toString());
  }

  public static PlayerChatId of(final String value) {
    Preconditions.checkNotNull(value);

    return new PlayerChatId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
