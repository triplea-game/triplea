package games.strategy.engine.data.export;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.DefaultNamed;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.framework.ServerGame;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.TechAdvance;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.UtilityClass;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.java.collections.IntegerMap;
import org.triplea.map.data.elements.AttachmentList;
import org.triplea.map.data.elements.DiceSides;
import org.triplea.map.data.elements.Game;
import org.triplea.map.data.elements.GamePlay;
import org.triplea.map.data.elements.Info;
import org.triplea.map.data.elements.Initialize;
import org.triplea.map.data.elements.PlayerList;
import org.triplea.map.data.elements.Production;
import org.triplea.map.data.elements.PropertyList;
import org.triplea.map.data.elements.RelationshipTypes;
import org.triplea.map.data.elements.ResourceList;
import org.triplea.map.data.elements.Technology;
import org.triplea.map.data.elements.TerritoryEffectList;
import org.triplea.map.data.elements.Triplea;
import org.triplea.map.data.elements.UnitList;
import org.triplea.util.Tuple;

/** Exports a {@link GameData} instance in XML format. */
@UtilityClass
public class GameDataExporter {

  /**
   * Converts a 'GameData' object into a 'Game' object, the latter is a POJO that models XML and is
   * suitable to then be written to an XML string. Use this method to export live game data to XML.
   */
  public static Game convertToXmlModel(final GameData data, final Path existingMapXmlPath) {
    return Game.builder()
        .info(info(data))
        .triplea(
            Triplea.builder()
                .minimumVersion(ProductVersionReader.getCurrentVersion().toString())
                .build())
        .diceSides(DiceSides.builder().value(data.getDiceSides()).build())
        .map(map(data))
        .resourceList(resourceList(data))
        .playerList(playerList(data))
        .unitList(unitList(data))
        .relationshipTypes(relationshipTypeList(data))
        .territoryEffectList(territoryEffectList(data))
        .gamePlay(gamePlay(data))
        .production(
            Production.builder()
                .productionRules(productionRules(data))
                .repairRules(repairRules(data))
                .repairFrontiers(repairFrontiers(data))
                .productionFrontiers(productionFrontiers(data))
                .playerProductions(playerProduction(data))
                .playerRepairs(playerRepair(data))
                .build())
        .technology(
            Technology.builder()
                .technologies(technologies(data))
                .playerTechs(playertechs(data))
                .build())
        .attachmentList(attachments(existingMapXmlPath))
        .initialize(
            Initialize.builder()
                .ownerInitialize(ownerInitialize(data))
                .unitInitialize(unitInitialize(data))
                .resourceInitialize(resourceInitialize(data))
                .relationshipInitialize(relationshipInitialize(data))
                .build())
        .propertyList(propertyList(data))
        .build();
  }

  private static List<Technology.PlayerTech> playertechs(final GameState data) {
    final List<Technology.PlayerTech> playerTechs = new ArrayList<>();

    for (final GamePlayer player : data.getPlayerList()) {
      if (!player.getTechnologyFrontierList().getFrontiers().isEmpty()) {
        final var playerTechBuilder = Technology.PlayerTech.builder().player(player.getName());

        final List<Technology.PlayerTech.Category> categories = new ArrayList<>();
        for (final TechnologyFrontier frontier :
            player.getTechnologyFrontierList().getFrontiers()) {
          final var technologyFrontierBuilder =
              Technology.PlayerTech.Category.builder().name(frontier.getName());

          final List<Technology.PlayerTech.Category.Tech> techs = new ArrayList<>();
          for (final TechAdvance tech : frontier.getTechs()) {
            techs.add(
                Technology.PlayerTech.Category.Tech.builder()
                    .name(
                        TechAdvance.ALL_PREDEFINED_TECHNOLOGY_NAMES.contains(tech.getName())
                            ? tech.getProperty()
                            : tech.getName())
                    .build());
          }
          categories.add(technologyFrontierBuilder.techs(techs).build());
        }

        playerTechs.add(playerTechBuilder.categories(categories).build());
      }
    }
    return playerTechs;
  }

