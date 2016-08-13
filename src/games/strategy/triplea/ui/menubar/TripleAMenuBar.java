package games.strategy.triplea.ui.menubar;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.pushingpixels.substance.api.skin.SubstanceAutumnLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceBusinessBlackSteelLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceCeruleanLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceChallengerDeepLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceCremeLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceDustCoffeeLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceDustLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceEmeraldDuskLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGeminiLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceMagellanLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceMarinerLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceNebulaBrickWallLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceOfficeBlack2007LookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel;
import org.pushingpixels.substance.api.skin.SubstanceTwilightLookAndFeel;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.util.Triple;

public class TripleAMenuBar extends JMenuBar {
  private static final long serialVersionUID = -1447295944297939539L;
  protected final TripleAFrame frame;

  public TripleAMenuBar(final TripleAFrame frame) {
    this.frame = frame;
    new FileMenu(this, frame);
    new ViewMenu(this, frame);
    new GameMenu(this, frame);
    new ExportMenu(this, frame);


    final Optional<InGameLobbyWatcherWrapper> watcher = frame.getInGameLobbyWatcher();
    if( watcher.isPresent() && watcher.get().isActive()) {
      createLobbyMenu(this, watcher.get());
    }
    new NetworkMenu(this, watcher, frame);
    new WebHelpMenu(this);

    new DebugMenu(this, frame);
    new HelpMenu(this, frame.getUIContext(), frame.getGame().getData(), getBackground());
  }

  private void createLobbyMenu(final JMenuBar menuBar, InGameLobbyWatcherWrapper watcher ) {
    final JMenu lobby = new JMenu("Lobby");
    lobby.setMnemonic(KeyEvent.VK_L);
    menuBar.add(lobby);
    lobby.add(new EditGameCommentAction(watcher, frame));
    lobby.add(new RemoveGameFromLobbyAction(watcher));
  }


  public static File getSaveGameLocationDialog(final Frame frame) {

    // For some strange reason,
    // the only way to get a Mac OS X native-style file dialog
    // is to use an AWT FileDialog instead of a Swing JDialog
    if (GameRunner.isMac()) {
      final FileDialog fileDialog = new FileDialog(frame);
      fileDialog.setMode(FileDialog.SAVE);


      fileDialog.setDirectory(ClientContext.folderSettings().getSaveGamePath());
      fileDialog.setFilenameFilter((dir, name) -> { // the extension should be .tsvg, but find svg
                                                                 // extensions as well
        return name.endsWith(".tsvg") || name.endsWith(".svg");
      });
      fileDialog.setVisible(true);
      String fileName = fileDialog.getFile();
      final String dirName = fileDialog.getDirectory();
      if (fileName == null) {
        return null;
      } else {
        if (!fileName.endsWith(".tsvg")) {
          fileName += ".tsvg";
        }
        // If the user selects a filename that already exists,
        // the AWT Dialog on Mac OS X will ask the user for confirmation
        final File f = new File(dirName, fileName);
        return f;
      }
    }
    // Non-Mac platforms should use the normal Swing JFileChooser
    else {
      final JFileChooser fileChooser = SaveGameFileChooser.getInstance();
      final int rVal = fileChooser.showSaveDialog(frame);
      if (rVal != JFileChooser.APPROVE_OPTION) {
        return null;
      }
      File f = fileChooser.getSelectedFile();
      // disallow sub directories to be entered (in the form directory/name) for Windows boxes
      if (GameRunner.isWindows()) {
        final int slashIndex = Math.min(f.getPath().lastIndexOf("\\"), f.getPath().length());
        final String filePath = f.getPath().substring(0, slashIndex);
        if (!fileChooser.getCurrentDirectory().toString().equals(filePath)) {
          JOptionPane.showConfirmDialog(frame, "Sub directories are not allowed in the file name.  Please rename it.",
              "Cancel?", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);
          return null;
        }
      }
      if (!f.getName().toLowerCase().endsWith(".tsvg")) {
        f = new File(f.getParent(), f.getName() + ".tsvg");
      }
      // A small warning so users will not over-write a file
      if (f.exists()) {
        final int choice =
            JOptionPane.showConfirmDialog(frame, "A file by that name already exists. Do you wish to over write it?",
                "Over-write?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
          return null;
        }
      }
      return f;
    }
  }

