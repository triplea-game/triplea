package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.HtmlUtils;
import games.strategy.engine.framework.I18nEngineFramework;
import games.strategy.engine.framework.I18nResourceBundle;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.ui.panels.main.HeadedServerSetupModel;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Optional;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;
import tools.map.making.ui.MapCreator;

/**
 * This is the main welcome panel with 'play online' button. This panel is just the upper right of
 * the main screen, it does not include the map information nor the 'play' and 'quit' buttons.
 */
public class MetaSetupPanel extends SetupPanel {

  private static final long serialVersionUID = 3926503672972937677L;

  public MetaSetupPanel(final HeadedServerSetupModel model) {
    final I18nResourceBundle bundle = I18nEngineFramework.get();
    final JButton connectToLobby =
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.PlayOnline.Lbl"))
            .biggerFont()
            .toolTipText(
                HtmlUtils.getHtml(bundle.getText("startup.SetupPanelModel.btn.PlayOnline.Tltp")))
            .actionListener(model::login)
            .build();
    final JButton startLocal =
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.StartLocalGame.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(
                    bundle.getText("startup.SetupPanelModel.btn.StartLocalGame.Tltp")))
            .actionListener(model::showLocal)
            .build();

    final JButton startPbf =
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.PlayByForum.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(bundle.getText("startup.SetupPanelModel.btn.PlayByForum.Tltp")))
            .actionListener(model::showPbf)
            .build();
    final JButton startPbem =
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.PlayByEmail.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(bundle.getText("startup.SetupPanelModel.btn.PlayByEmail.Tltp")))
            .actionListener(model::showPbem)
            .build();
    final JButton hostGame =
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.HostNetworkGame.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(
                    bundle.getText("startup.SetupPanelModel.btn.PlayOnline.HostNetworkGame.Tltp")))
            .actionListener(() -> new Thread(model::showServer).start())
            .build();
    final JButton connectToHostedGame =
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.ConnectToNetworkedGame.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(
                    bundle.getText(
                        "startup.SetupPanelModel.btn.PlayOnline.ConnectToNetworkedGame.Tltp")))
            .actionListener(() -> new Thread(model::showClient).start())
            .build();
    final JButton enginePreferences =
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.EnginePreferences.Lbl"))
            .toolTipText(bundle.getText("startup.SetupPanelModel.btn.EnginePreferences.Tltp"))
            .actionListener(
                () -> ClientSetting.showSettingsWindow(JOptionPane.getFrameForComponent(this)))
            .build();
    final JButton userGuideButton =
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.UserGuideHelp.Lbl"))
            .actionListener(
                () -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.USER_GUIDE))
            .build();
    final JButton mapCreator =
        new JButtonBuilder()
            .title(bundle.getText("startup.SetupPanelModel.btn.MapCreatorTools.Lbl"))
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
  public void cancel() { // nothing to do
  }

  @Override
  public Optional<ILauncher> getLauncher() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void postStartGame() {
    // nothing to do
  }
}
