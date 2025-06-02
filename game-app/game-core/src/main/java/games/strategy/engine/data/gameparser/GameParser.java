package games.strategy.engine.data.gameparser;

import static games.strategy.engine.framework.startup.ui.PlayerTypes.PLAYER_TYPE_HUMAN_LABEL;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import games.strategy.engine.data.AllianceTracker;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionFrontierList;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.RelationshipTypeList;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.RepairFrontierList;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Rule;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.TechnologyFrontierList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.StringProperty;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.TechAdvance;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;
import org.triplea.io.FileUtils;
import org.triplea.java.UrlStreams;
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
import org.triplea.map.data.elements.Technology;
import org.triplea.map.data.elements.TerritoryEffectList;
import org.triplea.map.data.elements.Triplea;
import org.triplea.map.data.elements.UnitList;
import org.triplea.map.description.file.MapDescriptionYaml;
import org.triplea.util.Tuple;
import org.triplea.util.Version;

/** Parses a game XML file into a {@link GameData} domain object. */
@Slf4j
public final class GameParser {
  @NonNls private static final String RESOURCE_IS_DISPLAY_FOR_NONE = "NONE";

  @Nonnull private final GameData data;
  private final Path xmlUri;
  private final XmlGameElementMapper xmlGameElementMapper;
  private GameDataVariables variables;
  private final Version engineVersion;
  private final boolean collectAttachmentOrderAndValues;

  private GameParser(
      final Path xmlUri,
      final XmlGameElementMapper xmlGameElementMapper,
      final Version engineVersion,
      final boolean collectAttachmentOrderAndValues) {
    data = new GameData();
    this.xmlUri = xmlUri;
    this.xmlGameElementMapper = xmlGameElementMapper;
    this.engineVersion = engineVersion;
    this.collectAttachmentOrderAndValues = collectAttachmentOrderAndValues;
  }

  /**
   * Performs a deep parse of the game XML file at the specified URI.
   *
   * @param xmlFile The game XML file to be parsed.
   * @return A complete {@link GameData} instance that can be used to play the game, otherwise
   *     returns empty if the file could not be parsed or is not valid.
   */
  public static Optional<GameData> parse(
      final Path xmlFile, boolean collectAttachmentOrderAndValues) {
    log.debug("Parsing game XML: {}", xmlFile.toAbsolutePath());
    final Optional<GameData> gameData =
        GameParser.parse(
            xmlFile,
            new XmlGameElementMapper(),
            ProductVersionReader.getCurrentVersion(),
            collectAttachmentOrderAndValues);

    // if parsed, find the 'map.yml' from a parent folder and set the 'mapName' property
    // using the 'map name' from 'map.yml'
    gameData.ifPresent(
        data ->
            FileUtils.findFileInParentFolders(xmlFile, MapDescriptionYaml.MAP_YAML_FILE_NAME)
                .flatMap(MapDescriptionYaml::fromFile)
                .ifPresent(
                    mapDescriptionYaml -> {
                      data.setGameName(mapDescriptionYaml.findGameNameFromXmlFileName(xmlFile));
                      data.setMapName(mapDescriptionYaml.getMapName());
                    }));

    return gameData;
  }

  @VisibleForTesting
  public static Optional<GameData> parse(
      final Path xmlFile,
      final XmlGameElementMapper xmlGameElementMapper,
      final Version engineVersion,
      final boolean collectAttachmentOrderAndValues) {
    return UrlStreams.openStream(
        xmlFile.toUri(),
        inputStream -> {
          try {
            return new GameParser(
                    xmlFile, xmlGameElementMapper, engineVersion, collectAttachmentOrderAndValues)
                .parse(xmlFile, inputStream);
          } catch (final EngineVersionException e) {
            log.warn("Game engine not compatible with: " + xmlFile, e);
            return null;
          } catch (final Exception e) {
            log.error("Could not parse:" + xmlFile + ", " + e.getMessage(), e);
            return null;
          }
        });
  }

