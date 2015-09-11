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
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

public class LobbyGameController implements ILobbyGameController {
  private final static Logger logger = Logger.getLogger(LobbyGameController.class.getName());
  private final Object mutex = new Object();
  private final Map<GUID, GameDescription> allGames = new HashMap<GUID, GameDescription>();
  private final ILobbyGameBroadcaster broadcaster;

  public LobbyGameController(final ILobbyGameBroadcaster broadcaster, final IServerMessenger messenger) {
    this.broadcaster = broadcaster;
    messenger.addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        connectionLost(to);
      }

      @Override
      public void connectionAdded(final INode to) {}
    });
  }

  private void connectionLost(final INode to) {
    final List<GUID> removed = new ArrayList<GUID>();
    synchronized (mutex) {
      final Iterator<GUID> keys = allGames.keySet().iterator();
      while (keys.hasNext()) {
        final GUID key = keys.next();
        final GameDescription game = allGames.get(key);
        if (game.getHostedBy().equals(to)) {
          keys.remove();
          removed.add(key);
        }
      }
    }
    for (final GUID guid : removed) {
      broadcaster.gameRemoved(guid);
    }
  }

  @Override
  public void postGame(final GUID gameID, final GameDescription description) {
    final INode from = MessageContext.getSender();
    assertCorrectHost(description, from);
    logger.info("Game added:" + description);
    synchronized (mutex) {
      allGames.put(gameID, description);
    }
    broadcaster.gameAdded(gameID, description);
  }

  private static void assertCorrectHost(final GameDescription description, final INode from) {
    if (!from.getAddress().getHostAddress().equals(description.getHostedBy().getAddress().getHostAddress())) {
      logger.severe("Game modified from wrong host, from:" + from + " game host:" + description.getHostedBy());
      throw new IllegalStateException("Game from the wrong host, from: " +  from.getAddress().getHostAddress() + ", hosted by:  " + description.getHostedBy().getAddress().getHostAddress());
    }
  }

  @Override
  public void updateGame(final GUID gameID, final GameDescription description) {
    final INode from = MessageContext.getSender();
    assertCorrectHost(description, from);
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Game updated:" + description);
    }
    synchronized (mutex) {
      final GameDescription oldDescription = allGames.get(gameID);
      // out of order updates
      // ignore, we already have the latest
      if (oldDescription.getVersion() > description.getVersion()) {
        return;
      }
      if (!oldDescription.getHostedBy().equals(description.getHostedBy())) {
        throw new IllegalStateException("Game modified by wrong host");
      }
      allGames.put(gameID, description);
    }
    broadcaster.gameUpdated(gameID, description);
  }

  @Override
  public Map<GUID, GameDescription> listGames() {
    synchronized (mutex) {
      final Map<GUID, GameDescription> rVal = new HashMap<GUID, GameDescription>(allGames);
      return rVal;
    }
  }

  public void register(final IRemoteMessenger remote) {
    remote.registerRemote(this, GAME_CONTROLLER_REMOTE);
  }

  @Override
  public String testGame(final GUID gameID) {
    GameDescription description;
    synchronized (mutex) {
      description = allGames.get(gameID);
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