  private static Technology.Technologies technologies(final GameState data) {
    return Technology.Technologies.builder()
        .techNames(
            data.getTechnologyFrontier().getTechs().stream()
                .map(
                    tech ->
                        Technology.Technologies.TechName.builder()
                            .name(
                                TechAdvance.ALL_PREDEFINED_TECHNOLOGY_NAMES.contains(tech.getName())
                                    ? tech.getProperty()
                                    : tech.getName())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  private static PropertyList propertyList(final GameState data) {
    final List<PropertyList.Property> properties =
        data.getProperties().getConstantPropertiesByName().entrySet().stream()
            .map(entry -> mapToProperty(entry.getKey(), entry.getValue()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    properties.addAll(printEditableProperties(data.getProperties().getEditablePropertiesByName()));

    return PropertyList.builder().properties(properties).build();
  }

  private static List<PropertyList.Property> printEditableProperties(
      final Map<String, IEditableProperty<?>> editableProperties) {
    return editableProperties.values().stream()
        .map(GameDataExporter::printEditableProperty)
        .collect(Collectors.toList());
  }

  private static PropertyList.Property printEditableProperty(final IEditableProperty<?> prop) {
    final var propertyBuilder =
        PropertyList.Property.builder().editable(true).value(String.valueOf(prop.getValue()));

    if (prop.getClass().equals(NumberProperty.class)) {
      final NumberProperty numberProperty = (NumberProperty) prop;
      propertyBuilder.min(numberProperty.getMin()).max(numberProperty.getMax());
    }
    return propertyBuilder.build();
  }

  private static Optional<PropertyList.Property> mapToProperty(
      final String propertyName, final Object propertyValue) {
    switch (propertyName) {
      case "notes":
        // Special handling of notes property
        return Optional.of(
            PropertyList.Property.builder()
                .valueProperty(
                    PropertyList.Property.Value.builder().data((String) propertyValue).build())
                .build());
      case "EditMode":
      case "GAME_UUID":
      case ServerGame.GAME_HAS_BEEN_SAVED_PROPERTY:
        // Don't print these options
        return Optional.empty();
      default:
        return Optional.of(
            PropertyList.Property.builder()
                .name(propertyName)
                .value(Optional.ofNullable(propertyValue).map(String::valueOf).orElse(null))
                .build());
    }
  }

  @Nullable
  private static Initialize.RelationshipInitialize relationshipInitialize(final GameState data) {
    if (data.getRelationshipTypeList().getAllRelationshipTypes().size() <= 4) {
      return null;
    }
    final List<Initialize.RelationshipInitialize.Relationship> relationships = new ArrayList<>();

    final RelationshipTracker rt = data.getRelationshipTracker();
    final Collection<GamePlayer> players = data.getPlayerList().getPlayers();
    final Collection<GamePlayer> playersAlreadyDone = new HashSet<>();
    for (final GamePlayer p1 : players) {
      for (final GamePlayer p2 : players) {
        if (p1.equals(p2) || playersAlreadyDone.contains(p2)) {
          continue;
        }
        final RelationshipType type = rt.getRelationshipType(p1, p2);
        final int roundValue = rt.getRoundRelationshipWasCreated(p1, p2);

        relationships.add(
            Initialize.RelationshipInitialize.Relationship.builder()
                .type(type.getName())
                .player1(p1.getName())
                .player2(p2.getName())
                .roundValue(roundValue)
                .build());
      }
      playersAlreadyDone.add(p1);
    }
    return Initialize.RelationshipInitialize.builder().relationships(relationships).build();
  }

  private static Initialize.ResourceInitialize resourceInitialize(final GameState data) {
    final List<Initialize.ResourceInitialize.ResourceGiven> resourcesGiven = new ArrayList<>();

    for (final GamePlayer player : data.getPlayerList()) {
      for (final Resource resource : data.getResourceList().getResources()) {
        if (player.getResources().getQuantity(resource.getName()) > 0) {
          resourcesGiven.add(
              Initialize.ResourceInitialize.ResourceGiven.builder()
                  .player(player.getName())
                  .resource(resource.getName())
                  .quantity(player.getResources().getQuantity(resource.getName()))
                  .build());
        }
      }
    }
    return Initialize.ResourceInitialize.builder().resourcesGiven(resourcesGiven).build();
  }

  private static Initialize.UnitInitialize unitInitialize(final GameState data) {
    final List<Initialize.UnitInitialize.UnitPlacement> unitPlacements = new ArrayList<>();

    for (final Territory terr : data.getMap().getTerritories()) {
      final UnitCollection uc = terr.getUnitCollection();
      for (final GamePlayer player : uc.getPlayersWithUnits()) {
        final IntegerMap<UnitType> ucp = uc.getUnitsByType(player);
        for (final UnitType unit : ucp.keySet()) {

          unitPlacements.add(
              Initialize.UnitInitialize.UnitPlacement.builder()
                  .owner(
                      player == null || player.getName().equals(Constants.PLAYER_NAME_NEUTRAL)
                          ? null
                          : player.getName())
                  .unitType(unit.getName())
                  .territory(terr.getName())
                  .quantity(ucp.getInt(unit))
                  .build());
        }
      }
    }

    return Initialize.UnitInitialize.builder().unitPlacements(unitPlacements).build();
  }

  private static Initialize.OwnerInitialize ownerInitialize(final GameState data) {
    return Initialize.OwnerInitialize.builder()
        .territoryOwners(
            data.getMap().getTerritories().stream()
                .filter(terr -> !terr.getOwner().getName().equals(Constants.PLAYER_NAME_NEUTRAL))
                .map(
                    terr ->
                        Initialize.OwnerInitialize.TerritoryOwner.builder()
                            .territory(terr.getName())
                            .owner(terr.getOwner().getName())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  private List<Tuple<IAttachment, List<Tuple<String, String>>>> loadAttachmentOrderAndValues(
      Path existingMapXmlPath) {
    return GameParser.parse(existingMapXmlPath, true)
        .map(GameData::getAttachmentOrderAndValues)
        .orElse(List.of());
  }

  private static AttachmentList attachments(final Path existingMapXmlPath) {
    return AttachmentList.builder()
        .attachments(
            loadAttachmentOrderAndValues(existingMapXmlPath).stream()
                .map(GameDataExporter::printAttachments)
                .collect(Collectors.toList()))
        .build();
  }

  private static List<AttachmentList.Attachment.Option> printAttachmentOptionsBasedOnOriginalXml(
      final List<Tuple<String, String>> attachmentPlusValues) {
    return attachmentPlusValues.stream()
        .map(
            current ->
                AttachmentList.Attachment.Option.builder()
                    .name(current.getFirst())
                    .value(current.getSecond())
                    .build())
        .collect(Collectors.toList());
  }

  private static AttachmentList.Attachment printAttachments(
      final Tuple<IAttachment, List<Tuple<String, String>>> attachmentPlusValues) {

    final IAttachment attachment = attachmentPlusValues.getFirst();
    final NamedAttachable attachTo = (NamedAttachable) attachment.getAttachedTo();

    return AttachmentList.Attachment.builder()
        .name(attachment.getName())
        .attachTo(attachTo.getName())
        .javaClass(attachment.getClass().getCanonicalName())
        .type(determineAttachmentType(attachTo))
        .options(printAttachmentOptionsBasedOnOriginalXml(attachmentPlusValues.getSecond()))
        .build();
  }

  private static String determineAttachmentType(final NamedAttachable attachTo) {
    if (attachTo.getClass().equals(GamePlayer.class)) {
      return "player";
    } else if (attachTo.getClass().equals(UnitType.class)) {
      return "unitType";
    } else if (attachTo.getClass().equals(Territory.class)) {
      return "territory";
    } else if (attachTo.getClass().equals(TerritoryEffect.class)) {
      return "territoryEffect";
    } else if (attachTo.getClass().equals(Resource.class)) {
      return "resource";
    } else if (attachTo.getClass().equals(RelationshipType.class)) {
      return "relationship";
    } else if (TechAdvance.class.isAssignableFrom(attachTo.getClass())) {
      return "technology";
    } else {
      throw new AttachmentExportException(
          "no attachmentType known for " + attachTo.getClass().getCanonicalName());
    }
  }

  private static List<Production.RepairRule> repairRules(final GameState data) {

    return data.getRepairRules().getRepairRules().stream()
        .map(
            rr ->
                Production.RepairRule.builder()
                    .costs(
                        rr.getCosts().keySet().stream()
                            .map(
                                cost ->
                                    Production.ProductionRule.Cost.builder()
                                        .resource(cost.getName())
                                        .quantity(rr.getCosts().getInt(cost))
                                        .build())
                            .collect(Collectors.toList()))
                    .results(
                        rr.getResults().keySet().stream()
                            .map(
                                result ->
                                    Production.ProductionRule.Result.builder()
                                        .resourceOrUnit(result.getName())
                                        .quantity(rr.getResults().getInt(result))
                                        .build())
                            .collect(Collectors.toList()))
                    .build())
        .collect(Collectors.toList());
  }

  private static List<Production.RepairFrontier> repairFrontiers(final GameState data) {
    return data.getRepairFrontierList().getRepairFrontierNames().stream()
        .map(frontierName -> data.getRepairFrontierList().getRepairFrontier(frontierName))
        .map(
            frontier ->
                Production.RepairFrontier.builder()
                    .name(frontier.getName())
                    .repairRules(
                        frontier.getRules().stream()
                            .map(DefaultNamed::getName)
                            .map(
                                name ->
                                    Production.RepairFrontier.RepairRules.builder()
                                        .name(name)
                                        .build())
                            .collect(Collectors.toList()))
                    .build())
        .collect(Collectors.toList());
  }

  private static List<Production.PlayerRepair> playerRepair(final GameState data) {
    return data.getPlayerList().stream()
        .filter(player -> player.getRepairFrontier() != null)
        .filter(player -> player.getName() != null)
        .map(
            player ->
                Production.PlayerRepair.builder()
                    .player(player.getName())
                    .frontier(player.getRepairFrontier().getName())
                    .build())
        .collect(Collectors.toList());
  }

  private static List<Production.PlayerProduction> playerProduction(final GameState data) {
    return data.getPlayerList().stream()
        .filter(player -> player.getName() != null)
        .filter(player -> player.getProductionFrontier() != null)
        .map(
            player ->
                Production.PlayerProduction.builder()
                    .player(player.getName())
                    .frontier(player.getProductionFrontier().getName())
                    .build())
        .collect(Collectors.toList());
  }

  private static List<Production.ProductionFrontier> productionFrontiers(final GameState data) {
    return data.getProductionFrontierList().getProductionFrontierNames().stream()
        .map(frontierName -> data.getProductionFrontierList().getProductionFrontier(frontierName))
        .map(
            frontier ->
                Production.ProductionFrontier.builder()
                    .name(frontier.getName())
                    .frontierRules(
                        frontier.getRules().stream()
                            .map(DefaultNamed::getName)
                            .map(Production.ProductionFrontier.FrontierRules::new)
                            .collect(Collectors.toList()))
                    .build())
        .collect(Collectors.toList());
  }

  private static List<Production.ProductionRule> productionRules(final GameState data) {
    return data.getProductionRuleList().getProductionRules().stream()
        .map(
            productionRule ->
                Production.ProductionRule.builder()
                    .costs(
                        productionRule.getCosts().entrySet().stream()
                            .map(
                                costEntry ->
                                    Production.ProductionRule.Cost.builder()
                                        .resource(costEntry.getKey().getName())
                                        .quantity(costEntry.getValue())
                                        .build())
                            .collect(Collectors.toList()))
                    .results(
                        productionRule.getResults().entrySet().stream()
                            .map(
                                resultEntry ->
                                    Production.ProductionRule.Result.builder()
                                        .resourceOrUnit(resultEntry.getKey().getName())
                                        .quantity(resultEntry.getValue())
                                        .build())
                            .collect(Collectors.toList()))
                    .build())
        .collect(Collectors.toList());
  }

  private static GamePlay gamePlay(final GameData data) {
    final List<GamePlay.Delegate> delegates =
        data.getDelegates().stream()
            .filter(delegate -> !delegate.getName().equals("edit"))
            .map(
                delegate ->
                    GamePlay.Delegate.builder()
                        .name(delegate.getName())
                        .javaClass(delegate.getClass().getCanonicalName())
                        .display(delegate.getDisplayName())
                        .build())
            .collect(Collectors.toList());
    return GamePlay.builder()
        .delegates(delegates)
        .sequence(sequence(data))
        .offset(GamePlay.Offset.builder().round(data.getSequence().getRound() - 1).build())
        .build();
  }

  private static GamePlay.Sequence sequence(final GameState data) {
    final List<GamePlay.Sequence.Step> steps = new ArrayList<>();

    for (final GameStep step : data.getSequence()) {
      final var stepBuilder =
          GamePlay.Sequence.Step.builder()
              .name(step.getName())
              .delegate(step.getDelegate().getName());

      if (step.getPlayerId() != null) {
        stepBuilder.player(step.getPlayerId().getName());
      }
      if (step.getDisplayName() != null) {
        stepBuilder.display(step.getDisplayName());
      }
      if (step.getMaxRunCount() > -1) {
        int maxRun = step.getMaxRunCount();
        if (maxRun == 0) {
          maxRun = 1;
        }
        stepBuilder.maxRunCount(maxRun);
      }
      steps.add(stepBuilder.build());
    }

    return GamePlay.Sequence.builder().steps(steps).build();
  }

  private static UnitList unitList(final GameState data) {
    return UnitList.builder()
        .units(
            data.getUnitTypeList().stream()
                .map(DefaultNamed::getName)
                .map(name -> UnitList.Unit.builder().name(name).build())
                .collect(Collectors.toList()))
        .build();
  }

  private static PlayerList playerList(final GameState data) {
    return PlayerList.builder()
        .players(
            data.getPlayerList().getPlayers().stream()
                .map(
                    player ->
                        PlayerList.Player.builder()
                            .name(player.getName())
                            .optional(player.getOptional())
                            .isHidden(player.isHidden())
                            .canBeDisabled(player.getCanBeDisabled())
                            .build())
                .collect(Collectors.toList()))
        .alliances(
            data.getAllianceTracker().getAlliances().stream()
                .map(
                    allianceName ->
                        data.getAllianceTracker().getPlayersInAlliance(allianceName).stream()
                            .map(
                                player ->
                                    PlayerList.Alliance.builder()
                                        .player(player.getName())
                                        .alliance(allianceName)
                                        .build())
                            .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList()))
        .build();
  }

  @Nullable
  private static RelationshipTypes relationshipTypeList(final GameState data) {
    final Collection<RelationshipType> types =
        data.getRelationshipTypeList().getAllRelationshipTypes();
    if (types.size() <= 4) {
      return null;
    }

    final List<RelationshipTypes.RelationshipType> relationshipTypes = new ArrayList<>();
    for (final RelationshipType current : types) {
      final String name = current.getName();
      if (name.equals(Constants.RELATIONSHIP_TYPE_SELF)
          || name.equals(Constants.RELATIONSHIP_TYPE_NULL)
          || name.equals(Constants.RELATIONSHIP_TYPE_DEFAULT_WAR)
          || name.equals(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED)) {
        continue;
      }

      relationshipTypes.add(RelationshipTypes.RelationshipType.builder().name(name).build());
    }
    return RelationshipTypes.builder().relationshipTypes(relationshipTypes).build();
  }

  @Nullable
  private static TerritoryEffectList territoryEffectList(final GameState data) {
    final Collection<TerritoryEffect> types = data.getTerritoryEffectList().values();
    if (types.isEmpty()) {
      return null;
    }
    return TerritoryEffectList.builder()
        .territoryEffects(
            types.stream()
                .map(DefaultNamed::getName)
                .map(name -> TerritoryEffectList.TerritoryEffect.builder().name(name).build())
                .collect(Collectors.toList()))
        .build();
  }

  private static ResourceList resourceList(final GameState data) {
    return ResourceList.builder()
        .resources(
            data.getResourceList().getResources().stream()
                .map(GameDataExporter::convertResource)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()))
        .build();
  }

  private static List<ResourceList.Resource> convertResource(final Resource resource) {
    return resource.getPlayers().isEmpty()
        ? List.of(ResourceList.Resource.builder().name(resource.getName()).build())
        : resource.getPlayers().stream()
            .map(
                player ->
                    ResourceList.Resource.builder()
                        .name(resource.getName())
                        .isDisplayedFor(player.getName())
                        .build())
            .collect(Collectors.toList());
  }

  private static org.triplea.map.data.elements.Map map(final GameState data) {
    final List<org.triplea.map.data.elements.Map.Territory> territories =
        data.getMap().getTerritories().stream()
            .map(
                ter ->
                    org.triplea.map.data.elements.Map.Territory.builder()
                        .name(ter.getName())
                        .water(ter.isWater())
                        .build())
            .collect(Collectors.toList());

    connections(data);

    return org.triplea.map.data.elements.Map.builder()
        .territories(territories)
        .connections(connections(data))
        .build();
  }

  @AllArgsConstructor
  @EqualsAndHashCode
  @VisibleForTesting
  static final class Connection {
    private final Territory territory1;
    private final Territory territory2;
  }

  private static List<org.triplea.map.data.elements.Map.Connection> connections(
      final GameState data) {
    final List<org.triplea.map.data.elements.Map.Connection> connections = new ArrayList<>();

    final GameMap map = data.getMap();
    final Set<Connection> reverseConnectionTracker = new HashSet<>();

    for (final Territory ter : map.getTerritories()) {
      for (final Territory nb : map.getNeighbors(ter)) {
        if (!reverseConnectionTracker.contains(new Connection(ter, nb))) {
          connections.add(
              org.triplea.map.data.elements.Map.Connection.builder()
                  .t1(ter.getName())
                  .t2(nb.getName())
                  .build());
          reverseConnectionTracker.add(new Connection(ter, nb));
        }
      }
    }
    return connections;
  }

  private static Info info(final GameData data) {
    return Info.builder().name(data.getGameName()).build();
  }
}
