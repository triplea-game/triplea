package games.strategy.engine.chat;

import com.google.common.base.Strings;
import games.strategy.engine.lobby.PlayerName;
import java.io.Serializable;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode(of = "playerName")
@ToString
public class ChatParticipant implements Serializable {
  private static final long serialVersionUID = 7103177780407531008L;
  @NonNull private final PlayerName playerName;
  private final boolean isModerator;
  @Setter @Nullable private String status;

  public String getStatus() {
    return Strings.nullToEmpty(status);
  }
}
