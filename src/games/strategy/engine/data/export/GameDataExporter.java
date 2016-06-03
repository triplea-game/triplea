package games.strategy.engine.data.export;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.ComboProperty;
import games.strategy.engine.data.properties.FileProperty;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.StringProperty;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

public class GameDataExporter {
  private final StringBuffer xmlfile;

  public GameDataExporter(final GameData data, final boolean currentAttachmentObjects) {
    xmlfile = new StringBuffer();
    init(data);
    tripleaMinimumVersion();
    diceSides(data);
    map(data);
    resourceList(data);
    playerList(data);
    unitList(data);
    relationshipTypeList(data);
    territoryEffectList(data);
    gamePlay(data);
    production(data);
    technology(data);
    attachments(data, currentAttachmentObjects);
    initialize(data);
    propertyList(data);
    finish();
  }

  private void tripleaMinimumVersion() {
    // Since we do not keep the minimum version info in the game data, just put the current version of triplea here
    // (since we have
    // successfully started the map, it is basically correct)
    xmlfile.append("    <triplea minimumVersion=\"").append(ClientContext.engineVersion()).append("\"/>\n");
  }

  private void diceSides(final GameData data) {
    final int diceSides = data.getDiceSides();
    xmlfile.append("    <diceSides value=\"").append(diceSides).append("\"/>\n");
  }

  private void technology(final GameData data) {
    final String technologies = technologies(data);
    final String playerTechs = playertechs(data);
    if (technologies.length() > 0 || playerTechs.length() > 0) {
      xmlfile.append("    <technology>\n");
      xmlfile.append(technologies);
      xmlfile.append(playerTechs);
      xmlfile.append("    </technology>\n");
    }
  }

  private String playertechs(final GameData data) {
    final StringBuffer returnValue = new StringBuffer();
    for(PlayerID player : data.getPlayerList()) {
      if (player.getTechnologyFrontierList().getFrontiers().size() > 0) {
        returnValue.append("        <playerTech player=\"").append(player.getName()).append("\">\n");
        for(TechnologyFrontier frontier : player.getTechnologyFrontierList().getFrontiers()) {
          returnValue.append("            <category name=\"").append(frontier.getName()).append("\">\n");
          for(TechAdvance tech : frontier.getTechs()) {
            String name = tech.getName();
            final String cat = tech.getProperty();
            for (final String definedName : TechAdvance.s_allPreDefinedTechnologyNames) {
              if (definedName.equals(name)) {
                name = cat;
              }
            }
            returnValue.append("                <tech name=\"").append(name).append("\"/>\n");
          }
          returnValue.append("            </category>\n");
        }
        returnValue.append("        </playerTech>\n");
      }
    }
    return returnValue.toString();
  }

  private String technologies(final GameData data) {
    final StringBuffer returnValue = new StringBuffer();
    if (data.getTechnologyFrontier().getTechs().size() > 0) {
      returnValue.append("        <technologies>\n");
      for(TechAdvance tech : data.getTechnologyFrontier().getTechs()) {
        String name = tech.getName();
        final String cat = tech.getProperty();
        // definedAdvances are handled differently by gameparser, they are set in xml with the category as the name but
        // stored in java with the normal category and name, this causes an xml bug when exporting.
        for (final String definedName : TechAdvance.s_allPreDefinedTechnologyNames) {
          if (definedName.equals(name)) {
            name = cat;
          }
        }
        returnValue.append("            <techname name=\"").append(name).append("\"");
        if (!name.equals(cat)) {
          returnValue.append(" tech=\"").append(cat).append("\" ");
        }
        returnValue.append("/>\n");
      }
      returnValue.append("        </technologies>\n");
    }
    return returnValue.toString();
  }

