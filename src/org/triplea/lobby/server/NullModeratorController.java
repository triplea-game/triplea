package org.triplea.lobby.server;

import java.util.Date;

import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;

/**
 * For Server Games, not the Lobby.
 */
public class NullModeratorController extends AbstractModeratorController {
  public NullModeratorController(final IServerMessenger messenger, final Messengers messengers) {
    super(messenger, messengers);
  }

  @Override
  public void banMac(final INode node, final String hashedMac, final Date banExpires) {
    // nothing
  }

  @Override
  public String mutePlayerHeadlessHostBot(final INode node, final String playerNameToBeMuted, final int minutes,
      final String hashedPassword, final String salt) {
    return null;
  }

  @Override
  public String bootPlayerHeadlessHostBot(final INode node, final String playerNameToBeBooted,
      final String hashedPassword, final String salt) {
    return null;
  }

  @Override
  public String getHostConnections(final INode node) {
    return null;
  }

  @Override
  public String getHeadlessHostBotSalt(final INode node) {
    return null;
  }

  @Override
  public String getChatLogHeadlessHostBot(final INode node, final String hashedPassword, final String salt) {
    return null;
  }

  @Override
  public String banPlayerHeadlessHostBot(final INode node, final String playerNameToBeBanned, final int hours,
      final String hashedPassword, final String salt) {
    return null;
  }

  @Override
  public String stopGameHeadlessHostBot(final INode node, final String hashedPassword, final String salt) {
    return null;
  }

  @Override
  public String shutDownHeadlessHostBot(final INode node, final String hashedPassword, final String salt) {
    return null;
  }

  @Override
  public void banUsername(final INode node, final Date banExpires) {
    // nothing
  }

  @Override
  public void banIp(final INode node, final Date banExpires) {
    // nothing
  }

  @Override
  public void banMac(final INode node, final Date banExpires) {
    // nothing
  }

  @Override
  public void muteUsername(final INode node, final Date muteExpires) {
    // nothing
  }

  @Override
  public void muteIp(final INode node, final Date muteExpires) {
    // nothing
  }

  @Override
  public void muteMac(final INode node, final Date muteExpires) {
    // nothing
  }

  @Override
  public void boot(final INode node) {
    // nothing
  }

  void assertUserIsAdmin() {
    throw new IllegalStateException("Not an admin");
  }

  @Override
  public boolean isAdmin() {
    return false;
  }

  @Override
  public boolean isPlayerAdmin(final INode node) {
    return false;
  }

  @Override
  public String getInformationOn(final INode node) {
    return "Feature not enabled in NullModeratorController";
  }

  @Override
  public String setPassword(final INode node, final String hashedPassword) {
    return "Feature not enabled in NullModeratorController";
  }
}
