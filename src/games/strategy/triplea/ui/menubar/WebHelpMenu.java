package games.strategy.triplea.ui.menubar;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.ui.SwingComponents;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.awt.event.KeyEvent;

public class WebHelpMenu {
  public WebHelpMenu(TripleAMenuBar menuBar) {
    final JMenu web = new JMenu("Web");
    web.setMnemonic(KeyEvent.VK_W);
    menuBar.add(web);
    addWebMenu(web);
  }

  private static void addWebMenu(final JMenu parentMenu) {
    final JMenuItem hostingLink = new JMenuItem("How to Host...");
    hostingLink.setMnemonic(KeyEvent.VK_H);
    final JMenuItem mapLink = new JMenuItem("Install Maps...");
    mapLink.setMnemonic(KeyEvent.VK_I);
    final JMenuItem bugReport = new JMenuItem("Bug Report...");
    bugReport.setMnemonic(KeyEvent.VK_B);
    final JMenuItem lobbyRules = new JMenuItem("Lobby Rules...");
    lobbyRules.setMnemonic(KeyEvent.VK_L);
    final JMenuItem warClub = new JMenuItem("War Club & Ladder...");
    warClub.setMnemonic(KeyEvent.VK_W);
    final JMenuItem devForum = new JMenuItem("Developer Forum...");
    devForum.setMnemonic(KeyEvent.VK_E);
    final JMenuItem donateLink = new JMenuItem("Donate...");
    donateLink.setMnemonic(KeyEvent.VK_O);
    final JMenuItem helpLink = new JMenuItem("Help...");
    helpLink.setMnemonic(KeyEvent.VK_G);
    final JMenuItem ruleBookLink = new JMenuItem("Rule Book...");
    ruleBookLink.setMnemonic(KeyEvent.VK_K);

    hostingLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_HOSTING_MAPS));
    mapLink.addActionListener(e  -> SwingComponents.newOpenUrlConfirmationDialog( UrlConstants.SF_HOSTING_MAPS));
    bugReport.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_TICKET_LIST));
    lobbyRules.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB_LOBBY_RULES ));
    warClub.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB));
    devForum.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_FORUM));
    donateLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.PAYPAL_DONATE));
    helpLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.WEBSITE_HELP));
    ruleBookLink.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.RULE_BOOK));

    parentMenu.add(hostingLink);
    parentMenu.add(mapLink);
    parentMenu.add(bugReport);
    parentMenu.add(lobbyRules);
    parentMenu.add(warClub);
    parentMenu.add(devForum);
    parentMenu.add(donateLink);
    parentMenu.add(helpLink);
    parentMenu.add(ruleBookLink);
  }


}