  @SuppressWarnings("unchecked")
  private void propertyList(final GameData data) {// TODO: Unchecked Reflection
    xmlfile.append("    <propertyList>\n");
    final GameProperties gameProperties = data.getProperties();
    try {
      final Field conPropField = GameProperties.class.getDeclaredField("m_constantProperties"); // TODO: unchecked
                                                                                                // reflection
      conPropField.setAccessible(true);
      final Field edPropField = GameProperties.class.getDeclaredField("m_editableProperties");
      edPropField.setAccessible(true);
      printConstantProperties((Map<String, Object>) conPropField.get(gameProperties));
      printEditableProperties((Map<String, IEditableProperty>) edPropField.get(gameProperties));
    } catch (final SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
      ClientLogger.logError("An Error occured whilst trying trying to setup the Property List", e);
    }
    xmlfile.append("    </propertyList>\n");
  }

  private void printEditableProperties(final Map<String, IEditableProperty> edProperties) {
    for(String property : edProperties.keySet()) {
      printEditableProperty(edProperties.get(property));
    }
  }

  @SuppressWarnings("unchecked")
  private void printEditableProperty(final IEditableProperty prop) {
    String typeString = "";
    String value = "" + prop.getValue();
    if (prop.getClass().equals(BooleanProperty.class)) {
      typeString = "            <boolean/>\n";
    }
    if (prop.getClass().equals(FileProperty.class)) {
      typeString = "            <file/>\n";
    }
    if (prop.getClass().equals(StringProperty.class)) {
      typeString = "            <string/>\n";
    }
    if (prop.getClass().equals(ColorProperty.class)) {
      typeString = "            <color/>\n";
      value = "0x" + Integer.toHexString((((Integer) prop.getValue()).intValue())).toUpperCase();
    }
    if (prop.getClass().equals(ComboProperty.class)) {
      Field listField;
      try {
        listField = ComboProperty.class.getDeclaredField("m_possibleValues"); // TODO: unchecked reflection
        listField.setAccessible(true);
        typeString = "            <list>" + Joiner.on(',').join((List<String>) listField.get(prop)) + "</list>\n";
      } catch (final NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
        ClientLogger.logError("An Error occured whilst trying to print the Property \"" + value + "\"", e);
      }
    }
    if (prop.getClass().equals(NumberProperty.class)) {
      try {
        final Field maxField = NumberProperty.class.getDeclaredField("m_max"); // TODO: unchecked reflection
        final Field minField = NumberProperty.class.getDeclaredField("m_min");
        maxField.setAccessible(true);
        minField.setAccessible(true);
        final int max = maxField.getInt(prop);
        final int min = minField.getInt(prop);
        typeString = "            <number min=\"" + min + "\" max=\"" + max + "\"/>\n";
      } catch (final SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
        ClientLogger.logError("An Error occured whilst trying to print a Number-XML Tag", e);
      }
    }
    xmlfile.append("        <property name=\"").append(prop.getName()).append("\" value=\"").append(value)
        .append("\" editable=\"true\">\n");
    xmlfile.append(typeString);
    xmlfile.append("        </property>\n");
  }

  private void printConstantProperties(final Map<String, Object> conProperties) {
    for(String propName : conProperties.keySet()) {
      if (propName.equals("notes")) { // TODO: unchecked reflection
        // Special handling of notes property
        printNotes((String) conProperties.get(propName));
      } else if (propName.equals("EditMode") || propName.equals("GAME_UUID")
          || propName.equals("games.strategy.engine.framework.ServerGame.GameHasBeenSaved")) {
        // Don't print these options
      } else {
        printConstantProperty(propName, conProperties.get(propName));
      }
    }
  }

  private void printNotes(final String notes) {
    xmlfile.append("        <property name=\"notes\">\n");
    xmlfile.append("            <value>\n");
    xmlfile.append("            <![CDATA[\n");
    xmlfile.append(notes);
    xmlfile.append("]]>\n");
    xmlfile.append("            </value>\n");
    xmlfile.append("        </property>\n");
  }