  @Nonnull
  private GameData parse(final Path xmlFile, final InputStream stream)
      throws XmlParsingException, GameParseException, EngineVersionException {
    final Game game = new XmlMapper(stream).mapXmlToObject(Game.class);

    // test minimum engine version first
    if (!isEngineCompatibleWithMap(game.getTriplea())) {
      throw new EngineVersionException(game.getTriplea().getMinimumVersion(), xmlFile);
    }

    // For backward compatibility with maps that do not have a map.yml file,
    // set game name using data found in XML. Note, similar will be done for map
    // name when we load all properties into 'GameData'.
    Optional.ofNullable(game.getInfo()).map(Info::getName).ifPresent(data::setGameName);

    parseDiceSides(game.getDiceSides());
    parsePlayerList(game.getPlayerList());
    parseAlliances(game);

    Optional.ofNullable(game.getPropertyList()).ifPresent(this::parseProperties);

    parseTerritories(game.getMap().getTerritories());
    parseConnections(game.getMap().getConnections());

    final org.triplea.map.data.elements.ResourceList resourceList = game.getResourceList();
    if (resourceList != null) {
      parseResources(resourceList);
    }

    Optional.ofNullable(game.getUnitList()).ifPresent(this::parseUnits);

    Optional.ofNullable(game.getRelationshipTypes()).ifPresent(this::parseRelationshipTypes);

    Optional.ofNullable(game.getTerritoryEffectList()).ifPresent(this::parseTerritoryEffects);

    parseDelegates(game.getGamePlay().getDelegates());

    variables = GameDataVariables.parse(game.getVariableList());
    parseSteps(game.getGamePlay().getSequence().getSteps());

    Optional.ofNullable(game.getGamePlay().getOffset()).ifPresent(this::parseOffset);

    if (game.getProduction() != null) {
      parseProductionRules(game.getProduction().getProductionRules());
      parseProductionFrontiers(game.getProduction().getProductionFrontiers());
      parsePlayerProduction(game.getProduction().getPlayerProductions());
      parseRepairRules(game.getProduction().getRepairRules());
      parseRepairFrontiers(game.getProduction().getRepairFrontiers());
      parsePlayerRepair(game.getProduction().getPlayerRepairs());
    }

    if (game.getTechnology() != null) {
      parseTechnologies(game.getTechnology().getTechnologies());
      parsePlayerTech(game.getTechnology().getPlayerTechs());
    } else {
      TechAdvance.createDefaultTechAdvances(data);
    }

    parseAttachments(game.getAttachmentList());

    if (game.getInitialize() != null) {

      if (game.getInitialize().getOwnerInitialize() != null) {
        parseOwner(game.getInitialize().getOwnerInitialize());
      }

      if (game.getInitialize().getUnitInitialize() != null) {
        parseUnitPlacement(game.getInitialize().getUnitInitialize().getUnitPlacements());
        parseHeldUnits(game.getInitialize().getUnitInitialize().getHeldUnits());
      }
      if (game.getInitialize().getResourceInitialize() != null) {
        parseResourceInitialization(game.getInitialize().getResourceInitialize());
      }
      if (game.getInitialize().getRelationshipInitialize() != null) {
        parseRelationInitialize(game.getInitialize().getRelationshipInitialize());
      }
    }

    // set & override default relationships
    // sets the relationship between all players and the NullPlayer to NullRelation (with archeType
    // War)
    data.getRelationshipTracker().setNullPlayerRelations();
    // sets the relationship for all players with themselves to the SelfRelation (with archeType
    // Allied)
    data.getRelationshipTracker().setSelfRelations();
    // set default tech attachments (comes after we parse all technologies, parse all attachments,
    // and parse all game options/properties)
    TechAbilityAttachment.setDefaultTechnologyAttachments(data);

    return data;
  }

  private void parseDiceSides(final DiceSides diceSides) {
    data.setDiceSides(diceSides == null ? 6 : diceSides.getValue());
  }

  private boolean isEngineCompatibleWithMap(final Triplea tripleA) {

    return tripleA == null
        || tripleA.getMinimumVersion().isBlank()
        || engineVersion.isCompatibleWithMapMinimumEngineVersion(
            new Version(tripleA.getMinimumVersion()));
  }

  private GamePlayer getPlayerId(final String name) throws GameParseException {
    return getPlayerIdOptional(name)
        .orElseThrow(() -> new GameParseException("Could not find player name:" + name));
  }

  private Optional<GamePlayer> getPlayerIdOptional(final String name) {
    return Optional.ofNullable(data.getPlayerList().getPlayerId(name));
  }

  private RelationshipType getRelationshipType(final String name) throws GameParseException {
    return Optional.ofNullable(data.getRelationshipTypeList().getRelationshipType(name))
        .orElseThrow(() -> new GameParseException("Could not find relationship type:" + name));
  }

  private TerritoryEffect getTerritoryEffect(final String name) throws GameParseException {
    return Optional.ofNullable(data.getTerritoryEffectList().get(name))
        .orElseThrow(() -> new GameParseException("Could not find territoryEffect:" + name));
  }

  /** If the productionRule cannot be found an exception will be thrown. */
  private ProductionRule getProductionRule(final String name) throws GameParseException {
    return Optional.ofNullable(data.getProductionRuleList().getProductionRule(name))
        .orElseThrow(() -> new GameParseException("Could not find production rule:" + name));
  }

