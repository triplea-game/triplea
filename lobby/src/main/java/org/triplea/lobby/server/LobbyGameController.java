package org.triplea.lobby.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.triplea.lobby.common.ILobbyGameBroadcaster;
import org.triplea.lobby.common.ILobbyGameController;

import com.google.common.base.Preconditions;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import lombok.extern.java.Log;

@Log
final class LobbyGameController implements ILobbyGameController {
  private final Object mutex = new Object();
  private final Map<GUID, GameDescription> allGames = new HashMap<>();
  private final ILobbyGameBroadcaster broadcaster;
  private final Map<INode, Set<GUID>> hostToGame = new HashMap<>();

  LobbyGameController(final ILobbyGameBroadcaster broadcaster, final IServerMessenger serverMessenger) {
    this.broadcaster = broadcaster;
    serverMessenger.addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        connectionLost(to);
      }

      @Override
      public void connectionAdded(final INode to) {}
    });
  }

  private void connectionLost(final INode to) {
    final List<GUID> removed = new ArrayList<>();
    synchronized (mutex) {
      final Iterator<Map.Entry<GUID, GameDescription>> keys = allGames.entrySet().iterator();
      while (keys.hasNext()) {
        final Map.Entry<GUID, GameDescription> entry = keys.next();
        final GUID key = entry.getKey();
        final GameDescription game = entry.getValue();
        if (game.getHostedBy().equals(to)) {
          keys.remove();
          removed.add(key);
        }
      }
      hostToGame.remove(to);
    }
    for (final GUID guid : removed) {
      broadcaster.gameRemoved(guid);
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
      final GameDescription oldDescription = allGames.get(gameId);
      // out of order updates
      // ignore, we already have the latest
      // TODO: Check if this method can ever be called out of order. TCP should be able to handle that.
      if (oldDescription.getVersion() > description.getVersion()) {
        return;
      }
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
    final int port = description.getPort();
    final String host = description.getHostedBy().getAddress().getHostAddress();
    try (Socket s = new Socket()) {
      s.connect(new InetSocketAddress(host, port), 10 * 1000);
      return null;
    } catch (final IOException e) {
      return "host:" + host + " " + " port:" + port;
    }
  }

  private void assertCorrectGameOwner(final GUID gameId) {
    Preconditions.checkNotNull(gameId);
    final INode sender = MessageContext.getSender();
    synchronized (mutex) {
      final Optional<Set<GUID>> allowedGames = Optional.ofNullable(hostToGame.get(sender));
      if (!allowedGames.orElseGet(HashSet::new).contains(gameId)) {
        throw new IllegalStateException(String.format("Invalid Node %s tried accessing other game", sender));
      }
    }
  }
}
