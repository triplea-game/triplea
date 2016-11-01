package games.strategy.triplea.ui.menubar;

import games.strategy.triplea.UrlConstants;
import games.strategy.ui.SwingComponents;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class WebHelpMenu {
  public WebHelpMenu(final TripleAMenuBar menuBar) {
    final Menu web = new Menu("_Web");
    menuBar.getMenus().add(web);
    addWebMenu(web);
  }

  private static void addWebMenu(final Menu parentMenu) {
    final MenuItem hostingLink = new MenuItem("How to _Host");
    hostingLink.setMnemonicParsing(true);
    hostingLink.setOnAction(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_HOSTING_MAPS));
    parentMenu.getItems().add(hostingLink);

    final MenuItem lobbyRules = new MenuItem("_Lobby Rules");
    lobbyRules.setMnemonicParsing(true);
    lobbyRules.setOnAction(
        e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB_LOBBY_RULES));
    parentMenu.getItems().add(lobbyRules);

    final MenuItem warClub = new MenuItem("_War Club & Ladder");
    warClub.setMnemonicParsing(true);
    warClub.setOnAction(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.TRIPLEA_WAR_CLUB));
    parentMenu.getItems().add(warClub);

    final MenuItem donateLink = new MenuItem("_Donate");
    donateLink.setMnemonicParsing(true);
    donateLink.setOnAction(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.PAYPAL_DONATE));
    parentMenu.getItems().add(donateLink);

    final MenuItem helpLink = new MenuItem("H_elp");
    helpLink.setMnemonicParsing(true);
    helpLink.setOnAction(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.WEBSITE_HELP));
    parentMenu.getItems().add(helpLink);

    final MenuItem ruleBookLink = new MenuItem("_Rule Book");
    ruleBookLink.setMnemonicParsing(true);
    ruleBookLink.setOnAction(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.RULE_BOOK));
    parentMenu.getItems().add(ruleBookLink);
  }
}
