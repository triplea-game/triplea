package games.strategy.triplea.ui.history;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.RandomStatsDetails;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.formatter.MyFormatter;
import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import lombok.extern.java.Log;
import org.triplea.java.collections.IntegerMap;

/**
 * A window used to display a textual summary of a particular history node, including all of its
 * descendants, if applicable.
 */
@Log
public class HistoryLog extends JFrame {
  private static final long serialVersionUID = 4880602702815333376L;
  private final JTextArea textArea;
  private final StringWriter stringWriter;
  private final PrintWriter printWriter;

  public HistoryLog() {
    textArea = new JTextArea(40, 80);
    textArea.setEditable(false);
    final JScrollPane scrollingArea = new JScrollPane(textArea);
    // ... Get the content pane, set layout, add to center
    final JPanel content = new JPanel();
    content.setLayout(new BorderLayout());
    content.add(scrollingArea, BorderLayout.CENTER);
    stringWriter = new StringWriter();
    printWriter = new PrintWriter(stringWriter);
    // ... Set window characteristics.
    this.setContentPane(content);
    this.setTitle("History Log");
    this.pack();
    this.setLocationRelativeTo(null);
  }

  public PrintWriter getWriter() {
    return printWriter;
  }

  @Override
  public String toString() {
    return stringWriter.toString();
  }

  public void clear() {
    stringWriter.getBuffer().delete(0, stringWriter.getBuffer().length());
    textArea.setText("");
  }

  /**
   * Adds details about the current turn for each player in {@code playersAllowed} to the log.
   * Information about each step and event that occurred during the turn are included.
   */
  public void printFullTurn(
      final GameData data, final boolean verbose, final Collection<GamePlayer> playersAllowed) {
    HistoryNode curNode = data.getHistory().getLastNode();
    final Collection<GamePlayer> players = new HashSet<>();
    if (playersAllowed != null) {
      players.addAll(playersAllowed);
    }
    // find Step node, if exists in this path
    Step stepNode = null;
    while (curNode != null) {
      if (curNode instanceof Step) {
        stepNode = (Step) curNode;
        break;
      }
      curNode = (HistoryNode) curNode.getPreviousNode();
    }
    if (stepNode != null) {
      final GamePlayer curPlayer = stepNode.getPlayerId();
      if (players.isEmpty()) {
        players.add(curPlayer);
      }
      // get first step for this turn
      Step turnStartNode;
      while (true) {
        turnStartNode = stepNode;
        stepNode = (Step) stepNode.getPreviousSibling();
        if (stepNode == null) {
          break;
        }
        if (stepNode.getPlayerId() == null) {
          break;
        }
        if (!players.contains(stepNode.getPlayerId())) {
          break;
        }
      }
      printRemainingTurn(turnStartNode, verbose, data.getDiceSides(), players);
    } else {
      log.severe("No step node found in!");
    }
  }

  private static GamePlayer getPlayerId(final HistoryNode printNode) {
    DefaultMutableTreeNode curNode = printNode;
    final TreePath parentPath = new TreePath(printNode.getPath()).getParentPath();
    GamePlayer curPlayer = null;
    if (parentPath != null) {
      final Object[] pathToNode = parentPath.getPath();
      for (final Object pathNode : pathToNode) {
        final HistoryNode node = (HistoryNode) pathNode;
        if (node instanceof Step) {
          curPlayer = ((Step) node).getPlayerId();
        }
      }
    }
    do {
      final Enumeration<?> nodeEnum = curNode.preorderEnumeration();
      while (nodeEnum.hasMoreElements()) {
        final HistoryNode node = (HistoryNode) nodeEnum.nextElement();
        if (node instanceof Step) {
          final String title = node.getTitle();
          final GamePlayer gamePlayer = ((Step) node).getPlayerId();
          if (!title.equals("Initializing Delegates") && gamePlayer != null) {
            curPlayer = gamePlayer;
          }
        }
      }
      curNode = curNode.getNextSibling();
    } while ((curNode instanceof Step) && ((Step) curNode).getPlayerId().equals(curPlayer));
    return curPlayer;
  }

