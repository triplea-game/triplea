package games.strategy.triplea.delegate.scoring;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.SmallFrontScoringAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tallies the operational score defined by each player's {@link SmallFrontScoringAttachment}.
 *
 * <p>This is a pure function of the game state, so the same tally drives both automatic termination
 * in {@link SmallFrontEndRoundDelegate} and the reinforcement-learning reward. A player counts as
 * holding a territory when it owns it, and a unit counts as supplied when its territory is supplied
 * for its owner. Supply is read positionally rather than from the supply tracker so that the score
 * does not depend on how far the turn sequence has advanced.
 */
public final class SmallFrontScoringService {
  public static final String AUTO_TERMINATION = "Auto Termination";
  public static final String SCORING_ROUND = "Scoring Round";
  public static final int DEFAULT_SCORING_ROUND = 8;

  private SmallFrontScoringService() {}

  public static boolean isAutoTerminationEnabled(final GameState data) {
    return data.getProperties().get(AUTO_TERMINATION, false);
  }

  public static int getScoringRound(final GameState data) {
    return Math.max(1, data.getProperties().get(SCORING_ROUND, DEFAULT_SCORING_ROUND));
  }

  /** Returns the score of every player that declares a scoring attachment, ordered by name. */
  public static Map<GamePlayer, Integer> score(final GameState data) {
    final Map<GamePlayer, Integer> scores = new LinkedHashMap<>();
    data.getPlayerList().getPlayers().stream()
        .sorted(Comparator.comparing(GamePlayer::getName))
        .forEach(
            player ->
                SmallFrontScoringAttachment.get(player)
                    .ifPresent(attachment -> scores.put(player, score(player, attachment, data))));
    return Map.copyOf(scores);
  }

  /**
   * Returns the single highest scorer, resolving a tie in favour of a player that declares {@code
   * winsTies}. Returns empty when nobody scores, or when a tie has no unique tie-break winner.
   */
  public static Optional<GamePlayer> winner(final GameState data) {
    final Map<GamePlayer, Integer> scores = score(data);
    if (scores.isEmpty()) {
      return Optional.empty();
    }
    final int best = scores.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
    final List<GamePlayer> leaders =
        scores.entrySet().stream()
            .filter(e -> e.getValue() == best)
            .map(Map.Entry::getKey)
            .toList();
    if (leaders.size() == 1) {
      return Optional.of(leaders.getFirst());
    }
    final List<GamePlayer> tieWinners =
        leaders.stream()
            .filter(
                player ->
                    SmallFrontScoringAttachment.get(player)
                        .map(SmallFrontScoringAttachment::getWinsTies)
                        .orElse(false))
            .toList();
    return tieWinners.size() == 1 ? Optional.of(tieWinners.getFirst()) : Optional.empty();
  }

  private static int score(
      final GamePlayer player, final SmallFrontScoringAttachment attachment, final GameState data) {
    int score = 0;
    for (final Territory territory : data.getMap().getTerritories()) {
      if (player.equals(territory.getOwner())) {
        score +=
            attachment.getPointsPerObjective()
                * TerritoryAttachment.get(territory)
                    .map(TerritoryAttachment::getVictoryCity)
                    .orElse(0);
      }
    }
    for (final ScoringBonus bonus : attachment.getSuppliedOccupationBonus()) {
      if (bonus.territories().stream()
          .anyMatch(territory -> hasSuppliedLandUnit(territory, player, data))) {
        score += bonus.points();
      }
    }
    for (final ScoringBonus bonus : attachment.getEnemyAbsentBonus()) {
      if (bonus.territories().stream()
          .noneMatch(territory -> hasSuppliedEnemyLandUnit(territory, player, data))) {
        score += bonus.points();
      }
    }
    return score;
  }

  private static boolean hasSuppliedEnemyLandUnit(
      final Territory territory, final GamePlayer viewer, final GameState data) {
    return data.getPlayerList().getPlayers().stream()
        .filter(candidate -> isEnemy(viewer, candidate, data))
        .anyMatch(enemy -> hasSuppliedLandUnit(territory, enemy, data));
  }

  private static boolean hasSuppliedLandUnit(
      final Territory territory, final GamePlayer owner, final GameState data) {
    return territory.anyUnitsMatch(Matches.unitIsLand().and(Matches.unitIsOwnedBy(owner)))
        && SupplyNetworkResolver.isSupplied(territory, owner, data);
  }

  private static boolean isEnemy(
      final GamePlayer viewer, final GamePlayer candidate, final GameState data) {
    if (viewer.equals(candidate) || candidate.isNull()) {
      return false;
    }
    final RelationshipTracker tracker = data.getRelationshipTracker();
    // A GameData assembled by a test may declare no relationship at all between two players.
    return tracker.getRelationship(viewer, candidate) != null && tracker.isAtWar(viewer, candidate);
  }
}