  public static List<String> getLookAndFeelAvailableList() {
    final List<String> substanceLooks = new ArrayList<>();
    for (final LookAndFeelInfo look : UIManager.getInstalledLookAndFeels()) {
      substanceLooks.add(look.getClassName());
    }
    substanceLooks.addAll(
        Arrays.asList(
            SubstanceAutumnLookAndFeel.class.getName(),
            SubstanceBusinessBlackSteelLookAndFeel.class.getName(),
            SubstanceBusinessBlueSteelLookAndFeel.class.getName(),
            SubstanceBusinessLookAndFeel.class.getName(),
            SubstanceCeruleanLookAndFeel.class.getName(),
            SubstanceChallengerDeepLookAndFeel.class.getName(),
            SubstanceCremeCoffeeLookAndFeel.class.getName(),
            SubstanceCremeLookAndFeel.class.getName(),
            SubstanceDustCoffeeLookAndFeel.class.getName(),
            SubstanceDustLookAndFeel.class.getName(),
            SubstanceEmeraldDuskLookAndFeel.class.getName(),
            SubstanceGeminiLookAndFeel.class.getName(),
            SubstanceGraphiteAquaLookAndFeel.class.getName(),
            SubstanceGraphiteGlassLookAndFeel.class.getName(),
            SubstanceGraphiteLookAndFeel.class.getName(),
            SubstanceMagellanLookAndFeel.class.getName(),
            SubstanceMarinerLookAndFeel.class.getName(),
            SubstanceMistAquaLookAndFeel.class.getName(),
            SubstanceMistSilverLookAndFeel.class.getName(),
            SubstanceModerateLookAndFeel.class.getName(),
            SubstanceNebulaBrickWallLookAndFeel.class.getName(),
            SubstanceNebulaLookAndFeel.class.getName(),
            SubstanceOfficeBlack2007LookAndFeel.class.getName(),
            SubstanceOfficeBlue2007LookAndFeel.class.getName(),
            SubstanceOfficeSilver2007LookAndFeel.class.getName(),
            SubstanceRavenLookAndFeel.class.getName(),
            SubstanceSaharaLookAndFeel.class.getName(),
            SubstanceTwilightLookAndFeel.class.getName()
        ));
    return substanceLooks;
  }

  /**
   * First is our JList, second is our LookAndFeels string -> class map, third is our 'current' look and feel.
   */
  public static Triple<JList<String>, Map<String, String>, String> getLookAndFeelList() {
    final Map<String, String> lookAndFeels = new LinkedHashMap<>();
    try {
      final List<String> substanceLooks = getLookAndFeelAvailableList();
      for (final String s : substanceLooks) {
        final Class<?> c = Class.forName(s);
        final LookAndFeel lf = (LookAndFeel) c.newInstance();
        lookAndFeels.put(lf.getName(), s);
      }
    } catch (final Exception e) {
      ClientLogger.logError("An Error occured while getting LookAndFeels", e);
      // we know all machines have these 3, so use them
      lookAndFeels.clear();
      lookAndFeels.put("Original", UIManager.getSystemLookAndFeelClassName());
      lookAndFeels.put("Metal", MetalLookAndFeel.class.getName());
      lookAndFeels.put("Platform Independent", UIManager.getCrossPlatformLookAndFeelClassName());
    }
    final JList<String> list = new JList<>(new Vector<>(lookAndFeels.keySet()));
    String currentKey = null;
    for (final String s : lookAndFeels.keySet()) {
      final String currentName = UIManager.getLookAndFeel().getClass().getName();
      if (lookAndFeels.get(s).equals(currentName)) {
        currentKey = s;
        break;
      }
    }
    list.setSelectedValue(currentKey, false);
    return Triple.of(list, lookAndFeels, currentKey);
  }
}
