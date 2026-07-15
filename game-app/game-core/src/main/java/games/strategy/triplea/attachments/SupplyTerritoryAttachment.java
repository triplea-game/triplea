package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.gameparser.GameParseException;
import java.io.Serial;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NonNls;

/** Defines supply sources and road links for one land territory. */
public final class SupplyTerritoryAttachment extends DefaultAttachment {
  @Serial private static final long serialVersionUID = 1L;

  private boolean supplySource;
  private final List<Territory> roadConnections = new ArrayList<>();

  public SupplyTerritoryAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Optional<SupplyTerritoryAttachment> get(final Territory territory) {
    final List<SupplyTerritoryAttachment> attachments =
        territory.getAttachments().values().stream()
            .filter(SupplyTerritoryAttachment.class::isInstance)
            .map(SupplyTerritoryAttachment.class::cast)
            .toList();
    if (attachments.size() > 1) {
      throw new IllegalStateException(
          "Territory " + territory.getName() + " has more than one supply attachment");
    }
    return attachments.stream().findFirst();
  }

  public boolean getSupplySource() {
    return supplySource;
  }

  @VisibleForTesting
  public void setSupplySource(final String value) {
    supplySource = getBool(value);
  }

  private void setSupplySource(final Boolean value) {
    supplySource = value;
  }

  private void resetSupplySource() {
    supplySource = false;
  }

  public List<Territory> getRoadConnections() {
    return List.copyOf(roadConnections);
  }

  @VisibleForTesting
  public void setRoadConnection(final String value) throws GameParseException {
    if (value.isBlank()) {
      return;
    }
    for (final String territoryName : splitOnColon(value)) {
      final Territory territory =
          getTerritory(territoryName.trim())
              .orElseThrow(
                  () ->
                      new GameParseException(
                          MessageFormat.format(
                              "SupplyTerritoryAttachment: No territory found for {0}; Setting roadConnection not possible with value {1}",
                              territoryName, value)));
      if (!roadConnections.contains(territory)) {
        roadConnections.add(territory);
      }
    }
  }

  private void replaceRoadConnections(final List<Territory> values) {
    roadConnections.clear();
    roadConnections.addAll(values);
  }

  private void resetRoadConnection() {
    roadConnections.clear();
  }

  @Override
  public void validate(final GameState data) throws GameParseException {
    if (!(getAttachedTo() instanceof Territory territory)) {
      throw new GameParseException(
          "Supply attachment must be attached to a territory" + thisErrorMsg());
    }
    if (territory.isWater() && (supplySource || !roadConnections.isEmpty())) {
      throw new GameParseException("Supply sources and roads must be on land" + thisErrorMsg());
    }
    for (final Territory connected : roadConnections) {
      if (connected.equals(territory)) {
        throw new GameParseException(
            "A road may not connect a territory to itself" + thisErrorMsg());
      }
      if (connected.isWater()) {
        throw new GameParseException("Road connections must end on land" + thisErrorMsg());
      }
    }
  }

  @Override
  public Optional<MutableProperty<?>> getPropertyOrEmpty(@NonNls final String propertyName) {
    return switch (propertyName) {
      case "supplySource" ->
          Optional.of(
              MutableProperty.of(
                  this::setSupplySource,
                  this::setSupplySource,
                  this::getSupplySource,
                  this::resetSupplySource));
      case "roadConnection" ->
          Optional.of(
              MutableProperty.of(
                  this::replaceRoadConnections,
                  this::setRoadConnection,
                  this::getRoadConnections,
                  this::resetRoadConnection));
      default -> Optional.empty();
    };
  }
}
