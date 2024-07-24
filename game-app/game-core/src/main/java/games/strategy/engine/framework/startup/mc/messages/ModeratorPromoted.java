package games.strategy.engine.framework.startup.mc.messages;

import java.io.Serializable;
import lombok.Value;

/**
 * This is a message sent to all players to indicate moderator player has changed. For example, in a
 * bot game, if there are 3 players - then the bot and oldest joined will be moderators. If the
 * oldest joined leaves, then the remaining player become smoderator.
 */
@Value
public class ModeratorPromoted implements Serializable {
  /** The name of the player who is now a moderator. */
  String playerName;
}
