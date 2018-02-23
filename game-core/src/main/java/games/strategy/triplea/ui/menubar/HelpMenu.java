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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
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
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.LocalizeHtml;
import swinglib.JLabelBuilder;

/**
 * The help menu.
 */
public final class HelpMenu extends JMenu {
  private static final long serialVersionUID = 4070541434144687452L;

  private final UiContext uiContext;
  private final GameData gameData;

  HelpMenu(final UiContext uiContext, final GameData gameData) {
    super("Help");

    this.uiContext = uiContext;
    this.gameData = gameData;

    setMnemonic(KeyEvent.VK_H);

    addMoveHelpMenu();
    addUnitHelpMenu();
    addGameNotesMenu();
    addAboutMenu();
    addReportBugsMenu();
  }

  private void addMoveHelpMenu() {
    final String moveSelectionHelpTitle = "Movement/Selection Help";
    add(SwingAction.of(moveSelectionHelpTitle, e -> {
      // html formatted string
      final JEditorPane editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorPane.setContentType("text/html");
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
      editorPane.setText(hints);
      final JScrollPane scroll = new JScrollPane(editorPane);
      JOptionPane.showMessageDialog(null, scroll, moveSelectionHelpTitle, JOptionPane.PLAIN_MESSAGE);
    })).setMnemonic(KeyEvent.VK_M);
  }

  static String getUnitStatsTable(final GameData gameData, final UiContext uiContext) {
    // html formatted string
    final StringBuilder hints = new StringBuilder();
    hints.append("<html>");
    hints.append("<head><style>th, tr{color:black}</style></head>");
    try {
      gameData.acquireReadLock();
      final Map<PlayerID, Map<UnitType, ResourceCollection>> costs =
          TuvUtils.getResourceCostsForTuv(gameData, true);
      final Map<PlayerID, List<UnitType>> playerUnitTypes =
          UnitType.getAllPlayerUnitsWithImages(gameData, uiContext, true);
      final String color3 = "FEECE2";
      final String color2 = "BDBDBD";
      final String color1 = "ABABAB";
      int i = 0;
      for (final Map.Entry<PlayerID, List<UnitType>> entry : playerUnitTypes.entrySet()) {
        final PlayerID player = entry.getKey();
        hints.append("<p><table border=\"1\" bgcolor=\"" + color1 + "\">");
        hints.append("<tr><th style=\"font-size:120%;000000\" bgcolor=\"" + color3 + "\" colspan=\"4\">")
            .append((player == null) ? "NULL" : player.getName()).append(" Units</th></tr>");
        hints.append("<tr").append(((i & 1) == 0) ? (" bgcolor=\"" + color1 + "\"") : (" bgcolor=\"" + color2 + "\""))
            .append("><td>Unit</td><td>Name</td><td>Cost</td><td>Tool Tip</td></tr>");
        for (final UnitType ut : entry.getValue()) {
          i++;
          hints.append("<tr").append(((i & 1) == 0) ? (" bgcolor=\"" + color1 + "\"") : (" bgcolor=\"" + color2 + "\""))
              .append(">").append("<td>").append(getUnitImageUrl(ut, player, uiContext)).append("</td>").append("<td>")
              .append(ut.getName()).append("</td>").append("<td>").append(costs.get(player).get(ut).toStringForHtml())
              .append("</td>").append("<td>").append(ut.getTooltip(player)).append("</td></tr>");
        }
        i++;
        hints.append("<tr").append(((i & 1) == 0) ? (" bgcolor=\"" + color1 + "\"") : (" bgcolor=\"" + color2 + "\""))
            .append(">").append("<td>Unit</td><td>Name</td><td>Cost</td><td>Tool Tip</td></tr></table></p><br />");
      }
    } finally {
      gameData.releaseReadLock();
    }
    hints.append("</html>");
    return hints.toString();
  }

  private static String getUnitImageUrl(final UnitType unitType, final PlayerID player, final UiContext uiContext) {
    final UnitImageFactory unitImageFactory = uiContext.getUnitImageFactory();
    if ((player == null) || (unitImageFactory == null)) {
      return "no image";
    }
    final Optional<URL> imageUrl = unitImageFactory.getBaseImageUrl(unitType.getName(), player);
    final String imageLocation = imageUrl.isPresent() ? imageUrl.get().toString() : "";

    return "<img src=\"" + imageLocation + "\" border=\"0\"/>";
  }



