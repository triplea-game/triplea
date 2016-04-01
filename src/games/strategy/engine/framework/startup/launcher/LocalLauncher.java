package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.util.ThreadUtil;

public class LocalLauncher extends AbstractLauncher {
  private static final Logger s_logger = Logger.getLogger(ILauncher.class.getName());
  private final IRandomSource m_randomSource;
  private final PlayerListing m_playerListing;

  public LocalLauncher(final GameSelectorModel gameSelectorModel, final IRandomSource randomSource,
      final PlayerListing playerListing) {
    super(gameSelectorModel);
    m_randomSource = randomSource;
    m_playerListing = playerListing;
  }

  @Override
  protected void launchInNewThread(final Component parent) {
    Exception exceptionLoadingGame = null;
    ServerGame game = null;
    try {
      m_gameData.doPreGameStartDataModifications(m_playerListing);
      final IServerMessenger messenger = new DummyMessenger();
      final Messengers messengers = new Messengers(messenger);
      final Set<IGamePlayer> gamePlayers =
          m_gameData.getGameLoader().createPlayers(m_playerListing.getLocalPlayerTypes());
      game = new ServerGame(m_gameData, gamePlayers, new HashMap<String, INode>(), messengers);
      game.setRandomSource(m_randomSource);
      // for debugging, we can use a scripted random source
      if (ScriptedRandomSource.useScriptedRandom()) {
        game.setRandomSource(new ScriptedRandomSource());
      }
      m_gameData.getGameLoader().startGame(game, gamePlayers, m_headless);
    } catch (final IllegalStateException e) {
      exceptionLoadingGame = e;
      Throwable error = e;
      while (error.getMessage() == null) {
        error = error.getCause();
      }
      final String message = error.getMessage();
      m_gameLoadingWindow.doneWait();
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
        }
      });
    } catch (final Exception ex) {
      ex.printStackTrace();
      exceptionLoadingGame = ex;
    } finally {
      m_gameLoadingWindow.doneWait();
    }
    try {
      if (exceptionLoadingGame == null) {
        s_logger.fine("Game starting");
        game.startGame();
        s_logger.fine("Game over");
      }
    } finally {
      // todo(kg), this does not occur on the swing thread, and this notifies setupPanel observers
      // having an oddball issue with the zip stream being closed while parsing to load default game. might be caused
      // by closing of stream while unloading map resources.
      ThreadUtil.sleep(100);
      m_gameSelectorModel.loadDefaultGame(parent);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          JOptionPane.getFrameForComponent(parent).setVisible(true);
        }
      });
    }
  }
}
