package games.strategy.triplea.ui.menubar;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import games.strategy.triplea.UrlConstants;
import games.strategy.ui.SwingComponents;

public class WebHelpMenu {
  public WebHelpMenu(final TripleAMenuBar menuBar) {
    final JMenu web = new JMenu("Web");
    web.setMnemonic(KeyEvent.VK_W);
    menuBar.add(web);
    addWebMenu(web);
  }

  private static void addWebMenu(final JMenu parentMenu) {
    final JMenuItem hostingLink = new JMenuItem("How to Host");
    hostingLink.setMnemonic(KeyEvent.VK_H);
    hostingLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_HELP));
    parentMenu.add(hostingLink);

    final JMenuItem lobbyRules = new JMenuItem("Lobby Rules");
    lobbyRules.setMnemonic(KeyEvent.VK_L);
    lobbyRules.addActionListener(
        e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB_LOBBY_RULES));
    parentMenu.add(lobbyRules);

    final JMenuItem warClub = new JMenuItem("War Club & Ladder");
    warClub.setMnemonic(KeyEvent.VK_W);
    warClub.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB));
    parentMenu.add(warClub);

    final JMenuItem donateLink = new JMenuItem("Donate");
    donateLink.setMnemonic(KeyEvent.VK_O);
    donateLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.PAYPAL_DONATE));
    parentMenu.add(donateLink);

    final JMenuItem helpLink = new JMenuItem("Help");
    helpLink.setMnemonic(KeyEvent.VK_G);
    helpLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_HELP));
    parentMenu.add(helpLink);

    final JMenuItem ruleBookLink = new JMenuItem("Rule Book");
    ruleBookLink.setMnemonic(KeyEvent.VK_K);
    ruleBookLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.RULE_BOOK));
    parentMenu.add(ruleBookLink);
  }
}