  private void addUnitHelpMenu() {
    final String unitHelpTitle = "Unit Help";
    add(SwingAction.of(unitHelpTitle, e -> {
      final JEditorPane editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorPane.setContentType("text/html");
      editorPane.setText(getUnitStatsTable(gameData, uiContext));
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
              ((scroll.getPreferredSize().width > availWidth) ? availWidth
                  : ((scroll.getPreferredSize().height > availHeight)
                  ? Math.min(availWidth, scroll.getPreferredSize().width + 22)
                  : scroll.getPreferredSize().width)),
              ((scroll.getPreferredSize().height > availHeight) ? availHeight
                  : ((scroll.getPreferredSize().width > availWidth)
                  ? Math.min(availHeight, scroll.getPreferredSize().height + 22)
                  : scroll.getPreferredSize().height))));
      final JDialog dialog = new JDialog((JFrame) null, unitHelpTitle);
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

  private void addGameNotesMenu() {
    // allow the game developer to write notes that appear in the game
    // displays whatever is in the notes field in html
    final String notesProperty = gameData.getProperties().get("notes", "");
    if ((notesProperty != null) && (notesProperty.trim().length() != 0)) {
      final String notes = LocalizeHtml.localizeImgLinksInHtml(notesProperty.trim());
      gameNotesPane.setEditable(false);
      gameNotesPane.setContentType("text/html");
      gameNotesPane.setText(notes);
      gameNotesPane.setForeground(Color.BLACK);
      final String gameNotesTitle = "Game Notes";
      add(SwingAction.of(gameNotesTitle, e -> SwingUtilities.invokeLater(() -> {
        final JScrollPane scroll = new JScrollPane(gameNotesPane);
        scroll.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
        final JDialog dialog = new JDialog((JFrame) null, gameNotesTitle);
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
        dialog.addWindowListener(new WindowAdapter() {
          @Override
          public void windowOpened(final WindowEvent e) {
            button.requestFocus();
          }
        });
        dialog.setVisible(true);
      }))).setMnemonic(KeyEvent.VK_N);
    }
  }

  private void addAboutMenu() {
    final String text = "<html>"
        + "<h2>" + gameData.getGameName() + "</h2>"
        + "<b>Engine Version:</b> " + ClientContext.engineVersion().getExactVersion() + "<br>"
        + "<b>Game Version:</b> " + gameData.getGameVersion() + "<br>"
        + "<br>"
        + "For more information, please visit: <b>" + UrlConstants.TRIPLEA_WEBSITE + "</b><br>"
        + "<br>"
        + "<b>License</b><br>"
        + "<br>"
        + "Copyright (C) 2001-2018 TripleA contributors.<br>"
        + "<br>"
        + "This program is free software: you can redistribute it and/or modify<br>"
        + "it under the terms of the GNU General Public License as published by<br>"
        + "the Free Software Foundation, either version 3 of the License, or<br>"
        + "(at your option) any later version.<br>"
        + "<br>"
        + "This program is distributed in the hope that it will be useful,<br>"
        + "but WITHOUT ANY WARRANTY; without even the implied warranty of<br>"
        + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the<br>"
        + "GNU General Public License for more details.<br>"
        + "<br>"
        + "The complete license notice is available at<br>"
        + "<b>" + UrlConstants.LICENSE_NOTICE + "</b><br>"
        + "</html>";
    final JLabel label = JLabelBuilder.builder()
        .border(BorderFactory.createEmptyBorder(0, 0, 20, 0))
        .text(text)
        .build();

    if (!SystemProperties.isMac()) {
      addSeparator();
      add(SwingAction.of("About", e -> JOptionPane.showMessageDialog(null, label,
          "About " + gameData.getGameName(), JOptionPane.PLAIN_MESSAGE))).setMnemonic(KeyEvent.VK_A);
    } else { // On Mac OS X, put the About menu where Mac users expect it to be
      Application.getApplication().setAboutHandler(paramAboutEvent -> JOptionPane.showMessageDialog(null, label,
          "About " + gameData.getGameName(), JOptionPane.PLAIN_MESSAGE));
    }
  }

  private void addReportBugsMenu() {
    add(SwingAction.of("Send Bug Report",
        e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_ISSUES))).setMnemonic(KeyEvent.VK_B);
  }
}
