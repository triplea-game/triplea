package games.strategy.engine.data.export;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.UnitType;

public class UnitSupportAttachmentExporter extends DefaultAttachmentExporter {
  @Override
  protected String printOption(final Field field, final IAttachment attachment) throws AttachmentExportException {
    final String fieldName = field.getName();
    if (fieldName.equals("m_unitType")) {
      return mUnitTypeHandler(field, attachment);
    }
    if (fieldName.equals("m_players")) {
      return mPlayersHandler(field, attachment);
    }
    if (fieldName.equals("m_offence") || fieldName.equals("m_defence") || fieldName.equals("m_roll")
        || fieldName.equals("m_strength") || fieldName.equals("m_allied") || fieldName.equals("m_enemy")) {
      return "";
    }
    return super.printOption(field, attachment);
  }

  private String mPlayersHandler(final Field field, final IAttachment attachment) throws AttachmentExportException {
    return printPlayerList(field, attachment);
  }

  @SuppressWarnings("unchecked")
  private String mUnitTypeHandler(final Field field, final IAttachment attachment) throws AttachmentExportException {
    try {
      final Set<UnitType> unitTypes = (Set<UnitType>) field.get(attachment);
      final Iterator<UnitType> iUnitTypes = unitTypes.iterator();
      String returnValue = "";
      if (iUnitTypes.hasNext()) {
        returnValue = iUnitTypes.next().getName();
      }
      while (iUnitTypes.hasNext()) {
        returnValue = returnValue + ":" + iUnitTypes.next().getName();
      }
      if (returnValue.length() > 0) {
        return printDefaultOption("unitType", returnValue);
      }
      return "";
    } catch (final IllegalArgumentException | IllegalAccessException e) {
      throw new AttachmentExportException(
          "e: " + e + " for mUnitTypesHandler on field: " + field + "  on Attachment: " + attachment.getName());
    }
  }
}
