package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementRule;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NonNls;

/** Defines deterministic, economy-independent reinforcement deliveries for one player. */
public final class FixedReinforcementAttachment extends DefaultAttachment {
  @Serial private static final long serialVersionUID = 1L;

  private final List<FixedReinforcementRule> reinforcements = new ArrayList<>();

  public FixedReinforcementAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Optional<FixedReinforcementAttachment> get(final GamePlayer player) {
    final List<FixedReinforcementAttachment> attachments =
        player.getAttachments().values().stream()
            .filter(FixedReinforcementAttachment.class::isInstance)
            .map(FixedReinforcementAttachment.class::cast)
            .toList();
    if (attachments.size() > 1) {
      throw new IllegalStateException(
          "Player " + player.getName() + " has more than one fixed reinforcement attachment");
    }
    return attachments.stream().findFirst();
  }

  @VisibleForTesting
  public void setReinforcement(final String value) throws GameParseException {
    final String[] tokens = value.split(":", -1);
    if (tokens.length != 4) {
      throw new GameParseException(
          "reinforcement must use round:territory:unitType:quantity" + thisErrorMsg());
    }
    final int round = getInt(tokens[0]);
    final String territoryName = tokens[1].trim();
    final String unitTypeName = tokens[2].trim();
    final int quantity = getInt(tokens[3]);
    if (round < 1 || quantity < 1) {
      throw new GameParseException(
          "reinforcement round and quantity must be positive" + thisErrorMsg());
    }
    if (getData().getMap().getTerritoryOrNull(territoryName) == null) {
      throw new GameParseException(
          "reinforcement territory does not exist: " + territoryName + thisErrorMsg());
    }
    if (getData().getUnitTypeList().getUnitType(unitTypeName).isEmpty()) {
      throw new GameParseException(
          "reinforcement unit type does not exist: " + unitTypeName + thisErrorMsg());
    }
    reinforcements.add(new FixedReinforcementRule(round, territoryName, unitTypeName, quantity));
  }

  public List<FixedReinforcementRule> getReinforcements() {
    return List.copyOf(reinforcements);
  }

  private void replaceReinforcements(final List<FixedReinforcementRule> values) {
    reinforcements.clear();
    reinforcements.addAll(values);
  }

  private void resetReinforcement() {
    reinforcements.clear();
  }

  @Override
  public void validate(final GameState data) {}

  @Override
  public Optional<MutableProperty<?>> getPropertyOrEmpty(@NonNls final String propertyName) {
    if ("reinforcement".equals(propertyName)) {
      return Optional.of(
          MutableProperty.of(
              this::replaceReinforcements,
              this::setReinforcement,
              this::getReinforcements,
              this::resetReinforcement));
    }
    return Optional.empty();
  }
}
