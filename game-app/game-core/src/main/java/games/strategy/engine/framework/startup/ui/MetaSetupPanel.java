package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.ui.panels.main.SetupPanelModel;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;
import tools.map.making.MapCreator;

/**
 * This is the main welcome panel with 'play online' button. This panel is just the upper right of
 * the main screen, it does not include the map information nor the 'play' and 'quit' buttons.
 */
public class MetaSetupPanel extends SetupPanel {

  private static final long serialVersionUID = 3926503672972937677L;

  public MetaSetupPanel(final SetupPanelModel model) {
    final ResourceBundle resourceBundle =
        ResourceBundle.getBundle("i18n.games.strategy.engine.framework.ui");
    final JButton connectToLobby =
        new JButtonBuilder(
                resourceBundle.getString("startup.SetupPanelModel.button.PlayOnline.Label"))
            .biggerFont() // startup.SetupPanelModel.button.StartLocalGame.Tooltip.Line1
            .toolTipText(
                getHTML(
                    new String[] {
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.PlayOnline.Tooltip.Line1"),
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.PlayOnline.Tooltip.Line2"),
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.PlayOnline.Tooltip.Line3")
                    }))
            .actionListener(model::login)
            .build();
    final JButton startLocal =
        new JButtonBuilder(
                resourceBundle.getString("startup.SetupPanelModel.button.StartLocalGame.Label"))
            .toolTipText(
                getHTML(
                    new String[] {
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.StartLocalGame.Tooltip.Line1"),
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.StartLocalGame.Tooltip.Line2"),
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.StartLocalGame.Tooltip.Line3")
                    }))
            .actionListener(model::showLocal)
            .build();

    final JButton startPbf =
        new JButtonBuilder(
                resourceBundle.getString("startup.SetupPanelModel.button.PlayByForum.Label"))
            .toolTipText(
                getHTML(
                    resourceBundle.getString("startup.SetupPanelModel.button.PlayByForum.Tooltip")))
            .actionListener(model::showPbf)
            .build();
    final JButton startPbem =
        new JButtonBuilder(
                resourceBundle.getString("startup.SetupPanelModel.button.PlayByEmail.Label"))
            .toolTipText(
                getHTML(
                    resourceBundle.getString("startup.SetupPanelModel.button.PlayByEmail.Tooltip")))
            .actionListener(model::showPbem)
            .build();
    final JButton hostGame =
        new JButtonBuilder(
                resourceBundle.getString("startup.SetupPanelModel.button.HostNetworkGame.Label"))
            .toolTipText(
                getHTML(
                    new String[] {
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.PlayOnline.HostNetworkGame.Tooltip.Line1"),
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.PlayOnline.HostNetworkGame.Tooltip.Line2"),
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.PlayOnline.HostNetworkGame.Tooltip.Line3")
                    }))
            .actionListener(() -> new Thread(model::showServer).start())
            .build();
    final JButton connectToHostedGame =
        new JButtonBuilder(
                resourceBundle.getString(
                    "startup.SetupPanelModel.button.ConnectToNetworkedGame.Label"))
            .toolTipText(
                getHTML(
                    new String[] {
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.PlayOnline.ConnectToNetworkedGame.Tooltip.Line1"),
                      resourceBundle.getString(
                          "startup.SetupPanelModel.button.PlayOnline.ConnectToNetworkedGame.Tooltip.Line2")
                    }))
            .actionListener(() -> new Thread(model::showClient).start())
            .build();
    final JButton enginePreferences =
        new JButtonBuilder(
                resourceBundle.getString("startup.SetupPanelModel.button.EnginePreferences.Label"))
            .toolTipText(
                resourceBundle.getString(
                    "startup.SetupPanelModel.button.EnginePreferences.Tooltip"))
            .actionListener(
                () -> ClientSetting.showSettingsWindow(JOptionPane.getFrameForComponent(this)))
            .build();
    final JButton userGuideButton =
        new JButtonBuilder(
                resourceBundle.getString("startup.SetupPanelModel.button.UserGuideHelp.Label"))
            .actionListener(
                () -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.USER_GUIDE))
            .build();
    final JButton mapCreator =
        new JButtonBuilder()
            .title(resourceBundle.getString("startup.SetupPanelModel.button.MapCreatorTools.Label"))
            .actionListener(MapCreator::openMapCreatorWindow)
            .build();

    setLayout(new BorderLayout());
    final JPanel mainContents = new JPanel();
    add(mainContents);
    mainContents.setLayout(new GridBagLayout());
    addButtonsToPanel(
        mainContents,
        new JButton[] {
          connectToLobby,
          startLocal,
          startPbf,
          startPbem,
          hostGame,
          connectToHostedGame,
          enginePreferences,
          mapCreator,
          userGuideButton
        });
  }

  private static String getHTML(final String[] lines) {

    return getHTML(String.join("<br/>", lines));
  }

  private static String getHTML(final String line) {
    return "<html>" + line + "</html>";
  }

  private void addButtonsToPanel(final JPanel panel, final JButton[] buttons) {
    for (int row = 0; row < buttons.length; row++) {
      panel.add(buttons[row], buildConstraintForRow(row));
    }
  }

  private GridBagConstraints buildConstraintForRow(final int rowNumber) {
    return new GridBagConstraintsBuilder(0, rowNumber)
        .anchor(GridBagConstraintsAnchor.CENTER)
        .fill(GridBagConstraintsFill.NONE)
        .insets(new Insets(10, 0, 0, 0))
        .build();
  }

  @Override
  public boolean canGameStart() {
    return false;
  }

  @Override
  public List<Action> getUserActions() {
    return List.of();
  }

  @Override
  public boolean isCancelButtonVisible() {
    return false;
  }

  @Override
  public void cancel() {}

  @Override
  public Optional<ILauncher> getLauncher() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void postStartGame() {}
}