  /** If the repairRule cannot be found an exception will be thrown. */
  private RepairRule getRepairRule(final String name) throws GameParseException {
    return Optional.ofNullable(data.getRepairRules().getRepairRule(name))
        .orElseThrow(() -> new GameParseException("Could not find repair rule:" + name));
  }

  private Territory getTerritory(final String name) throws GameParseException {
    return Optional.ofNullable(data.getMap().getTerritory(name))
        .orElseThrow(() -> new GameParseException("Could not find territory:" + name));
  }

  private UnitType getUnitType(final String name) throws GameParseException {
    return data.getUnitTypeList().getUnitTypeOrThrow(name);
  }

  /** If mustfind is true and cannot find the unitType an exception will be thrown. */
  private Optional<UnitType> getUnitTypeOptional(final String name) {
    return data.getUnitTypeList().getUnitType(name);
  }

  private TechAdvance getTechnology(final String name) throws GameParseException {
    final TechnologyFrontier frontier = data.getTechnologyFrontier();
    return Optional.ofNullable(frontier.getAdvanceByName(name))
        .or(() -> Optional.ofNullable(frontier.getAdvanceByProperty(name)))
        .orElseThrow(() -> new GameParseException("Could not find technology:" + name));
  }

  /** If the Delegate cannot be found an exception will be thrown. */
  private IDelegate getDelegate(final String name) throws GameParseException {
    return Optional.ofNullable(data.getDelegate(name))
        .orElseThrow(() -> new GameParseException("Could not find delegate:" + name));
  }

  private Resource getResource(final String name) throws GameParseException {
    return getResourceOptional(name)
        .orElseThrow(() -> new GameParseException("Could not find resource:" + name));
  }

  /** If mustfind is true and cannot find the Resource an exception will be thrown. */
  private Optional<Resource> getResourceOptional(final String name) {
    return Optional.ofNullable(data.getResourceList().getResource(name));
  }

  /** If the productionFrontier cannot be found an exception will be thrown. */
  private ProductionFrontier getProductionFrontier(final String name) throws GameParseException {
    return Optional.ofNullable(data.getProductionFrontierList().getProductionFrontier(name))
        .orElseThrow(() -> new GameParseException("Could not find production frontier:" + name));
  }

  /** If the repairFrontier cannot be found an exception will be thrown. */
  private RepairFrontier getRepairFrontier(final String name) throws GameParseException {
    return Optional.ofNullable(data.getRepairFrontierList().getRepairFrontier(name))
        .orElseThrow(() -> new GameParseException("Could not find repair frontier:" + name));
  }

  private void parseTerritories(
      final List<org.triplea.map.data.elements.Map.Territory> territories) {
    for (final org.triplea.map.data.elements.Map.Territory current : territories) {
      boolean isWater = Optional.ofNullable(current.getWater()).orElse(false);
      data.getMap().addTerritory(new Territory(current.getName(), isWater, data));
    }
  }

  private void parseConnections(
      final List<org.triplea.map.data.elements.Map.Connection> connections)
      throws GameParseException {
    for (final org.triplea.map.data.elements.Map.Connection current : connections) {
      final Territory t1 = getTerritory(current.getT1());
      final Territory t2 = getTerritory(current.getT2());
      data.getMap().addConnection(t1, t2);
    }
  }

  private void parseResources(final org.triplea.map.data.elements.ResourceList resourceList)
      throws GameParseException {
    for (final org.triplea.map.data.elements.ResourceList.Resource resource :
        resourceList.getResources()) {
      final String name = resource.getName();
      final String isDisplayedFor = resource.getIsDisplayedFor();
      if (isDisplayedFor == null || isDisplayedFor.isEmpty()) {
        data.getResourceList()
            .addResource(new Resource(name, data, data.getPlayerList().getPlayers()));
      } else if (isDisplayedFor.equalsIgnoreCase(RESOURCE_IS_DISPLAY_FOR_NONE)) {
        data.getResourceList().addResource(new Resource(name, data));
      } else {
        data.getResourceList()
            .addResource(new Resource(name, data, parsePlayersFromIsDisplayedFor(isDisplayedFor)));
      }
    }
  }

  @VisibleForTesting
  List<GamePlayer> parsePlayersFromIsDisplayedFor(final String encodedPlayerNames)
      throws GameParseException {
    final List<GamePlayer> players = new ArrayList<>();
    for (final String playerName : Splitter.on(':').split(encodedPlayerNames)) {
      final @Nullable GamePlayer player = data.getPlayerList().getPlayerId(playerName);
      if (player == null) {
        throw new GameParseException("Parse resources could not find player: " + playerName);
      }
      players.add(player);
    }
    return players;
  }

