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
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.swing.JDialog;
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
public class HistoryLog extends JDialog {
  static final String MORE_INDENT = "    ";

  @Serial private static final long serialVersionUID = 4880602702815333376L;
  private final JTextArea textArea;
  private final StringBuilder stringBuilder = new StringBuilder();
  private boolean verboseLog = false;

  public HistoryLog(final JFrame parent) {
    super(parent);
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
    // find Step node, if exists in this path
    while (curNode != null) {
      if (curNode instanceof Step stepNode) {
        final Collection<GamePlayer> players = new HashSet<>();
        if (playersAllowed != null) {
          players.addAll(playersAllowed);
        }
        final Optional<GamePlayer> optionalCurrentPlayer = stepNode.getPlayerId();
        if (players.isEmpty() && optionalCurrentPlayer.isPresent()) {
          players.add(optionalCurrentPlayer.get());
        }
        final Step turnStartNode = getFirstStepForTurn(stepNode, players);
        printRemainingTurn(turnStartNode, verbose, data.getDiceSides(), players);
        return;
      }
      curNode = (HistoryNode) curNode.getPreviousNode();
    }
    log.error("No step node found in!");
  }

  private static Step getFirstStepForTurn(
      final Step initialStepNode, Collection<GamePlayer> players) {
    Step stepNode = initialStepNode;
    while (true) {
      Step turnStartNode = stepNode;
      stepNode = (Step) stepNode.getPreviousSibling();
      if (stepNode == null) {
        return turnStartNode;
      }
      final Optional<GamePlayer> optionalStepNodePlayer = stepNode.getPlayerId();
      if (optionalStepNodePlayer.isEmpty() || !players.contains(optionalStepNodePlayer.get())) {
        return turnStartNode;
      }
    }
  }

  private static GamePlayer getPlayerId(final HistoryNode printNode) {
    DefaultMutableTreeNode curNode = printNode;
    final TreePath parentPath = new TreePath(printNode.getPath()).getParentPath();
    Optional<GamePlayer> optionalCurrentPlayerId = Optional.empty();
    if (parentPath != null) {
      final Object[] pathToNode = parentPath.getPath();
      for (final Object pathNode : pathToNode) {
        optionalCurrentPlayerId = getPlayerFromHistoryNode((HistoryNode) pathNode);
      }
    }
    do {
      final Enumeration<?> nodeEnum = curNode.preorderEnumeration();
      while (nodeEnum.hasMoreElements()) {
        final Optional<GamePlayer> optionalGamePlayerStep =
            getPlayerFromHistoryNode(
                (HistoryNode) nodeEnum.nextElement(),
                historyNode -> !historyNode.getTitle().equals("Initializing Delegates"));
        if (optionalGamePlayerStep.isPresent()) {
          optionalCurrentPlayerId = optionalGamePlayerStep;
        }
      }
      curNode = curNode.getNextSibling();
    } while ((curNode instanceof Step nodeStep)
        && nodeStep.getPlayerIdOrThrow().equals(optionalCurrentPlayerId.orElse(null)));
    return optionalCurrentPlayerId.orElseThrow(
        () -> new IllegalStateException("No player ID determined from steps"));
  }

  private static Optional<GamePlayer> getPlayerFromHistoryNode(HistoryNode node) {
    if (node instanceof Step nodeStep) {
      return nodeStep.getPlayerId();
    }
    return Optional.empty();
  }

  private static Optional<GamePlayer> getPlayerFromHistoryNode(
      HistoryNode node, Predicate<HistoryNode> predicate) {
    if (predicate.test(node)) {
      return getPlayerFromHistoryNode(node);
    }
    return Optional.empty();
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
    this.verboseLog = verbose;
    // print out the parent nodes
    final TreePath parentPath = new TreePath(printNode.getPath()).getParentPath();
    Optional<GamePlayer> optionalCurrentPlayer = Optional.empty();
    if (parentPath != null) {
      final Object[] pathToNode = parentPath.getPath();
      for (final Object pathNode : pathToNode) {
        final HistoryNode node = (HistoryNode) pathNode;
        stringBuilder.append(MORE_INDENT.repeat(Math.max(0, node.getLevel())));
        stringBuilder.append(node.getTitle()).append('\n');
        if (node.getLevel() == 0) {
          stringBuilder.append('\n');
        }
        if (node instanceof Step nodeStep) {
          optionalCurrentPlayer = nodeStep.getPlayerId();
        }
      }
    }
    final Collection<GamePlayer> players = new HashSet<>();
    if (playersAllowed != null) {
      players.addAll(playersAllowed);
    }
    optionalCurrentPlayer.ifPresent(players::add);
    fillStringBuilderForRemainingTurn(printNode, diceSides, players);
    textArea.setText(stringBuilder.toString());
  }

  StringBuilder conquerStr = new StringBuilder();

