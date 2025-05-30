package games.strategy.triplea.ui.history;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
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
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.collections.IntegerMap;

/**
 * A window used to display a textual summary of a particular history node, including all of its
 * descendants, if applicable.
 */
@Slf4j
public class HistoryLog extends JFrame {
  private static final long serialVersionUID = 4880602702815333376L;
  private final JTextArea textArea;
  private final StringBuilder stringBuilder = new StringBuilder();

  public HistoryLog() {
    textArea = new JTextArea(40, 80);
    textArea.setEditable(false);
    final JScrollPane scrollingArea = new JScrollPane(textArea);
    // ... Get the content pane, set layout, add to center
    final JPanel content = new JPanel();
    content.setLayout(new BorderLayout());
    content.add(scrollingArea, BorderLayout.CENTER);
    // ... Set window characteristics.
    this.setContentPane(content);
    this.setTitle("History Log");
    this.pack();
    this.setLocationRelativeTo(null);
  }

  public void append(final String string) {
    stringBuilder.append(string);
  }

  @Override
  public String toString() {
    return stringBuilder.toString();
  }

  public void clear() {
    stringBuilder.setLength(0);
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
      log.error("No step node found in!");
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
    final String moreIndent = "    ";
    // print out the parent nodes
    final TreePath parentPath = new TreePath(printNode.getPath()).getParentPath();
    GamePlayer currentPlayer = null;
    if (parentPath != null) {
      final Object[] pathToNode = parentPath.getPath();
      for (final Object pathNode : pathToNode) {
        final HistoryNode node = (HistoryNode) pathNode;
        stringBuilder.append(moreIndent.repeat(Math.max(0, node.getLevel())));
        stringBuilder.append(node.getTitle()).append('\n');
        if (node.getLevel() == 0) {
          stringBuilder.append('\n');
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
            stringBuilder.append(moveIter.next()).append('\n');
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
              stringBuilder.append(indent).append(moreIndent).append(title).append('\n');
            } else {
              // dice roll
              stringBuilder.append(indent).append(moreIndent).append(diceMsg1);
              final String hitDifferentialKey =
                  parseHitDifferentialKeyFromDiceRollMessage(diceMsg1);
              final DiceRoll diceRoll = (DiceRoll) details;
              final int hits = diceRoll.getHits();
              int rolls = 0;
              for (int i = 1; i <= diceSides; i++) {
                rolls += diceRoll.getRolls(i).size();
              }
              final double expectedHits = diceRoll.getExpectedHits();
              stringBuilder
                  .append(" ")
                  .append(hits)
                  .append("/")
                  .append(rolls)
                  .append(" hits, ")
                  .append(String.format("%.2f", expectedHits))
                  .append(" expected hits")
                  .append('\n');
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
                  stringBuilder.append(indent).append(title).append('\n');
                } else if (title.matches("\\d+ \\w+ owned by the .*? lost .*")
                    || title.matches("\\d+ \\w+ owned by the .*? lost")) {
                  if (!verbose) {
                    continue;
                  }
                  stringBuilder.append(indent).append(moreIndent).append(title).append('\n');
                } else if (title.startsWith("Battle casualty summary:")) {
                  stringBuilder
                      .append(indent)
                      .append(conquerStr)
                      .append(". Battle score ")
                      .append(title.substring(title.indexOf("for attacker is")))
                      .append('\n');
                  conquerStr = new StringBuilder();
                  // separate units by player and show casualty summary
                  final IntegerMap<GamePlayer> unitCount = new IntegerMap<>();
                  unitCount.add(unit.getOwner(), 1);
                  while (objIter.hasNext()) {
                    unit = (Unit) objIter.next();
                    unitCount.add(unit.getOwner(), 1);
                  }
                  for (final GamePlayer player : unitCount.keySet()) {
                    stringBuilder
                        .append(indent)
                        .append("Casualties for ")
                        .append(player.getName())
                        .append(": ")
                        .append(MyFormatter.unitsToTextNoOwner(allUnitsInDetails, player))
                        .append('\n');
                  }
                } else if (title.matches(".*? placed in .*")
                    || title.matches(".* owned by the \\w+ retreated to .*")) {
                  stringBuilder.append(indent).append(title).append('\n');
                } else if (title.matches("\\w+ win")) {
                  conquerStr =
                      new StringBuilder(
                          title
                              + conquerStr
                              + " with "
                              + MyFormatter.unitsToTextNoOwner(allUnitsInDetails)
                              + " remaining");
                } else {
                  stringBuilder.append(indent).append(title).append('\n');
                }
              } else {
                // collection of unhandled objects
                stringBuilder.append(indent).append(title).append('\n');
              }
            } else {
              // empty collection of something
              if (title.matches("\\w+ win")) {
                conquerStr = new StringBuilder(title + conquerStr + " with no units remaining");
              } else {
                // empty collection of unhandled objects
                stringBuilder.append(indent).append(title).append('\n');
              }
            }
          } else if (details instanceof Territory) {
            // territory details
            stringBuilder.append(indent).append(title).append('\n');
          } else if (details == null) {
            if (titleNeedsFurtherProcessing(title)) {
              if (title.matches("\\w+ collect \\d+ PUs?.*")) {
                stringBuilder.append(indent).append(title).append('\n');
              } else if (title.matches("\\w+ takes? .*? from \\w+")) {
                // British take Libya from Germans
                if (moving) {
                  final String str = moveList.remove(moveList.size() - 1);
                  moveList.add(str + "\n  " + indent + title.replaceAll(" takes ", " take "));
                } else {
                  conquerStr.append(title.replaceAll("^\\w+ takes ", ", taking "));
                }
              } else if (title.matches("\\w+ spend \\d+ on tech rolls")) {
                stringBuilder.append(indent).append(title).append('\n');
              } else if (!title.startsWith("Rolls to resolve tech hits:")) {
                stringBuilder.append(indent).append(title).append('\n');
              }
            }
          } else {
            // unknown details object
            stringBuilder.append(indent).append(title).append('\n');
          }
        } else if (node instanceof Step) {
          final GamePlayer gamePlayer = ((Step) node).getPlayerId();
          if (!title.equals("Initializing Delegates")) {
            stringBuilder.append('\n').append(indent).append(title);
            if (gamePlayer != null) {
              currentPlayer = gamePlayer;
              players.add(currentPlayer);
              stringBuilder.append(" - ").append(gamePlayer.getName());
            }
            stringBuilder.append('\n');
          }
        } else if (node instanceof Round) {
          stringBuilder.append('\n').append(indent).append(title).append('\n');
        } else {
          stringBuilder.append(indent).append(title).append('\n');
        }
      }
      curNode = curNode.getNextSibling();
    } while ((curNode instanceof Step) && players.contains(((Step) curNode).getPlayerId()));
    // if we are mid-phase, this might not get flushed
    if (moving && !moveList.isEmpty()) {
      final Iterator<String> moveIter = moveList.iterator();
      while (moveIter.hasNext()) {
        stringBuilder.append(moveIter.next()).append('\n');
        moveIter.remove();
      }
    }
    stringBuilder.append('\n');
    if (verbose) {
      stringBuilder.append("Combat Hit Differential Summary :\n\n");
      for (final String player : hitDifferentialMap.keySet()) {
        stringBuilder
            .append(moreIndent)
            .append(player)
            .append(" : ")
            .append(String.format("%.2f", hitDifferentialMap.get(player)))
            .append('\n');
      }
    }
    stringBuilder.append('\n');
    textArea.setText(stringBuilder.toString());
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
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      territories = data.getMap().getTerritories();
    }
    final Collection<GamePlayer> players = new HashSet<>();
    players.add(player);
    printTerritorySummary(players, territories);
  }

  private void printTerritorySummary(final GameData data) {
    final Collection<Territory> territories;
    final GamePlayer player;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      player = data.getSequence().getStep().getPlayerId();
      territories = data.getMap().getTerritories();
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
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      territories = data.getMap().getTerritories();
    }
    printTerritorySummary(allowedPlayers, territories);
  }

  private void printTerritorySummary(
      final Collection<GamePlayer> players, final Collection<Territory> territories) {
    if (players == null || players.isEmpty() || territories == null || territories.isEmpty()) {
      return;
    }
    // print all units in all territories, including "flags"
    stringBuilder
        .append("Territory Summary for ")
        .append(MyFormatter.defaultNamedToTextList(players))
        .append(" : \n\n");
    for (final Territory t : territories) {
      final List<Unit> ownedUnits = t.getMatches(Matches.unitIsOwnedByAnyOf(players));
      // see if there's a flag
      final Optional<TerritoryAttachment> optionalTerritoryAttachment = TerritoryAttachment.get(t);
      final boolean hasFlag =
          optionalTerritoryAttachment.isPresent()
              && !t.getOwner().isNull()
              && players.contains(t.getOwner())
              && (optionalTerritoryAttachment.get().getOriginalOwner().isEmpty()
                  || !players.contains(optionalTerritoryAttachment.get().getOriginalOwner().get()));
      if (hasFlag || !ownedUnits.isEmpty()) {
        stringBuilder.append("    ").append(t.getName()).append(" : ");
        if (hasFlag && ownedUnits.isEmpty()) {
          stringBuilder.append("1 flag").append('\n');
        } else if (hasFlag) {
          stringBuilder.append("1 flag, ");
        }
        if (!ownedUnits.isEmpty()) {
          stringBuilder.append(MyFormatter.unitsToTextNoOwner(ownedUnits)).append('\n');
        }
      }
    }
    stringBuilder.append('\n');
    stringBuilder.append('\n');
    textArea.setText(stringBuilder.toString());
  }

  public void printDiceStatistics(final GameData data, final IRandomStats randomStats) {
    final RandomStatsDetails stats = randomStats.getRandomStats(data.getDiceSides());
    final String diceStats = stats.getAllStatsString();
    if (diceStats.length() > 0) {
      stringBuilder.append(diceStats).append('\n').append('\n').append('\n');
    }
    textArea.setText(stringBuilder.toString());
  }

  /** Adds a production summary for each player in the game to the log. */
  public void printProductionSummary(final GameData data) {
    final Collection<GamePlayer> players;
    final Resource pus;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      pus = data.getResourceList().getResource(Constants.PUS);
      players = data.getPlayerList().getPlayers();
    }
    if (pus == null) {
      return;
    }
    stringBuilder.append("Production/PUs Summary :\n").append('\n');
    for (final GamePlayer player : players) {
      final int pusQuantity = player.getResources().getQuantity(pus);
      final int production = getProduction(player, data);
      stringBuilder
          .append("    ")
          .append(player.getName())
          .append(" : ")
          .append(production)
          .append(" / ")
          .append(pusQuantity)
          .append('\n');
    }
    stringBuilder.append('\n').append('\n');
    textArea.setText(stringBuilder.toString());
  }

  private static int getProduction(final GamePlayer player, final GameState data) {
    int production = 0;
    for (final Territory place : data.getMap().getTerritories()) {
      boolean isConvoyOrLand = false;
      final int terrProduction =
          TerritoryAttachment.get(place).map(TerritoryAttachment::getProduction).orElse(0);
      if (!place.isWater()
          || (!data.getPlayerList()
                  .getNullPlayer()
                  .equals(OriginalOwnerTracker.getOriginalOwner(place))
              && player.equals(OriginalOwnerTracker.getOriginalOwner(place))
              && place.isOwnedBy(player))) {
        isConvoyOrLand = true;
      }
      if (place.isOwnedBy(player) && isConvoyOrLand && terrProduction > 0) {
        production += terrProduction;
      }
    }
    return production;
  }
}
