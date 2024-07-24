package org.triplea.domain.data;

import com.google.common.base.Strings;
import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode(of = "userName")
@ToString
public class ChatParticipant implements Serializable {
  private static final long serialVersionUID = 7103177780407531008L;

  @Nonnull private final String userName;

  /**
   * Identifier attached to players when joining chat so that front-end can pass values to backend
   * to identify players, specifically useful example for moderator actions.
   */
  @Nonnull private final String playerChatId;

  /** True if the player has moderator privileges. */
  @Setter private boolean isModerator;

  /** Status is custom text set by players, eg: "AFK" or "Looking for a game". */
  @Setter @Nullable private String status;

  public String getStatus() {
    return Strings.nullToEmpty(status);
  }

  public UserName getUserName() {
    return UserName.of(userName);
  }

  public PlayerChatId getPlayerChatId() {
    return PlayerChatId.of(playerChatId);
  }
}