  private void printConstantProperty(final String propName, final Object property) {
    xmlfile.append("        <property name=\"").append(propName).append("\" value=\"").append(property.toString())
        .append("\" editable=\"false\">\n");
    if (property.getClass().equals(java.lang.String.class)) {
      xmlfile.append("            <string/>\n");
    }
    if (property.getClass().equals(java.io.File.class)) {
      xmlfile.append("            <file/>\n");
    }
    if (property.getClass().equals(java.lang.Boolean.class)) {
      xmlfile.append("            <boolean/>\n");
    }
    xmlfile.append("        </property>\n");
  }

  private void initialize(final GameData data) {
    xmlfile.append("    <initialize>\n");
    ownerInitialize(data);
    unitInitialize(data);
    resourceInitialize(data);
    relationshipInitialize(data);
    xmlfile.append("    </initialize>\n");
  }

  private void relationshipInitialize(final GameData data) {
    if (data.getRelationshipTypeList().getAllRelationshipTypes().size() <= 4) {
      return;
    }
    final RelationshipTracker rt = data.getRelationshipTracker();
    xmlfile.append("        <relationshipInitialize>\n");
    final Collection<PlayerID> players = data.getPlayerList().getPlayers();
    final Collection<PlayerID> playersAlreadyDone = new HashSet<>();
    for (final PlayerID p1 : players) {
      for (final PlayerID p2 : players) {
        if (p1.equals(p2) || playersAlreadyDone.contains(p2)) {
          continue;
        }
        final RelationshipType type = rt.getRelationshipType(p1, p2);
        final int roundValue = rt.getRoundRelationshipWasCreated(p1, p2);
        xmlfile.append("            <relationship type=\"").append(type.getName()).append("\" player1=\"")
            .append(p1.getName()).append("\" player2=\"").append(p2.getName()).append("\" roundValue=\"")
            .append(roundValue).append("\"/>\n");
      }
      playersAlreadyDone.add(p1);
    }
    xmlfile.append("        </relationshipInitialize>\n");
  }

  private void resourceInitialize(final GameData data) {
    xmlfile.append("        <resourceInitialize>\n");
    for(PlayerID player : data.getPlayerList()) {
      for(Resource resource : data.getResourceList().getResources()){
        if (player.getResources().getQuantity(resource.getName()) > 0) {
          xmlfile.append("            <resourceGiven player=\"").append(player.getName()).append("\" resource=\"")
              .append(resource.getName()).append("\" quantity=\"")
              .append(player.getResources().getQuantity(resource.getName())).append("\"/>\n");
        }
      }
    }
    xmlfile.append("        </resourceInitialize>\n");
  }

  private void unitInitialize(final GameData data) {
    xmlfile.append("        <unitInitialize>\n");
    for(Territory terr : data.getMap().getTerritories()){
      final UnitCollection uc = terr.getUnits();
      for(PlayerID player : uc.getPlayersWithUnits()){
        final IntegerMap<UnitType> ucp = uc.getUnitsByType(player);
        for(UnitType unit : ucp.keySet()){
          if (player == null || player.getName().equals(Constants.PLAYER_NAME_NEUTRAL)) {
            xmlfile.append("            <unitPlacement unitType=\"").append(unit.getName()).append("\" territory=\"")
                .append(terr.getName()).append("\" quantity=\"").append(ucp.getInt(unit)).append("\"/>\n");
          } else {
            xmlfile.append("            <unitPlacement unitType=\"").append(unit.getName()).append("\" territory=\"")
                .append(terr.getName()).append("\" quantity=\"").append(ucp.getInt(unit)).append("\" owner=\"")
                .append(player.getName()).append("\"/>\n");
          }
        }
      }
    }
    xmlfile.append("        </unitInitialize>\n");
  }

