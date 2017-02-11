package games.strategy.engine.data.export;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.util.IntegerMap;

/**
 * Base class for all attachment exporter classes, if you create a new attachment extend this class,
 * and configure the AttachmentExporterFactory to include your new exporter. Or if your new Attachment
 * is very standard you can use this one directly. Configure the same in AttachmentExporterFactory
 */
public class DefaultAttachmentExporter {

  protected String printIntegerOption(final Field field, final String option, final IAttachment attachment,
      final boolean printDefaultValue) throws AttachmentExportException {
    int value;
    try {
      value = field.getInt(attachment);
      // don't set default values
      final IAttachment defaultAttachment = attachment.getClass().newInstance();
      final int defaultValue = field.getInt(defaultAttachment);
      if (defaultValue != value || printDefaultValue) {
        return printDefaultOption(option, "" + value);
      } else {
        return "";
      }
    } catch (final IllegalArgumentException | InstantiationException | IllegalAccessException e) {
      throw new AttachmentExportException("e: " + e + " for printIntegerOption on field: " + field + " option: "
          + option + " on Attachment: " + attachment.getName());
    }
  }

  protected String printIntegerOption(final Field field, final String option, final IAttachment attachment)
      throws AttachmentExportException {
    return printIntegerOption(field, option, attachment, false);
  }

  protected String printStringOption(final Field field, final String option, final IAttachment attachment)
      throws AttachmentExportException {
    return printStringOption(field, option, attachment, false);
  }

  protected String printStringOption(final Field field, final String option, final IAttachment attachment,
      final boolean printDefaultValue) throws AttachmentExportException {
    String value;
    try {
      value = (String) field.get(attachment);
      if (value == null) {
        return "";
      }
      // don't set default values
      final IAttachment defaultAttachment = attachment.getClass().newInstance();
      final String defaultValue = (String) field.get(defaultAttachment);
      if (value.equals(defaultValue) && !printDefaultValue) {
        return "";
      } else {
        return printDefaultOption(option, "" + value);
      }
    } catch (final IllegalArgumentException | InstantiationException | IllegalAccessException e) {
      throw new AttachmentExportException("e: " + e + " for printStringOption on field: " + field + " option: " + option
          + " on Attachment: " + attachment.getName());
    }
  }

  protected String printBooleanOption(final Field field, final String option, final IAttachment attachment)
      throws AttachmentExportException {
    return printBooleanOption(field, option, attachment, false);
  }

  protected String printBooleanOption(final Field field, final String option, final IAttachment attachment,
      final boolean printDefaultValue) throws AttachmentExportException {
    boolean value = false;
    try {
      value = field.getBoolean(attachment);
      // don't set default values
      final IAttachment defaultAttachment = attachment.getClass().newInstance();
      final boolean defaultValue = field.getBoolean(defaultAttachment);
      if (value == defaultValue && !printDefaultValue) {
        return "";
      } else {
        return printDefaultOption(option, "" + value);
      }
    } catch (final IllegalArgumentException | InstantiationException | IllegalAccessException e) {
      throw new AttachmentExportException("e: " + e + " for printBooleanOption on field: " + field + " option: "
          + option + " on Attachment: " + attachment.getName());
    }
  }

  protected String printDefaultOption(final String option, final String value) {
    return "            <option name=\"" + option + "\" value=\"" + value + "\"/>\n";
  }

  protected String printCountOption(final String option, final String value, final String count) {
    return "            <option name=\"" + option + "\" value=\"" + value + "\" count=\"" + count + "\"/>\n";
  }

  @SuppressWarnings("unchecked")
  protected String printPlayerList(final Field field, final IAttachment attachment) throws AttachmentExportException {
    try {
      final List<PlayerID> playerIds = (List<PlayerID>) field.get(attachment);
      final List<String> playerNames = new ArrayList<>();
      for (final PlayerID playerID : playerIds) {
        playerNames.add(playerID.getName());
      }
      final String optionName = "" + Character.toLowerCase(field.getName().charAt(2)) + field.getName().substring(3);
      if (!playerNames.isEmpty()) {
        return printDefaultOption(optionName, Joiner.on(':').join(playerNames));
      }
      return "";
    } catch (final IllegalArgumentException | IllegalAccessException e) {
      throw new AttachmentExportException(
          "e: " + e + " for mPlayersHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
    }
  }

  @SuppressWarnings("unchecked")
  protected String printUnitIntegerMap(final Field field, final IAttachment attachment)
      throws AttachmentExportException {
    try {
      final String optionName = "" + Character.toLowerCase(field.getName().charAt(2)) + field.getName().substring(3);
      final IntegerMap<UnitType> map = (IntegerMap<UnitType>) field.get(attachment);
      final StringBuilder returnValue = new StringBuilder();
      if (map == null) {
        return "";
      }
      for (final UnitType type : map.keySet()) {
        final int number = map.getInt(type);
        if (type == null) {
          returnValue.append(printCountOption(optionName, "ANY", "" + number));
        } else {
          returnValue.append(printCountOption(optionName, type.getName(), "" + number));
        }
      }
      return returnValue.toString();
    } catch (final IllegalArgumentException | ArrayIndexOutOfBoundsException | IllegalAccessException e) {
      throw new AttachmentExportException("e: " + e + " for mUnitPresenceHandler on field: " + field.getName()
          + " on Attachment: " + attachment.getName());
    }
  }
}
