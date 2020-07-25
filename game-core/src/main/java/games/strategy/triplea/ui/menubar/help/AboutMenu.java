package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.ui.MacOsIntegration;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.util.Version;

@UtilityClass
class AboutMenu {
  Action buildMenu(String gameName, Version gameVersion) {
    final String text =
        "<html>"
            + "<h2>"
            + gameName
            + "</h2>"
            + "<b>Engine Version:</b> "
            + ClientContext.engineVersion()
            + "<br>"
            + "<b>Game Version:</b> "
            + gameVersion
            + "<br>"
            + "<br>"
            + "TripleA Website: <b>"
            + UrlConstants.TRIPLEA_WEBSITE
            + "</b><br>"
            + "<br>"
            + "<b>License</b><br>"
            + "<br>"
            + "Copyright (C) 2001-2019 TripleA contributors.<br>"
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
            + "<b>"
            + UrlConstants.LICENSE_NOTICE
            + "</b><br>"
            + "<br>"
            + "<b>TripleA Is Accredited To:</b><br>"
            + "<ul>"
            + "<li>TripleA Players and Community</li>"
            + "<li>TripleA Game Testers</li>"
            + "<li>TripleA Development Team</li>"
            + "<li>Installer by Install4j</li>"
            + "<li>Lantern button icon created by Made x Made from the Noun Project</li>"
            + "<li>Flag button icon created by AFY Studio from the Noun Project</li>"
            + "</ul>"
            + "</html>";
    final JLabel label =
        JLabelBuilder.builder()
            .border(BorderFactory.createEmptyBorder(0, 0, 20, 0))
            .text(text)
            .build();

    if (!SystemProperties.isMac()) {
      return SwingAction.of(
          "About",
          e ->
              JOptionPane.showMessageDialog(
                  null, label, "About " + gameName, JOptionPane.PLAIN_MESSAGE)))
    } else { // On Mac OS X, put the About menu where Mac users expect it to be
      MacOsIntegration.setAboutHandler(
          () ->
              JOptionPane.showMessageDialog(
                  null, label, "About " + gameName, JOptionPane.PLAIN_MESSAGE));
    }
  }

}
