package games.strategy.engine.framework.startup.ui.panels.main;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.HtmlUtils;
import games.strategy.engine.framework.I18nEngineFramework;
import games.strategy.engine.framework.startup.ui.SetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import org.triplea.game.chat.ChatModel;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * When the game launches, the MainFrame is loaded which will contain the MainPanel. The contents of
 * the MainPanel are swapped out until a new game has been started (TODO: check if the lobby uses
 * MainPanel at all).
 */
public class MainPanel extends JPanel {
  private static final long serialVersionUID = -5548760379892913464L;
  private static final Dimension initialSize = new Dimension(900, 780);

  private final JButton playButton =
      new JButtonBuilder()
          .title(I18nEngineFramework.get().getText("startup.MainPanel.btn.Play.Lbl"))
          .toolTip(
              HtmlUtils.getHtml()
                  .addText(
                      I18nEngineFramework.get().getText("startup.MainPanel.btn.Play.Tltp.line1"))
                  .lineBreak()
                  .addText(
                      I18nEngineFramework.get().getText("startup.MainPanel.btn.Play.Tltp.line2"))
                  .lineBreak()
                  .addText(
                      I18nEngineFramework.get().getText("startup.MainPanel.btn.Play.Tltp.line3"))
                  .toString())
          .build();
  private final JButton cancelButton =
      new JButtonBuilder()
          .title(I18nEngineFramework.get().getText("startup.MainPanel.btn.Cancel.Lbl"))
          .build();

  private final JPanel gameSetupPanelHolder = new JPanelBuilder().borderLayout().build();
  private final JPanel mainPanel;
  private final JSplitPane chatSplit;
  private final JPanel chatPanelHolder = new JPanelBuilder().height(62).borderLayout().build();
  private SetupPanel gameSetupPanel;
  private final GameSelectorPanel gameSelectorPanel;

  /**
   * MainPanel is the full contents of the 'mainFrame'. This panel represents the welcome screen and
   * subsequent screens.
   */
  MainPanel(
      final Runnable quitAction,
      final GameSelectorPanel gameSelectorPanel,
      final Consumer<MainPanel> launchAction,
      @Nullable final ChatModel chatModel,
      final Runnable cancelAction) {
    this.gameSelectorPanel = gameSelectorPanel;
    playButton.addActionListener(e -> launchAction.accept(this));
    cancelButton.addActionListener(e -> cancelAction.run());

    gameSelectorPanel.setBorder(new EtchedBorder());
    final JScrollPane gameSetupPanelScroll = new JScrollPane(gameSetupPanelHolder);
    gameSetupPanelScroll.setBorder(BorderFactory.createEmptyBorder());
    chatSplit = new JSplitPane();
    chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    chatSplit.setResizeWeight(0.8);
    chatSplit.setOneTouchExpandable(false);
    chatSplit.setDividerSize(5);

    mainPanel = new JPanelBuilder().border(0).gridBagLayout().build();
    mainPanel.add(
        gameSelectorPanel,
        new GridBagConstraintsBuilder(0, 0)
            .anchor(GridBagConstraintsAnchor.WEST)
            .fill(GridBagConstraintsFill.VERTICAL)
            .build());

    mainPanel.add(
        gameSetupPanelScroll,
        new GridBagConstraintsBuilder(1, 0)
            .anchor(GridBagConstraintsAnchor.CENTER)
            .fill(GridBagConstraintsFill.BOTH)
            .weightX(1.0)
            .weightY(1.0)
            .build());
    setLayout(new BorderLayout());

    if (chatModel instanceof ChatPanel) {
      addChat((ChatPanel) chatModel);
    } else {
      add(mainPanel, BorderLayout.CENTER);
    }

    final JButton quitButton =
        new JButtonBuilder()
            .title(I18nEngineFramework.get().getText("startup.MainPanel.btn.Quit.Lbl"))
            .toolTip(I18nEngineFramework.get().getText("startup.MainPanel.btn.Quit.Tltp"))
            .actionListener(quitAction)
            .build();
    final JPanel buttonsPanel =
        new JPanelBuilder().borderEtched().add(playButton).add(quitButton).build();
    add(buttonsPanel, BorderLayout.SOUTH);
    setPreferredSize(initialSize);
    updatePlayButtonState();
  }

  public void loadSaveFile(final Path file) {
    gameSelectorPanel.loadSaveFile(file);
  }

  private void addChat(final Component chatComponent) {
    remove(mainPanel);
    remove(chatSplit);
    chatPanelHolder.removeAll();

    chatPanelHolder.add(chatComponent, BorderLayout.CENTER);
    chatSplit.setTopComponent(mainPanel);
    chatSplit.setBottomComponent(chatPanelHolder);
    add(chatSplit, BorderLayout.CENTER);
  }

  /** This method will 'change' screens, swapping out one setup panel for another. */
  public void setSetupPanel(final SetupPanel panel) {
    if (gameSetupPanel != null) {
      gameSetupPanel.setPanelChangedListener(null);
    }
    gameSetupPanel = panel;
    // Note: The listener on the panel itself only updates widgets and doesn't
    // rebuild the UI components (this method), as that would cause focus to be lost.
    panel.setPanelChangedListener(setupPanel -> updatePlayButtonState());
    updatePlayButtonState();

    gameSetupPanelHolder.removeAll();
    gameSetupPanelHolder.add(panel, BorderLayout.CENTER);
    // add the cancel button if we are not choosing the type.
    if (panel.isCancelButtonVisible()) {
      final JPanel cancelPanel = new JPanel();
      cancelPanel.setBorder(new EmptyBorder(10, 0, 10, 10));
      cancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
      final List<Action> actions = gameSetupPanel.getUserActions();
      if (!actions.isEmpty()) {
        createUserActionMenu(cancelPanel, actions);
      }
      cancelPanel.add(cancelButton);
      gameSetupPanelHolder.add(cancelPanel, BorderLayout.SOUTH);
    }

    Optional.ofNullable(panel.getChatModel())
        .filter(ChatPanel.class::isInstance)
        .map(ChatPanel.class::cast)
        .ifPresentOrElse(
            this::addChat,
            () -> {
              remove(chatSplit);
              add(mainPanel);
            });

    revalidate();
  }

  private static void createUserActionMenu(final JPanel cancelPanel, final List<Action> actions) {
    // if we need this for something other than network, add a way to set it
    final JButton button =
        new JButton(I18nEngineFramework.get().getText("startup.MainPanel.btn.Network.Lbl"));
    button.addActionListener(
        e -> {
          final JPopupMenu menu = new JPopupMenu();
          for (final Action a : actions) {
            menu.add(a);
          }
          menu.show(button, 0, button.getHeight());
        });
    cancelPanel.add(button);
  }

  public void updatePlayButtonState() {
    SwingAction.invokeNowOrLater(
        () -> playButton.setEnabled(gameSetupPanel != null && gameSetupPanel.canGameStart()));
  }
}
