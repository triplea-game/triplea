package org.triplea.lobby.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

public class LobbyGameController implements ILobbyGameController {
  private final static Logger s_logger = Logger.getLogger(LobbyGameController.class.getName());
  private final Object m_mutex = new Object();
  private final Map<GUID, GameDescription> m_allGames = new HashMap<>();
  private final ILobbyGameBroadcaster m_broadcaster;

  public LobbyGameController(final ILobbyGameBroadcaster broadcaster, final IMessenger messenger) {
    m_broadcaster = broadcaster;
    final IMessenger m_messenger = messenger;
    ((IServerMessenger) m_messenger).addConnectionChangeListener(new IConnectionChangeListener() {
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
    synchronized (m_mutex) {
      final Iterator<GUID> keys = m_allGames.keySet().iterator();
      while (keys.hasNext()) {
        final GUID key = keys.next();
        final GameDescription game = m_allGames.get(key);
        if (game.getHostedBy().equals(to)) {
          keys.remove();
          removed.add(key);
        }
      }
    }
    for (final GUID guid : removed) {
      m_broadcaster.gameRemoved(guid);
    }
  }

  @Override
  public void postGame(final GUID gameID, final GameDescription description) {
    final INode from = MessageContext.getSender();
    assertCorrectHost(description, from);
    s_logger.info("Game added:" + description);
    synchronized (m_mutex) {
      m_allGames.put(gameID, description);
    }
    m_broadcaster.gameUpdated(gameID, description);
  }

  private static void assertCorrectHost(final GameDescription description, final INode from) {
    if (!from.getAddress().getHostAddress().equals(description.getHostedBy().getAddress().getHostAddress())) {
      s_logger.severe("Game modified from wrong host, from:" + from + " game host:" + description.getHostedBy());
      throw new IllegalStateException("Game from the wrong host");
    }
  }

  @Override
  public void updateGame(final GUID gameID, final GameDescription description) {
    final INode from = MessageContext.getSender();
    assertCorrectHost(description, from);
    if (s_logger.isLoggable(Level.FINE)) {
      s_logger.fine("Game updated:" + description);
    }
    synchronized (m_mutex) {
      final GameDescription oldDescription = m_allGames.get(gameID);
      // out of order updates
      // ignore, we already have the latest
      if (oldDescription.getVersion() > description.getVersion()) {
        return;
      }
      if (!oldDescription.getHostedBy().equals(description.getHostedBy())) {
        throw new IllegalStateException("Game modified by wrong host");
      }
      m_allGames.put(gameID, description);
    }
    m_broadcaster.gameUpdated(gameID, description);
  }

  @Override
  public Map<GUID, GameDescription> listGames() {
    synchronized (m_mutex) {
      final Map<GUID, GameDescription> rVal = new HashMap<>(m_allGames);
      return rVal;
    }
  }

  public void register(final IRemoteMessenger remote) {
    remote.registerRemote(this, GAME_CONTROLLER_REMOTE);
  }

  @Override
  public String testGame(final GUID gameID) {
    GameDescription description;
    synchronized (m_mutex) {
      description = m_allGames.get(gameID);
    }
    if (description == null) {
      return "No such game found";
    }
    // make sure we are being tested from the right node
    final INode from = MessageContext.getSender();
    assertCorrectHost(description, from);
    final int port = description.getPort();
    final String host = description.getHostedBy().getAddress().getHostAddress();
    s_logger.fine("Testing game connection on host:" + host + " port:" + port);
    final Socket s = new Socket();
    try {
      s.connect(new InetSocketAddress(host, port), 10 * 1000);
      s.close();
      s_logger.fine("Connection test passed for host:" + host + " port:" + port);
      return null;
    } catch (final IOException e) {
      s_logger.fine("Connection test failed for host:" + host + " port:" + port + " reason:" + e.getMessage());
      return "host:" + host + " " + " port:" + port;
    }
  }
}