  private void parseRelationshipTypes(final RelationshipTypes relationshipTypes) {
    relationshipTypes.getRelationshipTypes().stream()
        .map(RelationshipTypes.RelationshipType::getName)
        .map(name -> new RelationshipType(name, data))
        .forEach(data.getRelationshipTypeList()::addRelationshipType);
  }

  private void parseTerritoryEffects(final TerritoryEffectList territoryEffectList) {
    territoryEffectList.getTerritoryEffects().stream()
        .map(TerritoryEffectList.TerritoryEffect::getName)
        .forEach(name -> data.getTerritoryEffectList().put(name, new TerritoryEffect(name, data)));
  }

  private void parseUnits(final UnitList unitList) {
    unitList.getUnits().stream()
        .map(UnitList.Unit::getName)
        .map(name -> new UnitType(name, data))
        .forEach(data.getUnitTypeList()::addUnitType);
  }

  private void parsePlayerList(final PlayerList playerListData) {
    playerListData
        .getPlayers()
        .forEach(
            current ->
                data.getPlayerList()
                    .addPlayerId(
                        new GamePlayer(
                            current.getName(),
                            Optional.ofNullable(current.getOptional()).orElse(false),
                            Optional.ofNullable(current.getCanBeDisabled()).orElse(false),
                            Optional.ofNullable(current.getDefaultType())
                                .orElse(PLAYER_TYPE_HUMAN_LABEL),
                            Optional.ofNullable(current.getIsHidden()).orElse(false),
                            data)));
  }

  private void parseAlliances(final Game game) throws GameParseException {
    final AllianceTracker allianceTracker = data.getAllianceTracker();
    final Collection<GamePlayer> players = data.getPlayerList().getPlayers();

    for (final PlayerList.Alliance current : game.getPlayerList().getAlliances()) {
      final GamePlayer p1 = getPlayerId(current.getPlayer());
      final String alliance = current.getAlliance();
      allianceTracker.addToAlliance(p1, alliance);
    }
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    final RelationshipTypeList relationshipTypeList = data.getRelationshipTypeList();
    // iterate through all players to get known allies and enemies
    for (final GamePlayer currentPlayer : players) {
      // start with all players as enemies
      // start with no players as allies
      final Set<GamePlayer> allies = allianceTracker.getAllies(currentPlayer);
      final Set<GamePlayer> enemies = new HashSet<>(players);
      enemies.removeAll(allies);

      // remove self from enemies list (in case of free-for-all)
      enemies.remove(currentPlayer);
      // remove self from allies list (in case you are a member of an alliance)
      allies.remove(currentPlayer);
      // At this point enemies and allies should be set for this player.
      for (final GamePlayer alliedPLayer : allies) {
        relationshipTracker.setRelationship(
            currentPlayer, alliedPLayer, relationshipTypeList.getDefaultAlliedRelationship());
      }
      for (final GamePlayer enemyPlayer : enemies) {
        relationshipTracker.setRelationship(
            currentPlayer, enemyPlayer, relationshipTypeList.getDefaultWarRelationship());
      }
    }
  }

  private void parseRelationInitialize(final Initialize.RelationshipInitialize relations)
      throws GameParseException {
    if (!relations.getRelationships().isEmpty()) {
      final RelationshipTracker tracker = data.getRelationshipTracker();
      for (final Initialize.RelationshipInitialize.Relationship current :
          relations.getRelationships()) {
        final GamePlayer p1 = getPlayerId(current.getPlayer1());
        final GamePlayer p2 = getPlayerId(current.getPlayer2());
        final RelationshipType r = getRelationshipType(current.getType());
        final int roundValue = current.getRoundValue();
        tracker.setRelationship(p1, p2, r, roundValue);
      }
    }
  }