  /**
   * Adds details about {@code printNode} and all its sibling and child nodes that are part of the
   * current turn for each player in {@code playersAllowed} to the log.
   */
  public void printRemainingTurn(
      final HistoryNode printNode,
      final boolean verbose,
      final int diceSides,
      final Collection<GamePlayer> playersAllowed) {
    final PrintWriter logWriter = printWriter;
    final String moreIndent = "    ";
    // print out the parent nodes
    final TreePath parentPath = new TreePath(printNode.getPath()).getParentPath();
    GamePlayer currentPlayer = null;
    if (parentPath != null) {
      final Object[] pathToNode = parentPath.getPath();
      for (final Object pathNode : pathToNode) {
        final HistoryNode node = (HistoryNode) pathNode;
        for (int i = 0; i < node.getLevel(); i++) {
          logWriter.print(moreIndent);
        }
        logWriter.println(node.getTitle());
        if (node.getLevel() == 0) {
          logWriter.println();
        }
        if (node instanceof Step) {
          currentPlayer = ((Step) node).getPlayerId();
        }
      }
    }
    final Collection<GamePlayer> players = new HashSet<>();
    if (playersAllowed != null) {
      players.addAll(playersAllowed);
    }
    if (currentPlayer != null) {
      players.add(currentPlayer);
    }
    final List<String> moveList = new ArrayList<>();
    boolean moving = false;
    DefaultMutableTreeNode curNode = printNode;
    final Map<String, Double> hitDifferentialMap = new HashMap<>();
    do {
      // keep track of conquered territory during combat
      StringBuilder conquerStr = new StringBuilder();
      final Enumeration<?> nodeEnum = curNode.preorderEnumeration();
      while (nodeEnum.hasMoreElements()) {
        final HistoryNode node = (HistoryNode) nodeEnum.nextElement();
        final String title = node.getTitle();
        final String indent = moreIndent.repeat(Math.max(0, node.getLevel()));
        // flush move list
        if (moving && !(node instanceof Renderable)) {
          final Iterator<String> moveIter = moveList.iterator();
          while (moveIter.hasNext()) {
            logWriter.println(moveIter.next());
            moveIter.remove();
          }
          moving = false;
        }
        if (node instanceof Renderable) {
          final Object details = ((Renderable) node).getRenderingData();
          if (details instanceof DiceRoll) {
            if (!verbose) {
              continue;
            }
            final String diceMsg1 = title.substring(0, title.indexOf(':') + 1);
            if (diceMsg1.isEmpty()) {
              // tech roll
              logWriter.println(indent + moreIndent + title);
            } else {
              // dice roll
              logWriter.print(indent + moreIndent + diceMsg1);
              final String hitDifferentialKey =
                  parseHitDifferentialKeyFromDiceRollMessage(diceMsg1);
              final DiceRoll diceRoll = (DiceRoll) details;
              final int hits = diceRoll.getHits();
              int rolls = 0;
              for (int i = 1; i <= diceSides; i++) {
                rolls += diceRoll.getRolls(i).size();
              }
              final double expectedHits = diceRoll.getExpectedHits();
              logWriter.println(
                  " "
                      + hits
                      + "/"
                      + rolls
                      + " hits, "
                      + String.format("%.2f", expectedHits)
                      + " expected hits");
              final double hitDifferential = hits - expectedHits;
              hitDifferentialMap.merge(hitDifferentialKey, hitDifferential, Double::sum);
            }
          } else if (details instanceof MoveDescription) {
            // movement
            final Pattern p = Pattern.compile("\\w+ undo move (\\d+).");
            final Matcher m = p.matcher(title);
            if (m.matches()) {
              moveList.remove(Integer.parseInt(m.group(1)) - 1);
            } else {
              moveList.add(indent + title);
              moving = true;
            }
          } else if (details instanceof Collection) {
            @SuppressWarnings("unchecked")
            final Collection<Object> objects = (Collection<Object>) details;
            final Iterator<Object> objIter = objects.iterator();
            if (objIter.hasNext()) {
              final Object obj = objIter.next();
              if (obj instanceof Unit) {
                @SuppressWarnings("unchecked")
                final Collection<Unit> allUnitsInDetails = (Collection<Unit>) details;
                // purchase/place units - don't need details
                Unit unit = (Unit) obj;
                if (title.matches("\\w+ buy .*")
                    || title.matches("\\w+ attack with .*")
                    || title.matches("\\w+ defend with .*")) {
                  logWriter.println(indent + title);
                } else if (title.matches("\\d+ \\w+ owned by the .*? lost .*")
                    || title.matches("\\d+ \\w+ owned by the .*? lost")) {
                  if (!verbose) {
                    continue;
                  }
                  logWriter.println(indent + moreIndent + title);
                } else if (title.startsWith("Battle casualty summary:")) {
                  // logWriter.println(indent+"CAS1: "+title);
                  logWriter.println(
                      indent
                          + conquerStr.toString()
                          + ". Battle score "
                          + title.substring(title.indexOf("for attacker is")));
                  conquerStr = new StringBuilder();
                  // separate units by player and show casualty summary
                  final IntegerMap<GamePlayer> unitCount = new IntegerMap<>();
                  unitCount.add(unit.getOwner(), 1);
                  while (objIter.hasNext()) {
                    unit = (Unit) objIter.next();
                    unitCount.add(unit.getOwner(), 1);
                  }
                  for (final GamePlayer player : unitCount.keySet()) {
                    logWriter.println(
                        indent
                            + "Casualties for "
                            + player.getName()
                            + ": "
                            + MyFormatter.unitsToTextNoOwner(allUnitsInDetails, player));
                  }
                } else if (title.matches(".*? placed in .*")
                    || title.matches(".* owned by the \\w+ retreated to .*")) {
                  logWriter.println(indent + title);
                } else if (title.matches("\\w+ win")) {
                  conquerStr =
                      new StringBuilder(
                          title
                              + conquerStr
                              + " with "
                              + MyFormatter.unitsToTextNoOwner(allUnitsInDetails)
                              + " remaining");
                } else {
                  logWriter.println(indent + title);
                }
              } else {
                // collection of unhandled objects
                logWriter.println(indent + title);
              }
            } else {
              // empty collection of something
              if (title.matches("\\w+ win")) {
                conquerStr = new StringBuilder(title + conquerStr + " with no units remaining");
              } else {
                // empty collection of unhandled objects
                logWriter.println(indent + title);
              }
            }
          } else if (details instanceof Territory) {
            // territory details
            logWriter.println(indent + title);
          } else if (details == null) {
            if (titleNeedsFurtherProcessing(title)) {
              if (title.matches("\\w+ collect \\d+ PUs?.*")) {
                logWriter.println(indent + title);
              } else if (title.matches("\\w+ takes? .*? from \\w+")) {
                // British take Libya from Germans
                if (moving) {
                  final String str = moveList.remove(moveList.size() - 1);
                  moveList.add(str + "\n  " + indent + title.replaceAll(" takes ", " take "));
                } else {
                  conquerStr.append(title.replaceAll("^\\w+ takes ", ", taking "));
                }
              } else if (title.matches("\\w+ spend \\d+ on tech rolls")) {
                logWriter.println(indent + title);
              } else if (!title.startsWith("Rolls to resolve tech hits:")) {
                logWriter.println(indent + title);
              }
            }
          } else {
            // unknown details object
            logWriter.println(indent + title);
          }
        } else if (node instanceof Step) {
          final GamePlayer gamePlayer = ((Step) node).getPlayerId();
          if (!title.equals("Initializing Delegates")) {
            logWriter.println();
            logWriter.print(indent + title);
            if (gamePlayer != null) {
              currentPlayer = gamePlayer;
              players.add(currentPlayer);
              logWriter.print(" - " + gamePlayer.getName());
            }
            logWriter.println();
          }
        } else if (node instanceof Round) {
          logWriter.println();
          logWriter.println(indent + title);
        } else {
          logWriter.println(indent + title);
        }
      } // while (nodeEnum.hasMoreElements())
      curNode = curNode.getNextSibling();
    } while ((curNode instanceof Step) && players.contains(((Step) curNode).getPlayerId()));
    // if we are mid-phase, this might not get flushed
    if (moving && !moveList.isEmpty()) {
      final Iterator<String> moveIter = moveList.iterator();
      while (moveIter.hasNext()) {
        logWriter.println(moveIter.next());
        moveIter.remove();
      }
    }
    logWriter.println();
    if (verbose) {
      logWriter.println("Combat Hit Differential Summary :");
      logWriter.println();
      for (final String player : hitDifferentialMap.keySet()) {
        logWriter.println(
            moreIndent + player + " : " + String.format("%.2f", hitDifferentialMap.get(player)));
      }
    }
    logWriter.println();
    textArea.setText(stringWriter.toString());
  }

