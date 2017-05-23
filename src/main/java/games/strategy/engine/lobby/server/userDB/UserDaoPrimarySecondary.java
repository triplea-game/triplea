package games.strategy.engine.lobby.server.userDB;

/**
 * Allows for configuration flagging of multiple UserDao instances.
 *
 * <p>
 * to exist side by side, one in
 * primary mode, and zero or many in secondary mode. This config is intended for writes
 * to go to the primary and all secondarys, and reads to come from primary.
 * </p>
 */
public interface UserDaoPrimarySecondary extends UserDao {
  /**
   * True if this is an authoritative data source.
   */
  boolean isPrimary();
}