  private void parseProperties(final PropertyList propertyList) {
    final GameProperties properties = data.getProperties();

    for (final PropertyList.Property current : propertyList.getProperties()) {
      if (current.getName() == null) {
        continue;
      }
      final String propertyName = LegacyPropertyMapper.mapPropertyName(current.getName());

      // Get the value from first the body text of a "value" child node
      // or get the value from the 'value' attribute of the current node.
      final String value =
          Optional.ofNullable(current.getValueProperty())
              .map(PropertyList.Property.Value::getData)
              .orElseGet(current::getValue);

      // Next, infer the type of property based on its value
      // and set the property  in game data properties.
      if (current.getEditable() == null || !current.getEditable()) {
        final Object castedValue = PropertyValueTypeInference.castToInferredType(value);
        properties.set(propertyName, castedValue);
      } else {
        final Class<?> dataType = PropertyValueTypeInference.inferType(value);

        if (dataType == Boolean.class) {
          properties.addEditableProperty(
              new BooleanProperty(propertyName, null, Boolean.parseBoolean(value)));
        } else if (dataType == Integer.class) {
          final int min =
              Optional.ofNullable(current.getMin())
                  .or(
                      () ->
                          Optional.ofNullable(current.getNumberProperty())
                              .map(PropertyList.Property.XmlNumberTag::getMin))
                  .orElse(Integer.MIN_VALUE);

          final int max =
              Optional.ofNullable(current.getMax())
                  .or(
                      () ->
                          Optional.ofNullable(current.getNumberProperty())
                              .map(PropertyList.Property.XmlNumberTag::getMax))
                  .orElse(Integer.MAX_VALUE);

          properties.addEditableProperty(
              new NumberProperty(
                  propertyName, null, max, min, value == null ? 0 : Integer.parseInt(value)));
        } else {
          properties.addEditableProperty(new StringProperty(propertyName, null, value));
        }
      }
    }
    data.getPlayerList()
        .forEach(
            playerId ->
                data.getProperties()
                    .addPlayerProperty(
                        new NumberProperty(
                            Constants.getIncomePercentageFor(playerId), null, 999, 0, 100)));
    data.getPlayerList()
        .forEach(
            playerId ->
                data.getProperties()
                    .addPlayerProperty(
                        new NumberProperty(Constants.getPuIncomeBonus(playerId), null, 999, 0, 0)));
  }

  private void parseOffset(final GamePlay.Offset offset) {
    data.getSequence().setRoundOffset(Optional.ofNullable(offset.getRound()).orElse(0));
  }

  private void parseDelegates(final List<GamePlay.Delegate> delegateList)
      throws GameParseException {
    for (final GamePlay.Delegate current : delegateList) {
      // load the class
      final String className = current.getJavaClass();
      final IDelegate delegate =
          xmlGameElementMapper
              .newDelegate(className)
              .orElseThrow(
                  () -> new GameParseException("Class <" + className + "> is not a delegate."));
      final String name = current.getName();
      String displayName = current.getDisplay();
      if (displayName == null) {
        displayName = name;
      }
      delegate.initialize(name, displayName);
      data.addDelegate(delegate);
    }
  }

  private void parseSteps(final List<GamePlay.Sequence.Step> stepList) throws GameParseException {
    for (final GamePlay.Sequence.Step current : stepList) {
      final IDelegate delegate = getDelegate(current.getDelegate());
      final GamePlayer player = getPlayerIdOptional(current.getPlayer()).orElse(null);
      if (player == null && current.getPlayer() != null && !current.getPlayer().isBlank()) {
        throw new GameParseException(
            "The step "
                + current.getName()
                + " wants a player with the name of '"
                + current.getPlayer()
                + "' but that player can not be found. "
                + "Make sure the player's name is spelled correctly.");
      }
      final String name = current.getName();
      String displayName = null;
      final Properties stepProperties = parseStepProperties(current.getStepProperties());
      if (current.getDisplay() == null || !current.getDisplay().isBlank()) {
        displayName = current.getDisplay();
      }
      final GameStep step = new GameStep(name, displayName, player, delegate, data, stepProperties);
      if (current.getMaxRunCount() != null && current.getMaxRunCount() > 0) {
        step.setMaxRunCount(current.getMaxRunCount());
      }
      data.getSequence().addStep(step);
    }
  }

  private Properties parseStepProperties(
      final List<GamePlay.Sequence.Step.StepProperty> properties) {
    final Properties stepProperties = new Properties();
    for (final GamePlay.Sequence.Step.StepProperty p : properties) {
      stepProperties.setProperty(p.getName(), p.getValue());
    }
    return stepProperties;
  }

  private void parseProductionRules(final List<Production.ProductionRule> elements)
      throws GameParseException {
    for (final Production.ProductionRule current : elements) {
      final String name = current.getName();
      final ProductionRule rule = new ProductionRule(name, data);
      parseCosts(rule, current.getCosts());
      parseResults(rule, current);
      data.getProductionRuleList().addProductionRule(rule);
    }
  }

  private void parseRepairRules(final List<Production.RepairRule> elements)
      throws GameParseException {
    for (final Production.RepairRule current : elements) {
      final RepairRule rule = new RepairRule(current.getName(), data);
      parseRepairCosts(rule, current.getCosts());
      parseResults(rule, current);
      data.getRepairRules().addRepairRule(rule);
    }
  }