  private void ownerInitialize(final GameData data) {
    xmlfile.append("        <ownerInitialize>\n");
    for(Territory terr : data.getMap().getTerritories()){
      if (!terr.getOwner().getName().equals(Constants.PLAYER_NAME_NEUTRAL)) {
        xmlfile.append("            <territoryOwner territory=\"").append(terr.getName()).append("\" owner=\"")
            .append(terr.getOwner().getName()).append("\"/>\n");
      }
    }
    xmlfile.append("        </ownerInitialize>\n");
  }

  private void attachments(final GameData data, final boolean currentAttachmentObjects) {
    xmlfile.append("\n");
    xmlfile.append("    <attachmentList>\n");
    for(Tuple<IAttachment, ArrayList<Tuple<String, String>>> attachment : data.getAttachmentOrderAndValues()){
      // TODO: use a ui switch to determine if we are printing the xml as it was created, or as it stands right now
      // (including changes to
      // the game data)
      final Tuple<IAttachment, ArrayList<Tuple<String, String>>> current = attachment;
      printAttachments(current, currentAttachmentObjects);
    }
    xmlfile.append("    </attachmentList>\n");
  }

  private String printAttachmentOptionsBasedOnOriginalXML(final ArrayList<Tuple<String, String>> attachmentPlusValues,
      final IAttachment attachment) {
    if (attachmentPlusValues.isEmpty()) {
      return "";
    }
    final StringBuffer sb = new StringBuffer("");
    boolean alreadyHasOccupiedTerrOf = false;
    for (final Tuple<String, String> current : attachmentPlusValues) {
      sb.append("            <option name=\"").append(current.getFirst()).append("\" value=\"")
          .append(current.getSecond()).append("\"/>\n");
      if (current.getFirst().equals("occupiedTerrOf")) {
        alreadyHasOccupiedTerrOf = true;
      }
    }
    // add occupiedTerrOf until we fix engine to only use originalOwner
    if (!alreadyHasOccupiedTerrOf && attachment instanceof games.strategy.triplea.attachments.TerritoryAttachment) {
      final games.strategy.triplea.attachments.TerritoryAttachment ta =
          (games.strategy.triplea.attachments.TerritoryAttachment) attachment;
      if (ta.getOriginalOwner() != null) {
        sb.append("            <option name=\"occupiedTerrOf\" value=\"").append(ta.getOriginalOwner().getName())
            .append("\"/>\n");
      }
    }
    return sb.toString();
  }

  private void printAttachments(final Tuple<IAttachment, ArrayList<Tuple<String, String>>> attachmentPlusValues,
      final boolean currentAttachmentObjects) {
    final IAttachment attachment = attachmentPlusValues.getFirst();
    try {
      // TODO: none of the attachment exporter classes have been updated since TripleA version 1.3.2.2
      final String attachmentOptions;
      if (currentAttachmentObjects) {
        final IAttachmentExporter exporter = AttachmentExporterFactory.getExporter(attachment);
        attachmentOptions = exporter.getAttachmentOptions(attachment);
      } else {
        attachmentOptions = printAttachmentOptionsBasedOnOriginalXML(attachmentPlusValues.getSecond(), attachment);
      }
      final NamedAttachable attachTo = (NamedAttachable) attachment.getAttachedTo();
      // TODO: keep this list updated
      String type = "";
      if (attachTo.getClass().equals(PlayerID.class)) {
        type = "player";
      }
      if (attachTo.getClass().equals(UnitType.class)) {
        type = "unitType";
      }
      if (attachTo.getClass().equals(Territory.class)) {
        type = "territory";
      }
      if (attachTo.getClass().equals(TerritoryEffect.class)) {
        type = "territoryEffect";
      }
      if (attachTo.getClass().equals(Resource.class)) {
        type = "resource";
      }
      if (attachTo.getClass().equals(RelationshipType.class)) {
        type = "relationship";
      }
      if (TechAdvance.class.isAssignableFrom(attachTo.getClass())) {
        type = "technology";
      }
      if (type.equals("")) {
        throw new AttachmentExportException("no attachmentType known for " + attachTo.getClass().getCanonicalName());
      }
      if (attachmentOptions.length() > 0) {
        xmlfile.append("        <attachment name=\"").append(attachment.getName()).append("\" attachTo=\"")
            .append(attachTo.getName()).append("\" javaClass=\"").append(attachment.getClass().getCanonicalName())
            .append("\" type=\"").append(type).append("\">\n");
        xmlfile.append(attachmentOptions);
        xmlfile.append("        </attachment>\n");
      }
    } catch (final Exception e) {
      ClientLogger.logError("An Error occured whilst trying to print the Attachment \"" + attachment.getName() + "\"",
          e);
    }
  }

