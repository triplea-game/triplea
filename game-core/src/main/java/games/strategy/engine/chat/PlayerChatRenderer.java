package games.strategy.engine.chat;

import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.lobby.PlayerName;
import games.strategy.net.INode;
import games.strategy.triplea.ui.UiContext;
import java.awt.Component;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingConstants;

/**
 * Renders a chat participant in a {@link JList}.
 *
 * <p>This implementation optimizes rendering by caching the status icons and player-to-node
 * mappings.
 */
public class PlayerChatRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = -8195565028281374498L;
  private final IGame game;
  private final UiContext uiContext;
  private int maxIconCounter = 0;
  private final Map<String, List<Icon>> iconMap = new HashMap<>();
  private final Map<String, Set<String>> playerMap = new HashMap<>();

  public PlayerChatRenderer(final IGame game, final UiContext uiContext) {
    this.game = game;
    this.uiContext = uiContext;
    setIconMap();
  }

  @Override
  public Component getListCellRendererComponent(
      final JList<?> list,
      final Object value,
      final int index,
      final boolean isSelected,
      final boolean cellHasFocus) {
    final ChatParticipant chatParticipant = (ChatParticipant) value;
    final List<Icon> icons =
        iconMap.getOrDefault(chatParticipant.getPlayerName().getValue(), Collections.emptyList());
    if (icons.isEmpty()) {
      super.getListCellRendererComponent(
          list,
          getNodeLabelWithPlayers(chatParticipant.getPlayerName()),
          index,
          isSelected,
          cellHasFocus);
    } else {
      super.getListCellRendererComponent(
          list, chatParticipant.getPlayerName().getValue(), index, isSelected, cellHasFocus);
      setHorizontalTextPosition(SwingConstants.LEFT);
      setIcon(new CompositeIcon(icons));
    }
    return this;
  }

  private String getNodeLabelWithPlayers(final PlayerName playerName) {
    final Set<String> playerNames =
        playerMap.getOrDefault(playerName.getValue(), Collections.emptySet());
    return playerName
        + (playerNames.isEmpty()
            ? ""
            : playerNames.stream().collect(Collectors.joining(", ", " (", ")")));
  }

  private void setIconMap() {
    final PlayerManager playerManager = game.getPlayerManager();
    final PlayerList playerList;
    game.getData().acquireReadLock();
    try {
      playerList = game.getData().getPlayerList();
    } finally {
      game.getData().releaseReadLock();
    }
    if (uiContext == null || uiContext.getFlagImageFactory() == null) {
      return;
    }
    for (final INode playerNode : new HashSet<>(playerManager.getPlayerMapping().values())) {
      final Set<String> players = playerManager.getPlayedBy(playerNode);
      final List<Icon> icons =
          players.stream()
              .map(
                  player ->
                      new ImageIcon(
                          uiContext
                              .getFlagImageFactory()
                              .getSmallFlag(playerList.getPlayerId(player))))
              .collect(Collectors.toList());
      maxIconCounter = Math.max(maxIconCounter, icons.size());
      playerMap.put(playerNode.getPlayerName().getValue(), players);
      iconMap.put(playerNode.getPlayerName().getValue(), icons);
    }
  }

  int getMaxIconCounter() {
    return maxIconCounter;
  }
}
