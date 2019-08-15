package org.triplea.lobby.server;

import com.google.common.base.Preconditions;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.java.Log;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.ILobbyGameBroadcaster;
import org.triplea.lobby.common.ILobbyGameController;

@Log
final class LobbyGameController implements ILobbyGameController {
  private final Object mutex = new Object();
  private final Map<GUID, GameDescription> allGames = new HashMap<>();
  private final ILobbyGameBroadcaster broadcaster;
  private final Map<INode, Set<GUID>> hostToGame = new HashMap<>();

  LobbyGameController(
      final ILobbyGameBroadcaster broadcaster, final IServerMessenger serverMessenger) {
    this.broadcaster = broadcaster;
    serverMessenger.addConnectionChangeListener(
        new IConnectionChangeListener() {
          @Override
          public void connectionRemoved(final INode to) {
            connectionLost(to);
          }

          @Override
          public void connectionAdded(final INode to) {}
        });
  }

  private void connectionLost(final INode to) {
    final Set<GUID> games;
    synchronized (mutex) {
      games = hostToGame.remove(to);
      Optional.ofNullable(games)
          .ifPresent(
              g -> {
                allGames.keySet().removeAll(g);
                g.forEach(broadcaster::gameRemoved);
              });
    }
  }

  @Override
  public void postGame(final GUID gameId, final GameDescription description) {
    synchronized (mutex) {
      allGames.put(gameId, description);
      hostToGame.computeIfAbsent(MessageContext.getSender(), k -> new HashSet<>()).add(gameId);
    }
    log.info("Game added:" + description);
    broadcaster.gameUpdated(gameId, description);
  }

  @Override
  public void updateGame(final GUID gameId, final GameDescription description) {
    assertCorrectGameOwner(gameId);
    synchronized (mutex) {
      allGames.put(gameId, description);
    }
    broadcaster.gameUpdated(gameId, description);
  }

  @Override
  public Map<GUID, GameDescription> listGames() {
    synchronized (mutex) {
      return new HashMap<>(allGames);
    }
  }

  void register(final IRemoteMessenger remote) {
    remote.registerRemote(this, REMOTE_NAME);
  }

  @Override
  public String testGame(final GUID gameId) {
    assertCorrectGameOwner(gameId);
    final GameDescription description;
    synchronized (mutex) {
      description = allGames.get(gameId);
    }
    if (description == null) {
      return "No such game found";
    }
    // make sure we are being tested from the right node
    final InetSocketAddress address = description.getHostedBy().getSocketAddress();
    try (Socket s = new Socket()) {
      s.connect(address, 10 * 1000);
      return null;
    } catch (final IOException e) {
      return "host:" + address.getHostName() + " " + " port:" + address.getPort();
    }
  }

  private void assertCorrectGameOwner(final GUID gameId) {
    Preconditions.checkNotNull(gameId);
    final INode sender = MessageContext.getSender();
    synchronized (mutex) {
      final Optional<Set<GUID>> allowedGames = Optional.ofNullable(hostToGame.get(sender));
      if (!allowedGames.orElseGet(Collections::emptySet).contains(gameId)) {
        throw new IllegalStateException(
            String.format("Invalid Node %s tried accessing other game", sender));
      }
    }
  }
}
