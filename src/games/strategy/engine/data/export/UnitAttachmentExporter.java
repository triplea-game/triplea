package games.strategy.engine.data.export;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import games.strategy.engine.data.IAttachment;

public class UnitAttachmentExporter extends DefaultAttachmentExporter {
  @Override
  protected String printOption(final Field field, final IAttachment attachment) throws AttachmentExportException {
    final String fieldName = field.getName();
    if (fieldName.equals("m_requiresUnits")) {
      return mRequiresUnitsHandler(field, attachment);
    }
    if (fieldName.equals("m_canBeGivenByTerritoryTo")) {
      return mCanBeGivenByTerritoryToHandler(field, attachment);
    }
    if (fieldName.equals("m_destroyedWhenCapturedBy")) {
      return mDestroyedWhenCapturedByHandler(field, attachment);
    }
    if (fieldName.equals("m_givesMovement")) {
      return mGivesMovementHandler(field, attachment);
    }
    if (fieldName.equals("m_consumesUnits")) {
      return consumesUnitsHandler(field, attachment);
    }
    if (fieldName.equals("m_createsUnitsList")) {
      return mCreatesUnitsListHandler(field, attachment);
    }
    if (fieldName.equals("m_canBeCapturedOnEnteringBy")) {
      return mCanBeCapturedOnEnteringByHandler(field, attachment);
    }
    return super.printOption(field, attachment);
  }

  private String consumesUnitsHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    return printUnitIntegerMap(field, attachment);
  }

  private String mCanBeCapturedOnEnteringByHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    return printPlayerList(field, attachment);
  }

  private String mCreatesUnitsListHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    return printUnitIntegerMap(field, attachment);
  }

  private String mGivesMovementHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    return printUnitIntegerMap(field, attachment);
  }

  private String mDestroyedWhenCapturedByHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    return printPlayerList(field, attachment);
  }

  private String mCanBeGivenByTerritoryToHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    return printPlayerList(field, attachment);
  }

  @SuppressWarnings("unchecked")
  private String mRequiresUnitsHandler(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    try {
      final ArrayList<String[]> requiresUnitListList = (ArrayList<String[]>) field.get(attachment);
      final Iterator<String[]> iRequiresListList = requiresUnitListList.iterator();
      String returnValue = "";
      while (iRequiresListList.hasNext()) {
        final Iterator<String> iRequiresList = Arrays.asList(iRequiresListList.next()).iterator();
        String value = iRequiresList.next();
        while (iRequiresList.hasNext()) {
          value = value + ":" + iRequiresList.next();
        }
        returnValue = returnValue + printDefaultOption("requiresUnits", value);
      }
      return returnValue;
    } catch (final IllegalArgumentException | IllegalAccessException e) {
      throw new AttachmentExportException("e: " + e + " for mRequiresUnitsHandler on field: " + field.getName()
          + " on Attachment: " + attachment.getName());
    }
  }
}