  private void production(final GameData data) {
    xmlfile.append("\n");
    xmlfile.append("    <production>\n");
    productionRules(data);
    repairRules(data);
    repairFrontiers(data);
    productionFrontiers(data);
    playerProduction(data);
    playerRepair(data);
    xmlfile.append("    </production>\n");
  }

  private void repairRules(final GameData data) {
    for(RepairRule rr : data.getRepairRuleList().getRepairRules()){
      xmlfile.append("        <repairRule name=\"").append(rr.getName()).append("\">\n");
      for(Resource cost : rr.getCosts().keySet()){
        xmlfile.append("            <cost resource=\"").append(cost.getName()).append("\" quantity=\"")
            .append(rr.getCosts().getInt(cost)).append("\"/>\n");
      }
      for(NamedAttachable result : rr.getResults().keySet()){
        xmlfile.append("            <result resourceOrUnit=\"").append(result.getName()).append("\" quantity=\"")
            .append(rr.getResults().getInt(result)).append("\"/>\n");
      }
      xmlfile.append("        </repairRule>\n");
    }
  }

  private void repairFrontiers(final GameData data) {
    for(String frontierName : data.getRepairFrontierList().getRepairFrontierNames()) {
      final RepairFrontier frontier = data.getRepairFrontierList().getRepairFrontier(frontierName);
      xmlfile.append("\n");
      xmlfile.append("        <repairFrontier name=\"").append(frontier.getName()).append("\">\n");
      for(RepairRule rule : frontier.getRules()) {
        xmlfile.append("            <repairRules name=\"").append(rule.getName()).append("\"/>\n");
      }
      xmlfile.append("        </repairFrontier>\n");
    }
    xmlfile.append("\n");
  }

  private void playerRepair(final GameData data) {
    for(PlayerID player : data.getPlayerList()){
      try {
        final String playerRepair = player.getRepairFrontier().getName();
        final String playername = player.getName();
        xmlfile.append("        <playerRepair player=\"").append(playername).append("\" frontier=\"")
            .append(playerRepair).append("\"/>\n");
      } catch (final NullPointerException npe) {
        // neutral?
      }
    }
  }

  private void playerProduction(final GameData data) {
    for(PlayerID player : data.getPlayerList()){
      try {
        final String playerfrontier = player.getProductionFrontier().getName();
        final String playername = player.getName();
        xmlfile.append("        <playerProduction player=\"").append(playername).append("\" frontier=\"")
            .append(playerfrontier).append("\"/>\n");
      } catch (final NullPointerException npe) {
        // neutral?
      }
    }
  }

  private void productionFrontiers(final GameData data) {
    for(String frontierName : data.getProductionFrontierList().getProductionFrontierNames()){
      final ProductionFrontier frontier = data.getProductionFrontierList().getProductionFrontier(frontierName);
      xmlfile.append("\n");
      xmlfile.append("        <productionFrontier name=\"").append(frontier.getName()).append("\">\n");
      for(ProductionRule rule : frontier.getRules()){
        xmlfile.append("            <frontierRules name=\"").append(rule.getName()).append("\"/>\n");
      }
      xmlfile.append("        </productionFrontier>\n");
    }
    xmlfile.append("\n");
  }

