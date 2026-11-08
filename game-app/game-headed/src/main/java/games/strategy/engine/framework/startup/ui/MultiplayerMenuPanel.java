package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.HtmlUtils;
import games.strategy.engine.framework.I18nEngineFramework;
import games.strategy.engine.framework.I18nResourceBundle;
import games.strategy.engine.framework.startup.ui.panels.main.HeadedServerSetupModel;
import java.util.List;
import javax.swing.JButton;
import org.triplea.swing.JButtonBuilder;

/** Chooses how to play against other people: lobby, email, forum, or a direct connection. */
public class MultiplayerMenuPanel extends MenuPanel {
  private static final long serialVersionUID = 6197521473418529376L;

  public MultiplayerMenuPanel(final HeadedServerSetupModel model) {
    super(
        List.of(
            new JButtonBuilder(
                    I18nEngineFramework.get().getText("startup.SetupPanelModel.btn.PlayOnline.Lbl"))
                .toolTipText(
                    HtmlUtils.getHtml(
                        I18nEngineFramework.get()
                            .getText("startup.SetupPanelModel.btn.PlayOnline.Tltp")))
                .actionListener(model::login)
                .build()),
        buildSecondaryButtons(model));
  }

  private static List<JButton> buildSecondaryButtons(final HeadedServerSetupModel model) {
    final I18nResourceBundle bundle = I18nEngineFramework.get();
    return List.of(
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.PlayByForum.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(bundle.getText("startup.SetupPanelModel.btn.PlayByForum.Tltp")))
            .actionListener(model::showPbf)
            .build(),
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.PlayByEmail.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(bundle.getText("startup.SetupPanelModel.btn.PlayByEmail.Tltp")))
            .actionListener(model::showPbem)
            .build(),
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.HostNetworkGame.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(
                    bundle.getText("startup.SetupPanelModel.btn.PlayOnline.HostNetworkGame.Tltp")))
            .actionListener(() -> new Thread(model::showServer).start())
            .build(),
        new JButtonBuilder(bundle.getText("startup.SetupPanelModel.btn.ConnectToNetworkedGame.Lbl"))
            .toolTipText(
                HtmlUtils.getHtml(
                    bundle.getText(
                        "startup.SetupPanelModel.btn.PlayOnline.ConnectToNetworkedGame.Tltp")))
            .actionListener(() -> new Thread(model::showClient).start())
            .build());
  }
}
