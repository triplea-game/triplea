package games.strategy.triplea.ui.menubar;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.apple.eawt.Application;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.ui.SwingComponents;
import games.strategy.util.LocalizeHTML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.web.WebView;

public class HelpMenu {

  private final IUIContext iuiContext;
  private final GameData gameData;

  public HelpMenu(final MenuBar menuBar, final IUIContext iuiContext, final GameData gameData) {
    this.iuiContext = iuiContext;
    this.gameData = gameData;

    final Menu helpMenu = new Menu("_Help");
    menuBar.getMenus().add(helpMenu);

    addMoveHelpMenu(helpMenu);
    addUnitHelpMenu(helpMenu);

    addGameNotesMenu(helpMenu);

    addAboutMenu(helpMenu);

    addReportBugsMenu(helpMenu);
  }


  private void addMoveHelpMenu(final Menu helpMenu) {
    MenuItem moveHelp = new MenuItem("_Movement/Selection Help");
    moveHelp.setMnemonicParsing(true);
    moveHelp.setOnAction(e -> {
      // html formatted string
      final String hints = "<html><b> Selecting Units</b><br>" + "Left click on a unit stack to select 1 unit.<br>"
          + "ALT-Left click on a unit stack to select 10 units of that type in the stack.<br>"
          + "CTRL-Left click on a unit stack to select all units of that type in the stack.<br>"
          + "Shift-Left click on a unit to select all units in the territory.<br>"
          + "Left click on a territory but not on a unit to bring up a selection window for inputing the desired selection.<br>"
          + "<br><b> Deselecting Units</b><br>"
          + "Right click somewhere not on a unit stack to unselect the last selected unit.<br>"
          + "Right click on a unit stack to unselect one unit in the stack.<br>"
          + "ALT-Right click on a unit stack to unselect 10 units of that type in the stack.<br>"
          + "CTRL-Right click on a unit stack to unselect all units of that type in the stack.<br>"
          + "CTRL-Right click somewhere not on a unit stack to unselect all units selected.<br>"
          + "<br><b> Moving Units</b><br>"
          + "After selecting units Left click on a territory to move units there (do not Left click and Drag, instead select units, then move the mouse, then select the territory).<br>"
          + "CTRL-Left click on a territory to select the territory as a way point (this will force the units to move through this territory on their way to the destination).<br>"
          + "<br><b> Moving the Map Screen</b><br>"
          + "Right click and Drag the mouse to move your screen over the map.<br>"
          + "Left click the map (anywhere), use the arrow keys (or WASD keys) to move your map around. Holding down control will move the map faster.<br />"
          + "Left click in the Minimap at the top right of the screen, and Drag the mouse.<br>"
          + "Move the mouse to the edge of the map to scroll in that direction. Moving the mouse even closer to the edge will scroll faster.<br>"
          + "Scrolling the mouse wheel will move the map up and down.<br>" + "<br><b> Zooming Out</b><br>"
          + "Holding ALT while Scrolling the Mouse Wheel will zoom the map in and out.<br>"
          + "Select 'Zoom' from the 'View' menu, and change to the desired level.<br>"
          + "<br><b> Turn off Map Artwork</b><br>"
          + "Deselect 'Map Details' in the 'View' menu, to show a map without the artwork.<br>"
          + "Select a new 'Map Skin' from the 'View' menu to show a different kind of artwork (not all maps have skins).<br>"
          + "<br><b> Other Things</b><br>"
          + "Press 'n' to cycle through units with movement left (move phases only).<br>"
          + "Press 'f' to highlight all units you own that have movement left (move phases only).<br>"
          + "Press 'i' or 'v' to popup info on whatever territory and unit your mouse is currently over.<br>"
          + "Press 'u' while mousing over a unit to undo all moves that unit has made (beta).<br>"
          + "To list specific units from a territory in the Territory panel, drag and drop from the territory on the map to the territory panel.<br></html>";
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setTitle(moveHelp.getText());
      WebView webView = new WebView();
      webView.getEngine().loadContent(hints);
      alert.getDialogPane().setContent(webView);
      alert.show();
    });
  }