  private void parseCosts(
      final ProductionRule rule, final List<Production.ProductionRule.Cost> elements)
      throws GameParseException {
    if (elements.isEmpty()) {
      throw new GameParseException("no costs  for rule:" + rule.getName());
    }
    for (final Production.ProductionRule.Cost current : elements) {
      final Resource resource = getResource(current.getResource());
      final int quantity = Optional.ofNullable(current.getQuantity()).orElse(0);
      rule.addCost(resource, quantity);
    }
  }

  private void parseRepairCosts(
      final RepairRule rule, final List<Production.ProductionRule.Cost> elements)
      throws GameParseException {
    if (elements.isEmpty()) {
      throw new GameParseException("no costs  for rule:" + rule.getName());
    }
    for (final Production.ProductionRule.Cost current : elements) {
      final Resource resource = getResource(current.getResource());
      final int quantity = Optional.ofNullable(current.getQuantity()).orElse(0);
      rule.addCost(resource, quantity);
    }
  }

  private void parseResults(final Rule dataRule, final Production.Rule mapRule)
      throws GameParseException {
    List<Production.Rule.Result> ruleResults = mapRule.getRuleResults();
    if (ruleResults.isEmpty()) {
      throw new GameParseException(
          MessageFormat.format("No results for rule {0}", dataRule.getName()));
    }
    for (final Production.ProductionRule.Result current : ruleResults) {
      // must find either a resource or a unit with the given name
      final String resourceOrUnit = current.getResourceOrUnit();
      Optional<? extends NamedAttachable> result = getResourceOptional(resourceOrUnit);
      if (result.isEmpty()) {
        result = getUnitTypeOptional(resourceOrUnit);
      }
      if (result.isEmpty()) {
        throw new GameParseException("Could not find resource or unit " + resourceOrUnit);
      }
      final int quantity = Optional.ofNullable(current.getQuantity()).orElse(0);
      dataRule.addResult(result.get(), quantity);
    }
  }

  private void parseProductionFrontiers(final List<Production.ProductionFrontier> elements)
      throws GameParseException {
    final ProductionFrontierList frontiers = data.getProductionFrontierList();
    for (final Production.ProductionFrontier current : elements) {
      final String name = current.getName();
      final ProductionFrontier frontier = new ProductionFrontier(name, data);
      parseFrontierRules(current.getFrontierRules(), frontier);
      frontiers.addProductionFrontier(frontier);
    }
  }

  private void parseTechnologies(final Technology.Technologies element) {
    if (element == null) {
      return;
    }
    final TechnologyFrontier allTechs = data.getTechnologyFrontier();
    parseTechs(element.getTechNames(), allTechs);
  }

  private void parsePlayerTech(final List<Technology.PlayerTech> elements)
      throws GameParseException {
    for (final Technology.PlayerTech current : elements) {
      final GamePlayer player = getPlayerId(current.getPlayer());
      final TechnologyFrontierList categories = player.getTechnologyFrontierList();
      parseCategories(current.getCategories(), categories);
    }
  }

  private void parseCategories(
      final List<Technology.PlayerTech.Category> elements, final TechnologyFrontierList categories)
      throws GameParseException {
    for (final Technology.PlayerTech.Category current : elements) {
      final var technologyFrontier = new TechnologyFrontier(current.getName(), data);
      parseCategoryTechs(current.getTechs(), technologyFrontier);
      categories.addTechnologyFrontier(technologyFrontier);
    }
  }

  private void parseRepairFrontiers(final List<Production.RepairFrontier> elements)
      throws GameParseException {
    final RepairFrontierList frontiers = data.getRepairFrontierList();
    for (final Production.RepairFrontier current : elements) {
      final String name = current.getName();
      final RepairFrontier frontier = new RepairFrontier(name, data);
      parseRepairFrontierRules(current.getRepairRules(), frontier);
      frontiers.addRepairFrontier(frontier);
    }
  }

  private void parsePlayerProduction(final List<Production.PlayerProduction> elements)
      throws GameParseException {
    for (final Production.PlayerProduction current : elements) {
      final GamePlayer player = getPlayerId(current.getPlayer());
      final ProductionFrontier frontier = getProductionFrontier(current.getFrontier());
      player.setProductionFrontier(frontier);
    }
  }

  private void parsePlayerRepair(final List<Production.PlayerRepair> elements)
      throws GameParseException {
    for (final Production.PlayerRepair current : elements) {
      final GamePlayer player = getPlayerId(current.getPlayer());
      final RepairFrontier repairFrontier = getRepairFrontier(current.getFrontier());
      player.setRepairFrontier(repairFrontier);
    }
  }

