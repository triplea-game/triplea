package org.triplea.game.server;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.AutoSaveFileUtils;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.net.INode;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

  @Override
  public byte[] getGameOptions() {
    return serverModelView.getGameOptions();
  }

  @Override
  public List<String> getAvailableGames() {
    // Copy available games collection into a serializable collection
    // so it can be sent over network.
    List<String> availableGames = new ArrayList<>(headlessGameServer.getAvailableGames());
    availableGames.addAll(AutoSaveFileUtils.getAutoSaveFiles());
    return availableGames;
  }

  @Override
  public void changeServerGameTo(final String gameName) {
    headlessGameServer.setGameMapTo(gameName);
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
