package games.strategy.engine.framework.startup.mc;

import java.io.Serializable;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;

/** Represents a message sent to the server, from a moderator */
@Value
public class ModeratorMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  @Nonnull String action;
  @Nonnull String playerName;

  public static ModeratorMessage newDisconnect(String playerName) {
    return new ModeratorMessage("disconnect", playerName);
  }

  public static ModeratorMessage newBan(String playerName) {
    return new ModeratorMessage("ban", playerName);
  }

  boolean isBan() {
    return "ban".equalsIgnoreCase(action);
  }

  boolean isDisconnect() {
    return "disconnect".equalsIgnoreCase(action);
  }
}