  private void productionRules(final GameData data) {
    for(ProductionRule pr : data.getProductionRuleList().getProductionRules()){
      xmlfile.append("        <productionRule name=\"").append(pr.getName()).append("\">\n");
      for(Resource cost : pr.getCosts().keySet()){
        xmlfile.append("            <cost resource=\"").append(cost.getName()).append("\" quantity=\"")
            .append(pr.getCosts().getInt(cost)).append("\"/>\n");
      }
      for(NamedAttachable result : pr.getResults().keySet()){
        xmlfile.append("            <result resourceOrUnit=\"").append(result.getName()).append("\" quantity=\"")
            .append(pr.getResults().getInt(result)).append("\"/>\n");
      }
      xmlfile.append("        </productionRule>\n");
    }
  }

  private void gamePlay(final GameData data) {
    xmlfile.append("\n");
    xmlfile.append("    <gamePlay>\n");
    for(IDelegate delegate : data.getDelegateList()){
      if (!delegate.getName().equals("edit")) {
        xmlfile.append("        <delegate name=\"").append(delegate.getName()).append("\" javaClass=\"")
            .append(delegate.getClass().getCanonicalName()).append("\" display=\"").append(delegate.getDisplayName())
            .append("\"/>\n");
      }
    }
    sequence(data);
    xmlfile.append("        <offset round=\"").append(data.getSequence().getRound() - 1).append("\"/>\n");
    xmlfile.append("    </gamePlay>\n");
  }

  private void sequence(final GameData data) {
    xmlfile.append("\n");
    xmlfile.append("        <sequence>\n");
    for(GameStep step : data.getSequence()){
      try {
        final Field mDelegateField = GameStep.class.getDeclaredField("m_delegate"); // TODO: unchecked reflection
        mDelegateField.setAccessible(true);
        final String delegate = (String) mDelegateField.get(step);
        xmlfile.append("            <step name=\"").append(step.getName()).append("\" delegate=\"").append(delegate)
            .append("\"");
      } catch (final NullPointerException | NoSuchFieldException | IllegalArgumentException
          | IllegalAccessException e) {
        ClientLogger.logError("An Error occured whilst trying to sequence in game " + data.getGameName(), e);
      }
      if (step.getPlayerID() != null) {
        xmlfile.append(" player=\"").append(step.getPlayerID().getName()).append("\"");
      }
      if (step.getDisplayName() != null) {
        xmlfile.append(" display=\"").append(step.getDisplayName()).append("\"");
      }
      if (step.getMaxRunCount() > -1) {
        int maxRun = step.getMaxRunCount();
        if (maxRun == 0) {
          maxRun = 1;
        }
        xmlfile.append(" maxRunCount=\"").append(maxRun).append("\"");
      }
      xmlfile.append("/>\n");
    }
    xmlfile.append("        </sequence>\n");
  }

  private void unitList(final GameData data) {
    xmlfile.append("\n");
    xmlfile.append("    <unitList>\n");
    for(UnitType unit : data.getUnitTypeList()){
      xmlfile.append("        <unit name=\"").append(unit.getName()).append("\"/>\n");
    }
    xmlfile.append("    </unitList>\n");
  }

  private void playerList(final GameData data) {
    xmlfile.append("\n");
    xmlfile.append("    <playerList>\n");
    for(PlayerID player : data.getPlayerList().getPlayers()){
      xmlfile.append("        <player name=\"").append(player.getName()).append("\" optional=\"")
          .append(player.getOptional()).append("\"/>\n");
    }
    for(String allianceName : data.getAllianceTracker().getAlliances()){
      for(PlayerID alliedPlayer : data.getAllianceTracker().getPlayersInAlliance(allianceName)){
        xmlfile.append("        <alliance player=\"").append(alliedPlayer.getName()).append("\" alliance=\"")
            .append(allianceName).append("\"/>\n");
      }
    }
    xmlfile.append("    </playerList>\n");
  }