  private void parseFrontierRules(
      final List<Production.ProductionFrontier.FrontierRules> elements,
      final ProductionFrontier frontier)
      throws GameParseException {
    for (final Production.ProductionFrontier.FrontierRules element : elements) {
      frontier.addRule(getProductionRule(element.getName()));
    }
  }

  private void parseTechs(
      final List<Technology.Technologies.TechName> elements,
      final TechnologyFrontier allTechsFrontier) {
    for (final Technology.Technologies.TechName current : elements) {
      final String name = current.getName();
      final String tech = current.getTech();
      TechAdvance ta;
      if (tech != null && !tech.isBlank()) {
        ta =
            new GenericTechAdvance(
                name, TechAdvance.findDefinedAdvanceAndCreateAdvance(tech, data), data);
      } else {
        try {
          ta = TechAdvance.findDefinedAdvanceAndCreateAdvance(name, data);
        } catch (final IllegalArgumentException e) {
          ta = new GenericTechAdvance(name, null, data);
        }
      }
      allTechsFrontier.addAdvance(ta);
    }
  }

  private void parseCategoryTechs(
      final List<Technology.PlayerTech.Category.Tech> elements, final TechnologyFrontier frontier)
      throws GameParseException {
    for (final Technology.PlayerTech.Category.Tech current : elements) {
      TechAdvance ta = data.getTechnologyFrontier().getAdvanceByProperty(current.getName());
      if (ta == null) {
        ta = data.getTechnologyFrontier().getAdvanceByName(current.getName());
      }
      if (ta == null) {
        throw new GameParseException("Technology not found :" + current.getName());
      }
      frontier.addAdvance(ta);
    }
  }

  private void parseRepairFrontierRules(
      final List<Production.RepairFrontier.RepairRules> elements, final RepairFrontier frontier)
      throws GameParseException {
    for (final Production.RepairFrontier.RepairRules element : elements) {
      frontier.addRule(getRepairRule(element.getName()));
    }
  }

  private void parseAttachments(final AttachmentList root) throws GameParseException {
    for (final AttachmentList.Attachment current : root.getAttachments()) {
      final String foreach = current.getForeach();
      if (foreach == null || foreach.isBlank()) {
        parseAttachment(current, Map.of());
      } else {
        final List<Map<String, String>> combinations =
            variables.expandVariableCombinations(foreach);
        for (Map<String, String> foreachMap : combinations) {
          parseAttachment(current, foreachMap);
        }
      }
    }
  }

  private void parseAttachment(
      final AttachmentList.Attachment current, final Map<String, String> foreach)
      throws GameParseException {
    final String className = current.getJavaClass();
    final Attachable attachable =
        findAttachment(current, Optional.ofNullable(current.getType()).orElse("unitType"), foreach);
    String name = variables.replaceForeachVariables(current.getName(), foreach);
    // Only replace if needed, as replaceAll() can be slow.
    if (name.contains("ttatchment")) {
      name = name.replaceAll("ttatchment", "ttachment");
    }
    final IAttachment attachment =
        xmlGameElementMapper
            .newAttachment(className, name, attachable, data)
            .orElseThrow(
                () ->
                    new GameParseException(
                        "Attachment of type " + className + " could not be instantiated"));
    // replace-all to automatically correct legacy (1.8) attachment spelling
    attachable.addAttachment(name, attachment);

    final List<Tuple<String, String>> attachmentOptionValues =
        setOptions(attachment, current.getOptions(), foreach);
    // keep a list of attachment references in the order they were added
    if (collectAttachmentOrderAndValues) {
      data.addToAttachmentOrderAndValues(Tuple.of(attachment, attachmentOptionValues));
    }
  }

  private Attachable findAttachment(
      final AttachmentList.Attachment element, final String type, final Map<String, String> foreach)
      throws GameParseException {
    final String attachTo = variables.replaceForeachVariables(element.getAttachTo(), foreach);
    switch (type) {
      case "unitType":
        return getUnitType(attachTo);
      case "territory":
        return getTerritory(attachTo);
      case "resource":
        return getResource(attachTo);
      case "territoryEffect":
        return getTerritoryEffect(attachTo);
      case "player":
        return getPlayerId(attachTo);
      case "relationship":
        return getRelationshipType(attachTo);
      case "technology":
        return getTechnology(attachTo);
      default:
        throw new GameParseException("Type not found to attach to: " + type);
    }
  }

