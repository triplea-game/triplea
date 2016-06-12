package games.strategy.triplea.ui.menubar;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.DefaultMutableTreeNode;

import games.strategy.triplea.ui.BattleDisplay;
import games.strategy.triplea.ui.ExtendedStats;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.PoliticalStateOverview;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.VerifiedRandomNumbersDialog;
import games.strategy.ui.SwingAction;
import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.RandomStatsDetails;
import games.strategy.engine.stats.IStat;
import games.strategy.sound.SoundOptions;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorDialog;
import games.strategy.triplea.printgenerator.SetupFrame;
import games.strategy.triplea.util.PlayerOrderComparator;
import games.strategy.ui.IntTextField;
import games.strategy.ui.SwingComponents;
import games.strategy.util.IllegalCharacterRemover;
import games.strategy.util.LocalizeHTML;

/**
 * Main menu for the triplea frame.
 */
public class TripleAMenu extends BasicGameMenuBar {
  private static final long serialVersionUID = 987243661147503593L;

  public TripleAMenu(final TripleAFrame frame) {
    super(frame);
  }

  @Override
  protected void createGameSpecificMenus(final JMenuBar menuBar) {
    new ViewMenu(menuBar, frame, getData());
    new GameMenu(menuBar, frame, getData());
    new ExportMenu(menuBar, frame, getData());
  }


}