  private static boolean titleNeedsFurtherProcessing(final String title) {
    return !(title.equals("Adding original owners")
        || title.equals(MoveDelegate.CLEANING_UP_DURING_MOVEMENT_PHASE)
        || title.equals("Game Loaded")
        || title.contains("now being played by")
        || title.contains("Turn Summary")
        || title.contains("Move Summary")
        || title.contains("Setting uses for triggers used")
        || title.equals("Resetting and Giving Bonus Movement to Units")
        || title.equals("Recording Battle Statistics")
        || title.equals("Preparing Airbases for Possible Scrambling"));
  }

  @VisibleForTesting
  static String parseHitDifferentialKeyFromDiceRollMessage(final String message) {
    final Pattern diceRollPattern = Pattern.compile("^(.+) roll(?: (.+))? dice");
    final Matcher matcher = diceRollPattern.matcher(message);
    if (matcher.find()) {
      return matcher.group(1) + " " + Optional.ofNullable(matcher.group(2)).orElse("regular");
    }

    final int lastColonIndex = message.lastIndexOf(" :");
    return (lastColonIndex != -1) ? message.substring(0, lastColonIndex) : message;
  }

  /**
   * Adds a territory summary for the player associated with {@code printNode} to the log. The
   * summary includes each unit present in the territory.
   */
  public void printTerritorySummary(final HistoryNode printNode, final GameData data) {
    final Collection<Territory> territories;
    final GamePlayer player = getPlayerId(printNode);
    data.acquireReadLock();
    try {
      territories = data.getMap().getTerritories();
    } finally {
      data.releaseReadLock();
    }
    final Collection<GamePlayer> players = new HashSet<>();
    players.add(player);
    printTerritorySummary(players, territories);
  }