  private List<Tuple<String, String>> setOptions(
      final IAttachment attachment,
      final List<AttachmentList.Attachment.Option> options,
      final Map<String, String> foreach)
      throws GameParseException {
    final List<Tuple<String, String>> results = new ArrayList<>();
    for (final AttachmentList.Attachment.Option option : options) {
      final String optionName = option.getName();
      final String value = option.getValue();
      if (optionName == null || value == null) {
        continue;
      }
      // decapitalize the property name for backwards compatibility
      final String name =
          LegacyPropertyMapper.mapLegacyOptionName(decapitalize(optionName)).intern();
      if (name.isEmpty()) {
        throw new GameParseException(
            "Option name with zero length for attachment: " + attachment.getName());
      }
      if (LegacyPropertyMapper.ignoreOptionName(name, value)) {
        continue;
      }
      final String count = option.getCount();
      @NonNls
      final String countAndValue = Strings.isNullOrEmpty(count) ? value : (count + ":" + value);
      if (containsEmptyForeachVariable(countAndValue, foreach)) {
        continue; // Skip adding option if contains empty foreach variable
      }
      final String valueWithForeach = variables.replaceForeachVariables(countAndValue, foreach);
      final String interpolatedValue = variables.replaceVariables(valueWithForeach);
      final String finalValue =
          LegacyPropertyMapper.mapLegacyOptionValue(name, interpolatedValue).intern();
      try {
        attachment
            .getProperty(name)
            .orElseThrow(
                () ->
                    new GameParseException(
                        String.format(
                            "Missing property definition for option '%s' in attachment '%s'",
                            name, attachment.getName())))
            .setValue(finalValue);
      } catch (final GameParseException e) {
        throw e;
      } catch (final Exception e) {
        throw new GameParseException(
            String.format(
                "map name: '%s', Unexpected Exception while setting values for attachment: %s, %s",
                xmlUri, attachment, e.getMessage()),
            e);
      }
      results.add(Tuple.of(name, finalValue));
    }
    return results;
  }

  private boolean containsEmptyForeachVariable(final String s, final Map<String, String> foreach) {
    for (final Entry<String, String> entry : foreach.entrySet()) {
      if ((entry.getValue() == null || entry.getValue().isEmpty()) && s.contains(entry.getKey())) {
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  static String decapitalize(final String value) {
    return ((!value.isEmpty()) ? value.substring(0, 1).toLowerCase(Locale.ROOT) : "")
        + ((value.length() > 1) ? value.substring(1) : "");
  }

  private void parseOwner(final Initialize.OwnerInitialize elements) throws GameParseException {
    for (final Initialize.OwnerInitialize.TerritoryOwner current : elements.getTerritoryOwners()) {
      final Territory territory = getTerritory(current.getTerritory());
      final GamePlayer owner = getPlayerId(current.getOwner());
      territory.setOwner(owner);
    }
  }

  private void parseUnitPlacement(final List<Initialize.UnitInitialize.UnitPlacement> elements)
      throws GameParseException {
    for (final Initialize.UnitInitialize.UnitPlacement current : elements) {
      final Territory territory = getTerritory(current.getTerritory());
      final UnitType type = getUnitType(current.getUnitType());
      final String ownerString = current.getOwner();
      final int hits = Optional.ofNullable(current.getHitsTaken()).orElse(0);
      if (hits < 0 || hits > type.getUnitAttachment().getHitPoints() - 1) {
        throw new GameParseException(
            "hitsTaken cannot be less than zero or greater than one less than total hitPoints");
      }

      final int unitDamage = Optional.ofNullable(current.getUnitDamage()).orElse(0);
      if (unitDamage < 0) {
        throw new GameParseException("unitDamage cannot be less than zero");
      }

      final GamePlayer owner;
      if (ownerString == null || ownerString.isBlank()) {
        owner = territory.getData().getPlayerList().getNullPlayer();
      } else {
        owner = getPlayerIdOptional(current.getOwner()).orElse(null);
      }
      final int quantity = current.getQuantity();
      territory.getUnitCollection().addAll(type.create(quantity, owner, false, hits, unitDamage));
    }
  }

  private void parseHeldUnits(final List<Initialize.UnitInitialize.HeldUnits> elements)
      throws GameParseException {
    for (final Initialize.UnitInitialize.HeldUnits current : elements) {
      final GamePlayer player = getPlayerId(current.getPlayer());
      final UnitType type = getUnitType(current.getUnitType());
      final int quantity = current.getQuantity();
      player.getUnitCollection().addAll(type.create(quantity, player));
    }
  }

  private void parseResourceInitialization(final Initialize.ResourceInitialize elements)
      throws GameParseException {
    for (final Initialize.ResourceInitialize.ResourceGiven current : elements.getResourcesGiven()) {
      final GamePlayer player = getPlayerId(current.getPlayer());
      final Resource resource = getResource(current.getResource());
      final int quantity = current.getQuantity();
      player.getResources().addResource(resource, quantity);
    }
  }
}
