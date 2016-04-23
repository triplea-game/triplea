package games.strategy.engine.data.export;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.delegate.TechAdvance;

public class RulesAttachmentExporter extends DefaultAttachmentExporter {
  @Override
  protected String printOption(final Field field, final IAttachment attachment) throws AttachmentExportException {
    final String fieldName = field.getName();
    if (fieldName.equals("m_turns")) {
      return mTurnsHandler(field, attachment);
    }
    if (fieldName.equals("m_unitPresence")) {
      return mUnitPresenceHandler(field, attachment);
    }
    if (fieldName.equals("m_productionPerXTerritories")) {
      return mProductionPerXTerritoriesHandler(field, attachment);
    }
    if (fieldName.equals("m_atWarPlayers")) {
      // AtWarCount is part of m_atWarPlayers
      return mAtWarPlayersHandler(field, attachment);
    }
    if (fieldName.equals("m_atWarCount")) {
      // AtWarCount is part of m_atWarPlayers
      return "";
    }
    if (fieldName.equals("m_territoryCount")) {
      // atTerritoryCount is part of m_*Territories
      return "";
    }
    if (fieldName.matches("m_allied.*Territories") || fieldName.matches("m_enemy.*Territories")
        || fieldName.matches("m_direct.*Territories")) {
      return territoryCountListHandler(field, attachment, fieldName);
    }
    if (fieldName.equals("m_techs")) {
      return mTechsHandler(field, attachment);
    }
    if (fieldName.equals("m_techCount")) {
      // techCount is part of m_techs
      return "";
    }
    return super.printOption(field, attachment);
  }

  private String territoryCountListHandler(final Field field, final IAttachment attachment, final String fieldName)
      throws AttachmentExportException {
    String[] valueArray;
    try {
      valueArray = (String[]) field.get(attachment);
      if (valueArray == null || valueArray.length == 0) {
        return "";
      }
      final Iterator<String> values = Arrays.asList(valueArray).iterator();
      if (valueArray.length > 1) {
        // skip the arrayLength entry in the array because for Arrays > 1 the first entry is the count;
        values.next();
      }
      String returnValue = values.next();
      while (values.hasNext()) {
        returnValue = returnValue + ":" + values.next();
      }
      if (returnValue.length() == 0) {
        return "";
      }
      final String count = "" + ((RulesAttachment) attachment).getTerritoryCount();
      return printCountOption(fieldName.substring(2), returnValue, count);
    } catch (final IllegalArgumentException e) {
      throw new AttachmentExportException("e: " + e + " for territoryCountListHandler on option: " + fieldName
          + " on Attachment: " + attachment.getName());
    } catch (final IllegalAccessException e) {
      throw new AttachmentExportException("e: " + e + " for territoryCountListHandler on option: " + fieldName
          + " on Attachment: " + attachment.getName());
    } catch (final SecurityException e) {
      throw new AttachmentExportException("e: " + e + " for territoryCountListHandler on option: " + fieldName
          + " on Attachment: " + attachment.getName());
    }
  }

  @SuppressWarnings("unchecked")
  private String mAtWarPlayersHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    try {
      final Set<PlayerID> atWarPlayers = (Set<PlayerID>) field.get(attachment);
      if (atWarPlayers == null) {
        return "";
      }
      final String option = "" + Character.toLowerCase(field.getName().charAt(2)) + field.getName().substring(3);
      final Field atWarPlayerCountField = RulesAttachment.class.getDeclaredField("m_atWarCount");
      atWarPlayerCountField.setAccessible(true);
      final int count = atWarPlayerCountField.getInt(attachment);
      final Iterator<PlayerID> iWarPlayer = atWarPlayers.iterator();
      String value = iWarPlayer.next().getName();
      while (iWarPlayer.hasNext()) {
        value = value + ":" + iWarPlayer.next().getName();
      }
      return printCountOption(option, value, "" + count);
    } catch (final IllegalArgumentException | IllegalAccessException | SecurityException | NoSuchFieldException e) {
      throw new AttachmentExportException("e: " + e + " for mAtWarPlayersHandler on field: " + field.getName()
          + " on Attachment: " + attachment.getName());
    }
  }

  private String mUnitPresenceHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    return printUnitIntegerMap(field, attachment);
  }

  private String mProductionPerXTerritoriesHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    return printUnitIntegerMap(field, attachment);
  }

  @SuppressWarnings("unchecked")
  private String mTurnsHandler(final Field field, final IAttachment attachment) {
    Object oValue = null;
    try {
      oValue = field.get(attachment);
    } catch (final IllegalArgumentException | IllegalAccessException e) {
      ClientLogger.logError("Failed to get attachment: " + attachment + ", from field: " + field,  e);
    }
    String intList = "";
    final HashMap<Integer, Integer> intMap = (HashMap<Integer, Integer>) oValue;
    if (intMap == null) {
      return "";
    }
    for (final Integer curInt : intMap.keySet()) {
      final int start = curInt.intValue();
      final int end = intMap.get(curInt);
      if (intList.length() > 0) {
        intList = intList + ":";
      }
      if (start == end) {
        intList = intList + start;
      } else if (end == Integer.MAX_VALUE) {
        intList = intList + start + "-+";
      } else {
        intList = intList + start + "-" + end;
      }
    }
    return printDefaultOption("turns", intList);
  }

  @SuppressWarnings("unchecked")
  private String mTechsHandler(final Field field, final IAttachment attachment) throws AttachmentExportException {
    try {
      final List<TechAdvance> techAdvanceList = (List<TechAdvance>) field.get(attachment);
      if (techAdvanceList == null) {
        return "";
      }
      final Iterator<TechAdvance> iTechAdvances = techAdvanceList.iterator();
      String returnValue = "";
      if (iTechAdvances.hasNext()) {
        returnValue = iTechAdvances.next().getName();
      }
      while (iTechAdvances.hasNext()) {
        returnValue += ":" + iTechAdvances.next().getName();
      }
      if (returnValue.length() == 0) {
        return "";
      }
      return super.printCountOption("techs", returnValue, "" + ((RulesAttachment) attachment).getTechCount());
    } catch (final IllegalArgumentException | IllegalAccessException e) {
      throw new AttachmentExportException(
          "e: " + e + " for mTechHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
    }
  }
}
