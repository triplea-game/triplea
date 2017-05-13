package games.strategy.triplea.ui.menubar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

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
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.LocalizeHTML;

public class HelpMenu {

  private final IUIContext iuiContext;
  private final GameData gameData;

  HelpMenu(final JMenuBar menuBar, final IUIContext iuiContext, final GameData gameData, final Color backgroundColor) {
    this.iuiContext = iuiContext;
    this.gameData = gameData;

    final JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);
    menuBar.add(helpMenu);

    addMoveHelpMenu(helpMenu);
    addUnitHelpMenu(helpMenu);

    addGameNotesMenu(helpMenu);

    addAboutMenu(helpMenu, backgroundColor);

    addReportBugsMenu(helpMenu);
  }


  private static void addMoveHelpMenu(final JMenu parentMenu) {
    final String moveSelectionHelpTitle = "Movement/Selection Help";
    parentMenu.add(SwingAction.of(moveSelectionHelpTitle, e -> {
      // html formatted string
      final String hints = "<b> Selecting Units</b><br>" + "Left click on a unit stack to select 1 unit.<br>"
          + "ALT-Left click on a unit stack to select 10 units of that type in the stack.<br>"
          + "CTRL-Left click on a unit stack to select all units of that type in the stack.<br>"
          + "Shift-Left click on a unit to select all units in the territory.<br>"
          + "Left click on a territory but not on a unit to bring up a selection window for inputing the desired "
          + "selection.<br>"
          + "<br><b> Deselecting Units</b><br>"
          + "Right click somewhere not on a unit stack to unselect the last selected unit.<br>"
          + "Right click on a unit stack to unselect one unit in the stack.<br>"
          + "ALT-Right click on a unit stack to unselect 10 units of that type in the stack.<br>"
          + "CTRL-Right click on a unit stack to unselect all units of that type in the stack.<br>"
          + "CTRL-Right click somewhere not on a unit stack to unselect all units selected.<br>"
          + "<br><b> Moving Units</b><br>"
          + "After selecting units Left click on a territory to move units there (do not Left click and Drag, instead "
          + "select units, then move the mouse, then select the territory).<br>"
          + "CTRL-Left click on a territory to select the territory as a way point (this will force the units to move "
          + "through this territory on their way to the destination).<br>"
          + "<br><b> Moving the Map Screen</b><br>"
          + "Right click and Drag the mouse to move your screen over the map.<br>"
          + "Left click the map (anywhere), use the arrow keys (or WASD keys) to move your map around. Holding down "
          + "control will move the map faster.<br />"
          + "Left click in the Minimap at the top right of the screen, and Drag the mouse.<br>"
          + "Move the mouse to the edge of the map to scroll in that direction. Moving the mouse even closer to the "
          + "edge will scroll faster.<br>"
          + "Scrolling the mouse wheel will move the map up and down.<br>" + "<br><b> Zooming Out</b><br>"
          + "Holding ALT while Scrolling the Mouse Wheel will zoom the map in and out.<br>"
          + "Select 'Zoom' from the 'View' menu, and change to the desired level.<br>"
          + "<br><b> Turn off Map Artwork</b><br>"
          + "Deselect 'Map Details' in the 'View' menu, to show a map without the artwork.<br>"
          + "Select a new 'Map Skin' from the 'View' menu to show a different kind of artwork (not all maps have "
          + "skins).<br>"
          + "<br><b> Other Things</b><br>"
          + "Press 'n' to cycle through units with movement left (move phases only).<br>"
          + "Press 'f' to highlight all units you own that have movement left (move phases only).<br>"
          + "Press 'i' or 'v' to popup info on whatever territory and unit your mouse is currently over.<br>"
          + "Press 'u' while mousing over a unit to undo all moves that unit has made (beta).<br>"
          + "To list specific units from a territory in the Territory panel, drag and drop from the territory on the "
          + "map to the territory panel.<br>";
      final JEditorPane editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorPane.setContentType("text/html");
      editorPane.setText(hints);
      final JScrollPane scroll = new JScrollPane(editorPane);
      JOptionPane.showMessageDialog(null, scroll, moveSelectionHelpTitle, JOptionPane.PLAIN_MESSAGE);
    })).setMnemonic(KeyEvent.VK_M);
  }

  static String getUnitStatsTable(final GameData gameData, final IUIContext iuiContext) {
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



  private void addUnitHelpMenu(final JMenu parentMenu) {
    final String unitHelpTitle = "Unit Help";
    parentMenu.add(SwingAction.of(unitHelpTitle, e -> {
      final JEditorPane editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorPane.setContentType("text/html");
      editorPane.setText(getUnitStatsTable(gameData, iuiContext));
      editorPane.setCaretPosition(0);
      final JScrollPane scroll = new JScrollPane(editorPane);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
      // not only do we have a start bar, but we also have the message dialog to account for just the scroll bars plus
      // the window sides
      final int availHeight = screenResolution.height - 120;
      final int availWidth = screenResolution.width - 40;
      scroll
          .setPreferredSize(new Dimension(
              (scroll.getPreferredSize().width > availWidth ? availWidth
                  : (scroll.getPreferredSize().height > availHeight
                      ? Math.min(availWidth, scroll.getPreferredSize().width + 22) : scroll.getPreferredSize().width)),
              (scroll.getPreferredSize().height > availHeight ? availHeight
                  : (scroll.getPreferredSize().width > availWidth
                      ? Math.min(availHeight, scroll.getPreferredSize().height + 22)
                      : scroll.getPreferredSize().height))));
      final JDialog dialog = SwingComponents.newJDialog(unitHelpTitle);
      dialog.add(scroll, BorderLayout.CENTER);
      final JPanel buttons = new JPanel();
      final JButton button = new JButton(SwingAction.of("OK", event -> {
        dialog.setVisible(false);
        dialog.removeAll();
        dialog.dispose();
      }));
      buttons.add(button);
      dialog.getRootPane().setDefaultButton(button);
      dialog.add(buttons, BorderLayout.SOUTH);
      dialog.pack();
      // dialog.setLocationRelativeTo(frame);
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowOpened(final WindowEvent e) {
          scroll.getVerticalScrollBar().getModel().setValue(0);
          scroll.getHorizontalScrollBar().getModel().setValue(0);
          button.requestFocus();
        }
      });
      dialog.setVisible(true);
      // dialog.dispose();
    })).setMnemonic(KeyEvent.VK_U);
  }


  public static final JEditorPane gameNotesPane = new JEditorPane();

  private void addGameNotesMenu(final JMenu parentMenu) {
    // allow the game developer to write notes that appear in the game
    // displays whatever is in the notes field in html
    final String notesProperty = gameData.getProperties().get("notes", "");
    if (notesProperty != null && notesProperty.trim().length() != 0) {
      final String notes = LocalizeHTML.localizeImgLinksInHTML(notesProperty.trim());
      gameNotesPane.setEditable(false);
      gameNotesPane.setContentType("text/html");
      gameNotesPane.setText(notes);
      final String gameNotesTitle = "Game Notes";
      parentMenu.add(SwingAction.of(gameNotesTitle, e -> SwingUtilities.invokeLater(() -> {
        final JScrollPane scroll = new JScrollPane(gameNotesPane);
        scroll.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
        final JDialog dialog = SwingComponents.newJDialog(gameNotesTitle);
        dialog.add(scroll, BorderLayout.CENTER);
        final JPanel buttons = new JPanel();
        final JButton button = new JButton(SwingAction.of("OK", event -> {
          dialog.setVisible(false);
          dialog.removeAll();
          dialog.dispose();
        }));
        buttons.add(button);
        dialog.getRootPane().setDefaultButton(button);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        if (dialog.getWidth() < 400) {
          dialog.setSize(400, dialog.getHeight());
        }
        if (dialog.getHeight() < 300) {
          dialog.setSize(dialog.getWidth(), 300);
        }
        if (dialog.getWidth() > 800) {
          dialog.setSize(800, dialog.getHeight());
        }
        if (dialog.getHeight() > 600) {
          dialog.setSize(dialog.getWidth(), 600);
        }
        // dialog.setLocationRelativeTo(frame);
        dialog.addWindowListener(new WindowAdapter() {
          @Override
          public void windowOpened(final WindowEvent e) {
            scroll.getVerticalScrollBar().getModel().setValue(0);
            scroll.getHorizontalScrollBar().getModel().setValue(0);
            button.requestFocus();
          }
        });
        dialog.setVisible(true);
      }))).setMnemonic(KeyEvent.VK_N);
    }
  }

  private void addAboutMenu(final JMenu parentMenu, final Color backgroundColor) {
    final String text = "<h2>" + gameData.getGameName() + "</h2>" + "<p><b>Engine Version:</b> "
        + ClientContext.engineVersion() + "<br><b>Game:</b> " + gameData.getGameName() + "<br><b>Game Version:</b> "
        + gameData.getGameVersion() + "</p>" + "<p>For more information please visit,<br><br>" + "<b><a hlink='"
        + UrlConstants.TRIPLEA_WEBSITE + "'>" + UrlConstants.TRIPLEA_WEBSITE + "</a></b><br><br>";
    final JEditorPane editorPane = new JEditorPane();
    editorPane.setBorder(null);
    editorPane.setBackground(backgroundColor);
    editorPane.setEditable(false);
    editorPane.setContentType("text/html");
    editorPane.setText(text);
    final JScrollPane scroll = new JScrollPane(editorPane);
    scroll.setBorder(null);
    if (System.getProperty("mrj.version") == null) {
      parentMenu.addSeparator();
      parentMenu.add(SwingAction.of("About", e -> JOptionPane.showMessageDialog(null, editorPane,
          "About " + gameData.getGameName(), JOptionPane.PLAIN_MESSAGE))).setMnemonic(KeyEvent.VK_A);
    } else { // On Mac OS X, put the About menu where Mac users expect it to be
      Application.getApplication().setAboutHandler(paramAboutEvent -> JOptionPane.showMessageDialog(null, editorPane,
          "About " + gameData.getGameName(), JOptionPane.PLAIN_MESSAGE));
    }
  }

  private static void addReportBugsMenu(final JMenu parentMenu) {
    parentMenu.add(SwingAction.of("Send Bug Report",
        e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_ISSUES))).setMnemonic(KeyEvent.VK_B);
  }

}