  private void relationshipTypeList(final GameData data) {
    final Collection<RelationshipType> types = data.getRelationshipTypeList().getAllRelationshipTypes();
    if (types.size() <= 4) {
      return;
    }
    xmlfile.append("\n");
    xmlfile.append("    <relationshipTypes>\n");
    for(RelationshipType current : types){
      final String name = current.getName();
      if (name.equals(Constants.RELATIONSHIP_TYPE_SELF) || name.equals(Constants.RELATIONSHIP_TYPE_NULL)
          || name.equals(Constants.RELATIONSHIP_TYPE_DEFAULT_WAR)
          || name.equals(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED)) {
        continue;
      }
      xmlfile.append("        <relationshipType name=\"").append(name).append("\"/>\n");
    }
    xmlfile.append("    </relationshipTypes>\n");
  }

  private void territoryEffectList(final GameData data) {
    final Collection<TerritoryEffect> types = data.getTerritoryEffectList().values();
    if (types.isEmpty()) {
      return;
    }
    xmlfile.append("\n");
    xmlfile.append("    <territoryEffectList>\n");
    for(TerritoryEffect current : types){
      xmlfile.append("        <territoryEffect name=\"").append(current.getName()).append("\"/>\n");
    }
    xmlfile.append("    </territoryEffectList>\n");
  }

  private void resourceList(final GameData data) {
    xmlfile.append("\n");
    xmlfile.append("    <resourceList>\n");
    for(Resource resource : data.getResourceList().getResources()){
      xmlfile.append("        <resource name=\"").append(resource.getName()).append("\"/>\n");
    }
    xmlfile.append("    </resourceList>\n");
  }

  private void map(final GameData data) {
    xmlfile.append("\n");
    xmlfile.append("    <map>\n");
    xmlfile.append("        <!-- Territory Definitions -->\n");
    final GameMap map = data.getMap();
    for(Territory ter : map.getTerritories()){
      xmlfile.append("        <territory name=\"").append(ter.getName()).append("\"");
      if (ter.isWater()) {
        xmlfile.append(" water=\"true\"");
      }
      xmlfile.append("/>\n");
    }
    connections(data);
    xmlfile.append("    </map>\n");
  }

  private class Connection {
    private final Territory _t1;
    private final Territory _t2;

    private Connection(final Territory t1, final Territory t2) {
      _t1 = t1;
      _t2 = t2;
    }

    @Override
    public int hashCode() {
      return _t1.hashCode() + _t2.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (o == null) {
        return false;
      }
      final Connection con = (Connection) o;
      return (_t1 == con._t1 && _t2 == con._t2);
    }
  }

  private void connections(final GameData data) {
    xmlfile.append("        <!-- Territory Connections -->\n");
    final GameMap map = data.getMap();
    final List<Connection> reverseConnectionTracker = new ArrayList<>();
    for(Territory ter : map.getTerritories()) {
      for(Territory nb : map.getNeighbors(ter)){
        if (!reverseConnectionTracker.contains(new Connection(ter, nb))) {
          xmlfile.append("        <connection t1=\"").append(ter.getName()).append("\" t2=\"").append(nb.getName())
              .append("\"/>\n");
          reverseConnectionTracker.add(new Connection(nb, ter));
        }
      }
    }
  }

  private void init(final GameData data) {
    xmlfile.append("<?xml version=\"1.0\"?>\n");
    xmlfile.append("<!DOCTYPE game SYSTEM \"" + GameParser.DTD_FILE_NAME + "\">\n");
    xmlfile.append("<game>\n");
    xmlfile.append("    <info name=\"").append(data.getGameName()).append("\" version=\"")
        .append(data.getGameVersion().toString()).append("\"/>\n");
    xmlfile.append("    <loader javaClass=\"").append(data.getGameLoader().getClass().getCanonicalName())
        .append("\"/>\n");
  }

  private void finish() {
    xmlfile.append("\n");
    xmlfile.append("</game>\n");
  }

  public String getXML() {
    return xmlfile.toString();
  }
}
