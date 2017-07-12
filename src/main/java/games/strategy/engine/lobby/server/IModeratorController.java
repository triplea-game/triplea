package games.strategy.engine.lobby.server;

import java.time.Instant;
import java.util.Date;

import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;

public interface IModeratorController extends IRemote {
  /**
   * Boot the given INode from the network.
   *
   * <p>
   * This method can only be called by admin users.
   * </p>
   */
  void boot(INode node);

  // Remove the z Prefix of all methods in the next incompatible release

  /**
   * @deprecated Kept to maintain backwards compatibility.
   *             Remove with next incompatible release.
   */
  @Deprecated
  default void banUsername(INode node, Date banExpires) {
    zzBanUsername(node, banExpires != null ? banExpires.toInstant() : null);
  }

  /**
   * Ban the username of the given INode.
   */
  void zzBanUsername(INode node, Instant banExpires);

  /**
   * @deprecated Kept to maintain backwards compatibility.
   *             Remove with next incompatible release.
   */
  @Deprecated
  default void banIp(INode node, Date banExpires) {}

  /**
   * @deprecated Kept to maintain backwards compatibility.
   *             Remove with next incompatible release.
   */
  @Deprecated
  default void banMac(INode node, Date banExpires) {
    zzBanMac(node, banExpires != null ? banExpires.toInstant() : null);
  }

  /**
   * @deprecated Kept to maintain backwards compatibility.
   *             Remove with next incompatible release.
   */
  @Deprecated
  default void banMac(final INode node, final String hashedMac, final Date banExpires) {
    zzBanMac(node, banExpires != null ? banExpires.toInstant() : null);
  }

  /**
   * Ban the mac of the given INode.
   */
  void zzBanMac(INode node, Instant banExpires);

  /**
   * Ban the mac.
   */
  void zzBanMac(final INode node, final String hashedMac, final Instant banExpires);

  /**
   * @deprecated Kept to maintain backwards compatibility.
   *             Remove with next incompatible release.
   */
  @Deprecated
  default void muteUsername(INode node, Date muteExpires) {
    zzMuteUsername(node, muteExpires != null ? muteExpires.toInstant() : null);
  }

  /**
   * Mute the username of the given INode.
   */
  void zzMuteUsername(INode node, Instant muteExpires);

  /**
   * @deprecated Kept to maintain backwards compatibility.
   *             Remove with next incompatible release.
   */
  @Deprecated
  default void muteIp(INode node, Date muteExpires) {}

  /**
   * @deprecated Kept to maintain backwards compatibility.
   *             Remove with next incompatible release.
   */
  @Deprecated
  default void muteMac(INode node, Date muteExpires) {
    zzMuteMac(node, muteExpires != null ? muteExpires.toInstant() : null);
  }

  /**
   * Mute the mac of the given INode.
   */
  void zzMuteMac(INode node, Instant muteExpires);

  /**
   * Get list of people in the game.
   */
  String getHostConnections(INode node);

  /**
   * Remote get chat log of a headless host bot.
   */
  String getChatLogHeadlessHostBot(INode node, String hashedPassword, String salt);

  /**
   * Remote mute player in a headless host bot.
   */
  String mutePlayerHeadlessHostBot(INode node, String playerNameToBeBooted, int minutes, String hashedPassword,
      String salt);

  /**
   * Remote boot player in a headless host bot.
   */
  String bootPlayerHeadlessHostBot(INode node, String playerNameToBeBooted, String hashedPassword, String salt);

  /**
   * Remote ban player in a headless host bot.
   */
  String banPlayerHeadlessHostBot(INode node, String playerNameToBeBanned, int hours, String hashedPassword,
      String salt);

  /**
   * Remote stop game of a headless host bot.
   */
  String stopGameHeadlessHostBot(INode node, String hashedPassword, String salt);

  /**
   * Remote shutdown of a headless host bot.
   */
  String shutDownHeadlessHostBot(INode node, String hashedPassword, String salt);

  /**
   * For use with a password for the bot.
   */
  String getHeadlessHostBotSalt(INode node);

  /**
   * Reset the password of the given user. Returns null if the password was updated without error.
   *
   * <p>
   * You cannot change the password of an anonymous node, and you cannot change the password for an admin user.
   * </p>
   *
   * @deprecated Remove usages of this. Does not make sense for moderators to be able to reset passwords of logged
   *             in users.
   */
  @Deprecated
  String setPassword(INode node, String hashedPassword);

  String getInformationOn(INode node);

  /**
   * Is the current user an admin.
   */
  boolean isAdmin();

  /**
   * Is this node an admin.
   */
  boolean isPlayerAdmin(final INode node);
}
