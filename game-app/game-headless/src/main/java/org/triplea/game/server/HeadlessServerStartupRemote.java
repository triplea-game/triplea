package org.triplea.game.server;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.AutoSaveFileUtils;
import games.strategy.engine.framework.HeadlessAutoSaveType;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.net.INode;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.IoUtils;

@Slf4j
public class HeadlessServerStartupRemote implements IServerStartupRemote {

  private final ServerModelView serverModelView;
  private final HeadlessGameServer headlessGameServer;

  public HeadlessServerStartupRemote(
      ServerModelView serverModelView, HeadlessGameServer headlessGameServer) {
    this.serverModelView = Preconditions.checkNotNull(serverModelView);
    this.headlessGameServer = Preconditions.checkNotNull(headlessGameServer);
  }

  @Override
  public PlayerListing getPlayerListing() {
    return serverModelView.getPlayerListing();
  }

  @Override
  public void takePlayer(final INode who, final String playerName) {
    serverModelView.takePlayer(who, playerName);
  }

  @Override
  public void releasePlayer(final INode who, final String playerName) {
    serverModelView.releasePlayer(who, playerName);
  }

  @Override
  public void disablePlayer(final String playerName) {
    serverModelView.disablePlayer(playerName);
  }

  @Override
  public void enablePlayer(final String playerName) {
    serverModelView.enablePlayer(playerName);
  }

  @Override
  public boolean isGameStarted(final INode newNode) {
    return serverModelView.isGameStarted(newNode);
  }

  @Override
  public boolean getIsServerHeadless() {
    return true;
  }

  /**
   * This should not be called from within game, only from the game setup screen, while everyone is
   * waiting for game to start.
   */
  @Override
  public byte[] getSaveGame() {
    return serverModelView.getSaveGame();
  }

  @Override
  public byte[] getGameOptions() {
    return serverModelView.getGameOptions();
  }

  @Override
  public Set<String> getAvailableGames() {
    // Copy available games collection into a serializable collection
    // so it can be sent over network.
    var games = new HashSet<String>();
    games.addAll(AutoSaveFileUtils.getAutoSaveFiles());
    games.addAll(headlessGameServer.getAvailableGames());
    return games;
  }

  @Override
  public void changeServerGameTo(final String gameName) {
    headlessGameServer.setGameMapTo(gameName);
  }

  @Override
  public void changeToLatestAutosave(final HeadlessAutoSaveType autoSaveType) {
    if (Files.exists(autoSaveType.getFile())) {
      headlessGameServer.loadGameSave(autoSaveType.getFile());
    }
  }

  @Override
  public void changeToGameSave(final byte[] bytes, final String fileName) {
    // TODO: change to a string message return, so we can tell the user/requester if it was
    // successful or not, and why if not.
    if (bytes == null) {
      return;
    }
    try {
      IoUtils.consumeFromMemory(
          bytes,
          is -> {
            try (InputStream inputStream = new BufferedInputStream(is)) {
              headlessGameServer.loadGameSave(inputStream);
            }
          });
    } catch (final Exception e) {
      log.error("Failed to load save game: " + fileName, e);
    }
  }

  @Override
  public void changeToGameOptions(final byte[] bytes) {
    // TODO: change to a string message return, so we can tell the user/requester if it was
    // successful or not, and why if not.
    if (bytes == null) {
      return;
    }
    try {
      headlessGameServer.loadGameOptions(bytes);
    } catch (final Exception e) {
      log.error("Failed to load game options", e);
    }
  }
}
