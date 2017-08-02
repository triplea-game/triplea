package games.strategy.engine.lobby.server;

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

class LobbyGameController implements ILobbyGameController {
  private static final Logger logger = Logger.getLogger(LobbyGameController.class.getName());
  private final Object m_mutex = new Object();
  private final Map<GUID, GameDescription> m_allGames = new HashMap<>();
  private final ILobbyGameBroadcaster m_broadcaster;

  LobbyGameController(final ILobbyGameBroadcaster broadcaster, final IMessenger messenger) {
    m_broadcaster = broadcaster;
    ((IServerMessenger) messenger).addConnectionChangeListener(new IConnectionChangeListener() {
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
  public void postGame(final GUID gameId, final GameDescription description) {
    final INode from = MessageContext.getSender();
    assertCorrectHost(description, from);
    logger.info("Game added:" + description);
    synchronized (m_mutex) {
      m_allGames.put(gameId, description);
    }
    m_broadcaster.gameUpdated(gameId, description);
  }

  private static void assertCorrectHost(final GameDescription description, final INode from) {
    if (!from.getAddress().getHostAddress().equals(description.getHostedBy().getAddress().getHostAddress())) {
      logger.severe("Game modified from wrong host, from:" + from + " game host:" + description.getHostedBy());
      throw new IllegalStateException("Game from the wrong host");
    }
  }

  @Override
  public void updateGame(final GUID gameId, final GameDescription description) {
    final INode from = MessageContext.getSender();
    assertCorrectHost(description, from);
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Game updated:" + description);
    }
    synchronized (m_mutex) {
      final GameDescription oldDescription = m_allGames.get(gameId);
      // out of order updates
      // ignore, we already have the latest
      if (oldDescription.getVersion() > description.getVersion()) {
        return;
      }
      if (!oldDescription.getHostedBy().equals(description.getHostedBy())) {
        throw new IllegalStateException("Game modified by wrong host");
      }
      m_allGames.put(gameId, description);
    }
    m_broadcaster.gameUpdated(gameId, description);
  }

  @Override
  public Map<GUID, GameDescription> listGames() {
    synchronized (m_mutex) {
      final Map<GUID, GameDescription> rVal = new HashMap<>(m_allGames);
      return rVal;
    }
  }

  void register(final IRemoteMessenger remote) {
    remote.registerRemote(this, GAME_CONTROLLER_REMOTE);
  }

  @Override
  public String testGame(final GUID gameId) {
    GameDescription description;
    synchronized (m_mutex) {
      description = m_allGames.get(gameId);
    }
    if (description == null) {
      return "No such game found";
    }
    // make sure we are being tested from the right node
    final INode from = MessageContext.getSender();
    assertCorrectHost(description, from);
    final int port = description.getPort();
    final String host = description.getHostedBy().getAddress().getHostAddress();
    logger.fine("Testing game connection on host:" + host + " port:" + port);
    final Socket s = new Socket();
    try {
      s.connect(new InetSocketAddress(host, port), 10 * 1000);
      s.close();
      logger.fine("Connection test passed for host:" + host + " port:" + port);
      return null;
    } catch (final IOException e) {
      logger.fine("Connection test failed for host:" + host + " port:" + port + " reason:" + e.getMessage());
      return "host:" + host + " " + " port:" + port;
    }
  }
}
