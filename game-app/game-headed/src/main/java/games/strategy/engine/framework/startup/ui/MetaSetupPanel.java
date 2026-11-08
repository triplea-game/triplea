package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.HtmlUtils;
import games.strategy.engine.framework.I18nEngineFramework;
import games.strategy.engine.framework.I18nResourceBundle;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.ui.panels.main.HeadedServerSetupModel;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.Component;
import java.awt.Frame;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingComponents;
import tools.map.making.ui.MapCreator;

/** The welcome screen: choose local or multiplayer play, or open a tool. */
public class MetaSetupPanel extends MenuPanel {
  private static final long serialVersionUID = 3926503672972937677L;

  public MetaSetupPanel(final HeadedServerSetupModel model) {
    super(buildPrimaryButtons(model), buildSecondaryButtons());
  }

  private static List<JButton> buildPrimaryButtons(final HeadedServerSetupModel model) {
    final I18nResourceBundle bundle = I18nEngineFramework.get();
    return List.of(
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.StartLocalGame.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(
                    bundle.getText("startup.SetupPanelModel.btn.StartLocalGame.Tltp")))
            .actionListener(model::showLocal)
            .build(),
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.Multiplayer.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(bundle.getText("startup.SetupPanelModel.btn.Multiplayer.Tltp")))
            .actionListener(model::showMultiplayer)
            .build());
  }

  private static List<JButton> buildSecondaryButtons() {
    final I18nResourceBundle bundle = I18nEngineFramework.get();
    return List.of(
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.DownloadMaps.Lbl"))
            .actionListener(DownloadMapsWindow::showDownloadMapsWindow)
            .build(),
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.EnginePreferences.Lbl"))
            .toolTipText(bundle.getText("startup.SetupPanelModel.btn.EnginePreferences.Tltp"))
            .actionListener(c -> ClientSetting.showSettingsWindow(frameOf(c)))
            .build(),
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.MapCreatorTools.Lbl"))
            .actionListener(MapCreator::openMapCreatorWindow)
            .build(),
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.UserGuideHelp.Lbl"))
            .actionListener(
                c ->
                    SwingComponents.newOpenUrlConfirmationDialog(
                        frameOf(c), UrlConstants.USER_GUIDE))
            .build());
  }

  private static Frame frameOf(final Component component) {
    return JOptionPane.getFrameForComponent(component);
  }
}
