package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.delegate.scoring.ScoringBonus;
import java.io.Serial;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NonNls;

/**
 * Defines how one player's operational score is tallied when the game is scored.
 *
 * <p>A player scores {@code pointsPerObjective} for each victory city it owns, plus any bonus that
 * holds. {@code suppliedOccupationBonus} pays when the player has a supplied land unit in at least
 * one of its territories; {@code enemyAbsentBonus} pays when no enemy has a supplied land unit in
 * any of its territories. Both are written {@code points:territory:territory:...} and may be
 * repeated.
 */
public final class SmallFrontScoringAttachment extends DefaultAttachment {
  @Serial private static final long serialVersionUID = 1L;

  private int pointsPerObjective = 1;
  private final List<ScoringBonus> suppliedOccupationBonuses = new ArrayList<>();
  private final List<ScoringBonus> enemyAbsentBonuses = new ArrayList<>();
  private boolean winsTies;

  public SmallFrontScoringAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Optional<SmallFrontScoringAttachment> get(final GamePlayer player) {
    final List<SmallFrontScoringAttachment> attachments =
        player.getAttachments().values().stream()
            .filter(SmallFrontScoringAttachment.class::isInstance)
            .map(SmallFrontScoringAttachment.class::cast)
            .toList();
    if (attachments.size() > 1) {
      throw new IllegalStateException(
          "Player " + player.getName() + " has more than one scoring attachment");
    }
    return attachments.stream().findFirst();
  }

  public int getPointsPerObjective() {
    return pointsPerObjective;
  }

  @VisibleForTesting
  public void setPointsPerObjective(final String value) throws GameParseException {
    final int points = getInt(value);
    if (points < 0) {
      throw new GameParseException("pointsPerObjective must not be negative" + thisErrorMsg());
    }
    pointsPerObjective = points;
  }

  private void setPointsPerObjective(final Integer value) {
    pointsPerObjective = value;
  }

  private void resetPointsPerObjective() {
    pointsPerObjective = 1;
  }

  public List<ScoringBonus> getSuppliedOccupationBonus() {
    return List.copyOf(suppliedOccupationBonuses);
  }

  @VisibleForTesting
  public void setSuppliedOccupationBonus(final String value) throws GameParseException {
    suppliedOccupationBonuses.add(parseBonus("suppliedOccupationBonus", value));
  }

  private void replaceSuppliedOccupationBonuses(final List<ScoringBonus> values) {
    suppliedOccupationBonuses.clear();
    suppliedOccupationBonuses.addAll(values);
  }

  private void resetSuppliedOccupationBonus() {
    suppliedOccupationBonuses.clear();
  }

  public List<ScoringBonus> getEnemyAbsentBonus() {
    return List.copyOf(enemyAbsentBonuses);
  }

  @VisibleForTesting
  public void setEnemyAbsentBonus(final String value) throws GameParseException {
    enemyAbsentBonuses.add(parseBonus("enemyAbsentBonus", value));
  }

  private void replaceEnemyAbsentBonuses(final List<ScoringBonus> values) {
    enemyAbsentBonuses.clear();
    enemyAbsentBonuses.addAll(values);
  }

  private void resetEnemyAbsentBonus() {
    enemyAbsentBonuses.clear();
  }

  public boolean getWinsTies() {
    return winsTies;
  }

  @VisibleForTesting
  public void setWinsTies(final String value) {
    winsTies = getBool(value);
  }

  private void setWinsTies(final Boolean value) {
    winsTies = value;
  }

  private void resetWinsTies() {
    winsTies = false;
  }

  private ScoringBonus parseBonus(final String optionName, final String value)
      throws GameParseException {
    final String[] tokens = splitOnColon(value);
    if (tokens.length < 2) {
      throw new GameParseException(
          MessageFormat.format(
              "SmallFrontScoringAttachment: {0} must use points:territory:territory:...; got {1}{2}",
              optionName, value, thisErrorMsg()));
    }
    final int points = getInt(tokens[0]);
    if (points < 1) {
      throw new GameParseException(
          MessageFormat.format(
              "SmallFrontScoringAttachment: {0} points must be positive; got {1}{2}",
              optionName, value, thisErrorMsg()));
    }
    final List<Territory> territories = new ArrayList<>();
    for (int i = 1; i < tokens.length; i++) {
      final String territoryName = tokens[i].trim();
      final Territory territory =
          getTerritory(territoryName)
              .orElseThrow(
                  () ->
                      new GameParseException(
                          MessageFormat.format(
                              "SmallFrontScoringAttachment: No territory found for {0}; setting {1} not possible with value {2}{3}",
                              territoryName, optionName, value, thisErrorMsg())));
      if (!territories.contains(territory)) {
        territories.add(territory);
      }
    }
    return new ScoringBonus(points, territories);
  }

  @Override
  public void validate(final GameState data) throws GameParseException {
    if (!(getAttachedTo() instanceof GamePlayer)) {
      throw new GameParseException(
          "Scoring attachment must be attached to a player" + thisErrorMsg());
    }
  }

  @Override
  public Optional<MutableProperty<?>> getPropertyOrEmpty(@NonNls final String propertyName) {
    return switch (propertyName) {
      case "pointsPerObjective" ->
          Optional.of(
              MutableProperty.of(
                  this::setPointsPerObjective,
                  this::setPointsPerObjective,
                  this::getPointsPerObjective,
                  this::resetPointsPerObjective));
      case "suppliedOccupationBonus" ->
          Optional.of(
              MutableProperty.of(
                  this::replaceSuppliedOccupationBonuses,
                  this::setSuppliedOccupationBonus,
                  this::getSuppliedOccupationBonus,
                  this::resetSuppliedOccupationBonus));
      case "enemyAbsentBonus" ->
          Optional.of(
              MutableProperty.of(
                  this::replaceEnemyAbsentBonuses,
                  this::setEnemyAbsentBonus,
                  this::getEnemyAbsentBonus,
                  this::resetEnemyAbsentBonus));
      case "winsTies" ->
          Optional.of(
              MutableProperty.of(
                  this::setWinsTies, this::setWinsTies, this::getWinsTies, this::resetWinsTies));
      default -> Optional.empty();
    };
  }
}