  private void fillStringBuilderForRemainingTurn(
      HistoryNode printNode, int diceSides, Collection<GamePlayer> players) {
    final List<String> moveList = new ArrayList<>();
    final Map<String, Double> hitDifferentialMap = new HashMap<>();
    final boolean moving =
        fillStringBuilderForEachTurnNode(
            diceSides, players, printNode, moveList, hitDifferentialMap);
    // if we are mid-phase, this might not get flushed
    if (moving && !moveList.isEmpty()) {
      final Iterator<String> moveIter = moveList.iterator();
      while (moveIter.hasNext()) {
        stringBuilder.append(moveIter.next()).append('\n');
        moveIter.remove();
      }
    }
    stringBuilder.append('\n');
    if (verboseLog) {
      stringBuilder.append("Combat Hit Differential Summary :\n\n");
      for (final var playerHitEntry : hitDifferentialMap.entrySet()) {
        stringBuilder
            .append(MORE_INDENT)
            .append(playerHitEntry.getKey())
            .append(" : ")
            .append(String.format("%.2f", playerHitEntry.getValue()))
            .append('\n');
      }
    }
    stringBuilder.append('\n');
  }

  private boolean fillStringBuilderForEachTurnNode(
      int diceSides,
      Collection<GamePlayer> players,
      DefaultMutableTreeNode turnNode,
      List<String> moveList,
      Map<String, Double> hitDifferentialMap) {
    boolean moving = false;
    do {
      // keep track of conquered territory during combat
      final Enumeration<?> nodeEnum = turnNode.preorderEnumeration();
      while (nodeEnum.hasMoreElements()) {
        final HistoryNode node = (HistoryNode) nodeEnum.nextElement();
        final String title = node.getTitle();
        final String indent = MORE_INDENT.repeat(Math.max(0, node.getLevel()));
        moving = flushMoveList(moveList, moving, node);
        if (node instanceof Renderable renderableNode) {
          moving =
              fillSbWithNodeRenderable(
                  diceSides, renderableNode, title, indent, hitDifferentialMap, moveList, moving);
        } else if (node instanceof Step nodeStep) {
          fillSbWithNodeStep(players, nodeStep, title, indent);
        } else if (node instanceof Round) {
          stringBuilder.append('\n').append(indent).append(title).append('\n');
        } else {
          stringBuilder.append(indent).append(title).append('\n');
        }
      }
      turnNode = turnNode.getNextSibling();
    } while (turnNode instanceof Step nodeStep
        && players.contains(nodeStep.getPlayerId().orElse(null)));
    return moving;
  }

  private boolean flushMoveList(List<String> moveList, boolean moving, HistoryNode node) {
    if (moving && !(node instanceof Renderable)) {
      final Iterator<String> moveIter = moveList.iterator();
      while (moveIter.hasNext()) {
        stringBuilder.append(moveIter.next()).append('\n');
        moveIter.remove();
      }
      moving = false;
    }
    return moving;
  }

  private void fillSbWithNodeStep(
      Collection<GamePlayer> players, Step nodeStep, String title, String indent) {
    if (!title.equals("Initializing Delegates")) {
      stringBuilder.append('\n').append(indent).append(title);
      final Optional<GamePlayer> optionalGamePlayer = nodeStep.getPlayerId();
      optionalGamePlayer.ifPresent(
          gamePlayer -> {
            players.add(gamePlayer);
            stringBuilder.append(" - ").append(gamePlayer.getName());
          });
      stringBuilder.append('\n');
    }
  }

  private boolean fillSbWithNodeRenderable(
      int diceSides,
      Renderable renderableNode,
      String title,
      String indent,
      Map<String, Double> hitDifferentialMap,
      List<String> moveList,
      boolean moving) {
    final Object details = renderableNode.getRenderingData();
    if (details instanceof DiceRoll diceRoll && verboseLog) {
      fillSbWithDetailsDiceRoll(diceSides, diceRoll, title, indent, hitDifferentialMap);
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
      fillSbWithDetailsCollection(details, title, indent);
    } else if (details instanceof Territory) {
      // territory details
      stringBuilder.append(indent).append(title).append('\n');
    } else if (details == null) {
      fillSbWithDetailsNull(title, indent, moving, moveList, conquerStr);
    } else {
      // unknown details object
      stringBuilder.append(indent).append(title).append('\n');
    }
    return moving;
  }