  protected static String getUnitStatsTable(final GameData gameData, final IUIContext iuiContext) {
    // html formatted string
    int i = 0;
    final String color1 = "ABABAB";
    final String color2 = "BDBDBD";
    final String color3 = "FEECE2";
    final StringBuilder hints = new StringBuilder();
    hints.append("<html>");
    try {
      gameData.acquireReadLock();
      final Map<PlayerID, Map<UnitType, ResourceCollection>> costs =
          BattleCalculator.getResourceCostsForTUV(gameData, true);
      final Map<PlayerID, List<UnitType>> playerUnitTypes =
          UnitType.getAllPlayerUnitsWithImages(gameData, iuiContext, true);
      for (final Map.Entry<PlayerID, List<UnitType>> entry : playerUnitTypes.entrySet()) {
        final PlayerID player = entry.getKey();
        hints.append("<p><table border=\"1\" bgcolor=\"" + color1 + "\">");
        hints.append("<tr><th style=\"font-size:120%;000000\" bgcolor=\"" + color3 + "\" colspan=\"4\">")
            .append(player == null ? "NULL" : player.getName()).append(" Units</th></tr>");
        hints.append("<tr").append(((i & 1) == 0) ? " bgcolor=\"" + color1 + "\"" : " bgcolor=\"" + color2 + "\"")
            .append("><td>Unit</td><td>Name</td><td>Cost</td><td>Tool Tip</td></tr>");
        for (final UnitType ut : entry.getValue()) {
          i++;
          hints.append("<tr").append(((i & 1) == 0) ? " bgcolor=\"" + color1 + "\"" : " bgcolor=\"" + color2 + "\"")
              .append(">").append("<td>").append(getUnitImageURL(ut, player, iuiContext)).append("</td>").append("<td>")
              .append(ut.getName()).append("</td>").append("<td>").append(costs.get(player).get(ut).toStringForHTML())
              .append("</td>").append("<td>").append(ut.getTooltip(player)).append("</td></tr>");
        }
        i++;
        hints.append("<tr").append(((i & 1) == 0) ? " bgcolor=\"" + color1 + "\"" : " bgcolor=\"" + color2 + "\"")
            .append(">").append("<td>Unit</td><td>Name</td><td>Cost</td><td>Tool Tip</td></tr></table></p><br />");
      }
    } finally {
      gameData.releaseReadLock();
    }
    hints.append("</html>");
    return hints.toString();
  }

  private static String getUnitImageURL(final UnitType unitType, final PlayerID player, final IUIContext iuiContext) {
    final UnitImageFactory unitImageFactory = iuiContext.getUnitImageFactory();
    if (player == null || unitImageFactory == null) {
      return "no image";
    }
    final Optional<URL> imageUrl = unitImageFactory.getBaseImageURL(unitType.getName(), player);
    final String imageLocation = imageUrl.isPresent() ? imageUrl.get().toString() : "";

    return "<img src=\"" + imageLocation + "\" border=\"0\"/>";
  }



  private void addUnitHelpMenu(final Menu parentMenu) {
    MenuItem unitHelp = new MenuItem("_Unit Help");
    unitHelp.setMnemonicParsing(true);
    unitHelp.setOnAction(e -> {
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setTitle(unitHelp.getText());
      WebView webView = new WebView();
      webView.getEngine().loadContent(getUnitStatsTable(gameData, iuiContext));
      ScrollPane scrollPane = new ScrollPane(webView);
      alert.getDialogPane().setContent(scrollPane);
      alert.show();
    });
  }


  public static final WebView gameNotesPane = new WebView();

  protected void addGameNotesMenu(final Menu helpMenu) {
    // allow the game developer to write notes that appear in the game
    // displays whatever is in the notes field in html
    final String notesProperty = gameData.getProperties().get("notes", "");
    if (notesProperty != null && notesProperty.trim().length() != 0) {
      final String notes = LocalizeHTML.localizeImgLinksInHTML(notesProperty.trim());
      MenuItem gameNotes = new MenuItem("Game _Notes");
      gameNotes.setMnemonicParsing(true);
      gameNotes.setOnAction(e -> {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(gameNotes.getText());
        gameNotesPane.getEngine().loadContent(notes);
        alert.getDialogPane().setContent(gameNotesPane);
        alert.show();
      });
      helpMenu.getItems().add(gameNotes);
    }
  }

  private void addAboutMenu(final Menu parentMenu) {
    final String text = "<h2>" + gameData.getGameName() + "</h2>" + "<p><b>Engine Version:</b> "
        + ClientContext.engineVersion() + "<br><b>Game:</b> " + gameData.getGameName()
        + "<br><b>Game Version:</b>" + gameData.getGameVersion() + "</p>"
        + "<p>For more information please visit,<br><br>"
        + "<b><a hlink='" + UrlConstants.TRIPLEA_WEBSITE + "'>" + UrlConstants.TRIPLEA_WEBSITE + "</a></b><br><br>";
    MenuItem aboutMenu = new MenuItem("_About");
    aboutMenu.setMnemonicParsing(true);
    aboutMenu.setOnAction(e -> {
      Alert alert = new Alert(AlertType.INFORMATION);
      alert.setTitle(aboutMenu.getText());
      gameNotesPane.getEngine().loadContent(text);
      alert.getDialogPane().setContent(gameNotesPane);
      alert.show();
    });
    if (System.getProperty("mrj.version") == null) {
      parentMenu.getItems().add(new SeparatorMenuItem());
      parentMenu.getItems().add(aboutMenu);
    } else {
      // On Mac OS X, put the About menu where Mac users expect it to be
      Application.getApplication().setAboutHandler(paramAboutEvent -> aboutMenu.getOnAction().handle(null));
    }
  }

  private void addReportBugsMenu(final Menu parentMenu) {
    MenuItem sendBugs = new MenuItem("Send _Bug Report");
    sendBugs.setMnemonicParsing(true);
    sendBugs.setOnAction(e -> {
      SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_ISSUES);// TODO change this
    });
  }

}
