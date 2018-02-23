package games.strategy.engine.chat;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingConstants;

import com.google.common.base.Joiner;

import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;
import games.strategy.net.INode;
import games.strategy.triplea.ui.UiContext;

public class PlayerChatRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = -8195565028281374498L;
  private final IGame game;
  private final UiContext uiContext;
  int maxIconCounter = 0;
  HashMap<String, List<Icon>> iconMap = new HashMap<>();
  HashMap<String, Set<String>> playerMap = new HashMap<>();

  public PlayerChatRenderer(final IGame game, final UiContext uiContext) {
    this.game = game;
    this.uiContext = uiContext;
    setIconMap();
  }

  @Override
  public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
      final boolean isSelected, final boolean cellHasFocus) {
    final List<Icon> icons = iconMap.get(value.toString());
    if (icons != null) {
      super.getListCellRendererComponent(list, ((INode) value).getName(), index, isSelected, cellHasFocus);
      setHorizontalTextPosition(SwingConstants.LEFT);
      setIcon(new CompositeIcon(icons));
    } else {
      final StringBuilder sb = new StringBuilder(((INode) value).getName());
      final Set<String> players = playerMap.get(value.toString());
      if ((players != null) && !players.isEmpty()) {
        sb.append(" (");
        sb.append(Joiner.on(", ").join(players));
        sb.append(")");
      }
      super.getListCellRendererComponent(list, sb.toString(), index, isSelected, cellHasFocus);
    }
    return this;
  }

  private void setIconMap() {
    final PlayerManager playerManager = game.getPlayerManager();
    PlayerList playerList;
    game.getData().acquireReadLock();
    try {
      playerList = game.getData().getPlayerList();
    } finally {
      game.getData().releaseReadLock();
    }
    // new HashSet removes duplicates
    for (final INode playerNode : new HashSet<>(playerManager.getPlayerMapping().values())) {
      final Set<String> players = playerManager.getPlayedBy(playerNode);
      if (players.size() > 0) {
        final List<Icon> icons = new ArrayList<>(players.size());
        for (final String player : players) {
          if ((uiContext != null) && (uiContext.getFlagImageFactory() != null)) {
            icons.add(new ImageIcon(uiContext.getFlagImageFactory().getSmallFlag(playerList.getPlayerId(player))));
          }
        }
        maxIconCounter = Math.max(maxIconCounter, icons.size());
        playerMap.put(playerNode.toString(), players);
        if (uiContext == null) {
          iconMap.put(playerNode.toString(), null);
        } else {
          iconMap.put(playerNode.toString(), icons);
        }
      }
    }
  }

  public int getMaxIconCounter() {
    return maxIconCounter;
  }
}