  /**
   * @param details details object to be printed
   * @param title current title
   * @param indent indent text to be used as prefix to line
   */
  private void fillSbWithDetailsCollection(Object details, String title, String indent) {

    @SuppressWarnings("unchecked")
    final Collection<Object> objects = (Collection<Object>) details;
    final Iterator<Object> objIter = objects.iterator();
    if (objIter.hasNext()) {
      final Object obj = objIter.next();
      if (obj instanceof Unit unit) {
        @SuppressWarnings("unchecked")
        final Collection<Unit> allUnitsInDetails = (Collection<Unit>) details;
        fillSbWithDetailsCollectionUnits(title, indent, unit, objIter, allUnitsInDetails);
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
  }

  private void fillSbWithDetailsCollectionUnits(
      String title,
      String indent,
      Unit unit,
      Iterator<Object> objIter,
      Collection<Unit> allUnitsInDetails) {
    // purchase/place units don't need details
    if (title.matches("\\w+ buy .*")
        || title.matches("\\w+ attack with .*")
        || title.matches("\\w+ defend with .*")) {
      stringBuilder.append(indent).append(title).append('\n');
    } else if (title.matches("\\d+ \\w+ owned by the .*? lost .*")
        || title.matches("\\d+ \\w+ owned by the .*? lost")) {
      if (verboseLog) {
        stringBuilder.append(indent).append(MORE_INDENT).append(title).append('\n');
      }
    } else if (title.startsWith("Battle casualty summary:")) {
      fillSbWithBattleSummary(title, indent, unit, objIter, allUnitsInDetails);
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
  }

  private void fillSbWithBattleSummary(
      String title,
      String indent,
      Unit unit,
      Iterator<Object> objIter,
      Collection<Unit> allUnitsInDetails) {
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
  }

  private void fillSbWithDetailsNull(
      String title,
      String indent,
      boolean moving,
      List<String> moveList,
      StringBuilder conquerStr) {
    if (titleNeedsFurtherProcessing(title)) {
      if (title.matches("\\w+ collect \\d+ PUs?.*")) {
        stringBuilder.append(indent).append(title).append('\n');
      } else if (title.matches("\\w+ takes? .*? from \\w+")) {
        // British take Libya from Germans
        if (moving) {
          final String str = moveList.remove(moveList.size() - 1);
          moveList.add(str + "\n  " + indent + title.replace(" takes ", " take "));
        } else {
          conquerStr.append(title.replaceAll("^\\w+ takes ", ", taking "));
        }
      } else if (title.matches("\\w+ spend \\d+ on tech rolls")) {
        stringBuilder.append(indent).append(title).append('\n');
      } else if (!title.startsWith("Rolls to resolve tech hits:")) {
        stringBuilder.append(indent).append(title).append('\n');
      }
    }
  }

  private void fillSbWithDetailsDiceRoll(
      int diceSides,
      DiceRoll diceRoll,
      String title,
      String indent,
      Map<String, Double> hitDifferentialMap) {
    final String diceMsg1 = title.substring(0, title.indexOf(':') + 1);
    if (diceMsg1.isEmpty()) {
      // tech roll
      stringBuilder.append(indent).append(MORE_INDENT).append(title).append('\n');
    } else {
      // dice roll
      stringBuilder.append(indent).append(MORE_INDENT).append(diceMsg1);
      final String hitDifferentialKey = parseHitDifferentialKeyFromDiceRollMessage(diceMsg1);
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
      final boolean isOwnerInPlayersAndAttachmentOriginalOwnerNot =
          !t.getOwner().isNull()
              && players.contains(t.getOwner())
              && isOriginalOwnerInPlayers(TerritoryAttachment.get(t).orElse(null), players);
      if (isOwnerInPlayersAndAttachmentOriginalOwnerNot || !ownedUnits.isEmpty()) {
        stringBuilder.append(MORE_INDENT).append(t.getName()).append(" : ");
        if (isOwnerInPlayersAndAttachmentOriginalOwnerNot && ownedUnits.isEmpty()) {
          stringBuilder.append("1 flag").append('\n');
        } else if (isOwnerInPlayersAndAttachmentOriginalOwnerNot) {
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

  private static boolean isOriginalOwnerInPlayers(
      @Nullable TerritoryAttachment territoryAttachment, Collection<GamePlayer> players) {
    if (territoryAttachment == null) {
      return false;
    }
    final Optional<GamePlayer> optionalOriginalOwner = territoryAttachment.getOriginalOwner();
    return (optionalOriginalOwner.isEmpty() || !players.contains(optionalOriginalOwner.get()));
  }

  public void printDiceStatistics(final GameData data, final IRandomStats randomStats) {
    final RandomStatsDetails stats = randomStats.getRandomStats(data.getDiceSides());
    final String diceStats = stats.getAllStatsString();
    if (!diceStats.isEmpty()) {
      stringBuilder.append(diceStats).append('\n').append('\n').append('\n');
    }
    textArea.setText(stringBuilder.toString());
  }

  /** Adds a production summary for each player in the game to the log. */
  public void printProductionSummary(final GameData data) {
    final Collection<GamePlayer> players;
    final Resource pus;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
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
          .append(MORE_INDENT)
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
      if (!place.isWater() || isPlayerOwnerAndOriginalOwnerOfLand(player, place)) {
        isConvoyOrLand = true;
      }
      if (place.isOwnedBy(player) && isConvoyOrLand && terrProduction > 0) {
        production += terrProduction;
      }
    }
    return production;
  }

  /**
   * @param territory {@link Territory} to be checked
   * @param player {@link GamePlayer} to be checked
   * @return {@code true} when player is not the NullPlayer and the same as the land territory's
   *     current owner as well as original owner
   */
  private static boolean isPlayerOwnerAndOriginalOwnerOfLand(
      GamePlayer player, Territory territory) {
    GamePlayer originalOwner = OriginalOwnerTracker.getOriginalOwner(territory).orElse(null);
    return !player.getData().getPlayerList().getNullPlayer().equals(originalOwner)
        && player.equals(originalOwner)
        && territory.isOwnedBy(player);
  }
}
