package games.strategy.triplea.ui.menubar;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.network.ui.BanPlayerAction;
import games.strategy.engine.framework.network.ui.BootPlayerAction;
import games.strategy.engine.framework.network.ui.SetPasswordAction;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.net.IServerMessenger;
import games.strategy.triplea.ui.PlayersPanel;
import games.strategy.triplea.ui.TripleAFrame;
import java.util.Optional;
import javax.swing.JMenu;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.key.binding.KeyCode;

final class NetworkMenu {

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public enum Mnemonic {
    SHOW_WHO_IS_WHO(KeyCode.W),
    REMOVE_PLAYER(KeyCode.R),
    BAN_PLAYER(KeyCode.B),
    SET_PASSWORD(KeyCode.P);

    private final KeyCode mnemonicCode;

    public int getValue() {
      return mnemonicCode.getInputEventCode();
    }
  }

  public static JMenu get(
      final Optional<InGameLobbyWatcherWrapper> watcher, final TripleAFrame frame) {
    final IGame game = frame.getGame();
    final boolean isServer = game.getMessengers().isServer();
    return new JMenuBuilder("Network", TripleAMenuBar.Mnemonic.NETWORK.getMnemonicCode())
        .addMenuItemIf(
            isServer,
            new JMenuItemBuilder(
                    new BootPlayerAction(frame, getServerMessenger(game)),
                    Mnemonic.REMOVE_PLAYER.getMnemonicCode())
                .build())
        .addMenuItemIf(
            isServer,
            new JMenuItemBuilder(
                    new BanPlayerAction(frame, getServerMessenger(game)),
                    Mnemonic.BAN_PLAYER.getMnemonicCode())
                .build())
        .addMenuItemIf(
            watcher.isPresent(),
            new JMenuItemBuilder(
                    new SetPasswordAction(
                        frame,
                        watcher.orElse(null),
                        (ClientLoginValidator) getServerMessenger(game).getLoginValidator()),
                    Mnemonic.SET_PASSWORD.getMnemonicCode())
                .build())
        .addMenuItemIf(
            !game.getData().getProperties().getEditableProperties().isEmpty(),
            new JMenuItemBuilder("Show Who is Who", Mnemonic.SHOW_WHO_IS_WHO.getMnemonicCode())
                .actionListener(() -> PlayersPanel.showPlayers(game, frame)))
        .build();
  }

  private static IServerMessenger getServerMessenger(final IGame game) {
    return game.getMessengers().getServerMessenger();
  }
}