  private void printTerritorySummary(final GameData data) {
    final Collection<Territory> territories;
    final GamePlayer player;
    data.acquireReadLock();
    try {
      player = data.getSequence().getStep().getPlayerId();
      territories = data.getMap().getTerritories();
    } finally {
      data.releaseReadLock();
    }
    final Collection<GamePlayer> players = new HashSet<>();
    players.add(player);
    printTerritorySummary(players, territories);
  }

  /**
   * Adds a territory summary for each player in {@code allowedPlayers} to the log. The summary
   * includes each unit present in the territory.
   */
  public void printTerritorySummary(
      final GameData data, final Collection<GamePlayer> allowedPlayers) {
    if (allowedPlayers == null || allowedPlayers.isEmpty()) {
      printTerritorySummary(data);
      return;
    }
    final Collection<Territory> territories;
    data.acquireReadLock();
    try {
      territories = data.getMap().getTerritories();
    } finally {
      data.releaseReadLock();
    }
    printTerritorySummary(allowedPlayers, territories);
  }

  private void printTerritorySummary(
      final Collection<GamePlayer> players, final Collection<Territory> territories) {
    if (players == null || players.isEmpty() || territories == null || territories.isEmpty()) {
      return;
    }
    final PrintWriter logWriter = printWriter;
    // print all units in all territories, including "flags"
    logWriter.println(
        "Territory Summary for " + MyFormatter.defaultNamedToTextList(players) + " : \n");
    for (final Territory t : territories) {
      final List<Unit> ownedUnits =
          t.getUnitCollection().getMatches(Matches.unitIsOwnedByOfAnyOfThesePlayers(players));
      // see if there's a flag
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      final boolean hasFlag =
          ta != null
              && t.getOwner() != null
              && players.contains(t.getOwner())
              && (ta.getOriginalOwner() == null || !players.contains(ta.getOriginalOwner()));
      if (hasFlag || !ownedUnits.isEmpty()) {
        logWriter.print("    " + t.getName() + " : ");
        if (hasFlag && ownedUnits.isEmpty()) {
          logWriter.println("1 flag");
        } else if (hasFlag) {
          logWriter.print("1 flag, ");
        }
        if (!ownedUnits.isEmpty()) {
          logWriter.println(MyFormatter.unitsToTextNoOwner(ownedUnits));
        }
      }
    }
    logWriter.println();
    logWriter.println();
    textArea.setText(stringWriter.toString());
  }

  public void printDiceStatistics(final GameData data, final IRandomStats randomStats) {
    final PrintWriter logWriter = printWriter;
    final RandomStatsDetails stats = randomStats.getRandomStats(data.getDiceSides());
    final String diceStats = stats.getAllStatsString();
    if (diceStats.length() > 0) {
      logWriter.println(diceStats);
      logWriter.println();
      logWriter.println();
    }
    textArea.setText(stringWriter.toString());
  }

  /** Adds a production summary for each player in the game to the log. */
  public void printProductionSummary(final GameData data) {
    final PrintWriter logWriter = printWriter;
    final Collection<GamePlayer> players;
    final Resource pus;
    data.acquireReadLock();
    try {
      pus = data.getResourceList().getResource(Constants.PUS);
      players = data.getPlayerList().getPlayers();
    } finally {
      data.releaseReadLock();
    }
    if (pus == null) {
      return;
    }
    logWriter.println("Production/PUs Summary :\n");
    for (final GamePlayer player : players) {
      final int pusQuantity = player.getResources().getQuantity(pus);
      final int production = getProduction(player, data);
      logWriter.println("    " + player.getName() + " : " + production + " / " + pusQuantity);
    }
    logWriter.println();
    logWriter.println();
    textArea.setText(stringWriter.toString());
  }

  private static int getProduction(final GamePlayer player, final GameData data) {
    int production = 0;
    for (final Territory place : data.getMap().getTerritories()) {
      boolean isConvoyOrLand = false;
      final TerritoryAttachment ta = TerritoryAttachment.get(place);
      if (!place.isWater()
          || (ta != null
              && !GamePlayer.NULL_PLAYERID.equals(OriginalOwnerTracker.getOriginalOwner(place))
              && player.equals(OriginalOwnerTracker.getOriginalOwner(place))
              && place.getOwner().equals(player))) {
        isConvoyOrLand = true;
      }
      if (place.getOwner().equals(player) && isConvoyOrLand && ta != null) {
        production += ta.getProduction();
      }
    }
    return production;
  }
}
