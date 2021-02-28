package games.strategy.engine.chat;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;
import games.strategy.net.INode;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.ui.UiContext;
import java.awt.Component;
import java.util.Collection;
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
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;

/**
 * Renders a chat participant in a {@link JList}.
 *
 * <p>This implementation optimizes rendering by caching the status icons and player-to-node
 * mappings.
 */
public class PlayerChatRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = -8195565028281374498L;
  private final Map<String, List<Icon>> iconMap = new HashMap<>();
  private final Map<String, Set<String>> playerMap = new HashMap<>();

  public PlayerChatRenderer(final IGame game, final UiContext uiContext) {
    Preconditions.checkNotNull(game);
    Preconditions.checkNotNull(uiContext);
    final FlagIconImageFactory factory =
        Preconditions.checkNotNull(uiContext.getFlagImageFactory());

    final PlayerManager playerManager = game.getPlayerManager();
    final PlayerList playerList = getPlayerList(game);
    for (final INode playerNode : new HashSet<>(playerManager.getPlayerMapping().values())) {
      final Set<String> players = playerManager.getPlayedBy(playerNode);
      final List<Icon> icons =
          players.stream()
              .map(player -> new ImageIcon(factory.getSmallFlag(playerList.getPlayerId(player))))
              .collect(Collectors.toList());
      final String name = playerNode.getPlayerName().getValue();
      playerMap.put(name, players);
      iconMap.put(name, icons);
    }
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
        iconMap.getOrDefault(chatParticipant.getUserName().getValue(), List.of());
    if (icons.isEmpty()) {
      super.getListCellRendererComponent(
          list,
          getNodeLabelWithPlayers(chatParticipant.getUserName()),
          index,
          isSelected,
          cellHasFocus);
    } else {
      super.getListCellRendererComponent(
          list, chatParticipant.getUserName().getValue(), index, isSelected, cellHasFocus);
      setHorizontalTextPosition(SwingConstants.LEFT);
      setIcon(new CompositeIcon(icons));
    }
    return this;
  }

  private String getNodeLabelWithPlayers(final UserName userName) {
    final Set<String> playerNames = playerMap.getOrDefault(userName.getValue(), Set.of());
    return userName
        + (playerNames.isEmpty()
            ? ""
            : playerNames.stream().collect(Collectors.joining(", ", " (", ")")));
  }

  private static PlayerList getPlayerList(final IGame game) {
    game.getData().acquireReadLock();
    try {
      return game.getData().getPlayerList();
    } finally {
      game.getData().releaseReadLock();
    }
  }

  int getMaxIconCounter() {
    return iconMap.values().stream().mapToInt(Collection::size).max().orElse(0);
  }
}
