package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;

/**
 * Contains some utility methods that subclasses can use to make writing attachments easier.
 * FYI: You may never have a hashmap/linkedhashmap of any other "attachment" within an attachment.
 * This is because there will be a circular reference from this hashmap -> attachment1 -> playerid -> attachment2 ->
 * hashmap -> attachment1,
 * and this causes major problems for Java's deserializing.
 * When deserializing the attachments will not be resolved before their hashcode is taken, resulting in the wrong
 * hashcode and the
 * attachment going in the wrong bucket,
 * so that a .get(attachment1) will result in a null instead of giving the key for attachment1. So just don't have maps
 * of attachments, in
 * an attachment. Thx, Veqryn.
 */
public abstract class DefaultAttachment extends GameDataComponent implements IAttachment {
  private static final long serialVersionUID = -1985116207387301730L;
  @InternalDoNotExport
  private Attachable m_attachedTo;
  @InternalDoNotExport
  private String m_name;

  protected DefaultAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(gameData);
    setName(name);
    setAttachedTo(attachable);
  }

  @Override
  public abstract void validate(final GameData data) throws GameParseException;

  /**
   * Gets the attachment with the specified name and type from the specified object.
   *
   * @param namedAttachable The object from which the attachment is to be retrieved.
   * @param attachmentName The name of the attachment to retrieve.
   * @param attachmentType The type of the attachment to retrieve.
   *
   * @return The requested attachment.
   *
   * @throws IllegalStateException If the requested attachment is not found in the specified object.
   * @throws ClassCastException If the requested attachment is not of the specified type.
   */
  protected static <T extends IAttachment> T getAttachment(
      final NamedAttachable namedAttachable,
      final String attachmentName,
      final Class<T> attachmentType) {
    checkNotNull(namedAttachable);
    checkNotNull(attachmentName);
    checkNotNull(attachmentType);

    final T attachment = attachmentType.cast(namedAttachable.getAttachment(attachmentName));
    if (attachment == null) {
      throw new IllegalStateException(String.format("No attachment named '%s' of type '%s' for object named '%s'",
          attachmentName, attachmentType, namedAttachable.getName()));
    }

    return attachment;
  }

  /**
   * Throws an error if format is invalid.
   */
  protected static int getInt(final String value) {
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException nfe) {
      throw new IllegalArgumentException("Attachments: " + value + " is not a valid int value");
    }
  }

  /**
   * Throws an error if format is invalid. Must be either true or false ignoring case.
   */
  protected static boolean getBool(final String value) {
    if (value.equalsIgnoreCase(Constants.PROPERTY_TRUE)) {
      return true;
    } else if (value.equalsIgnoreCase(Constants.PROPERTY_FALSE)) {
      return false;
    } else {
      throw new IllegalArgumentException("Attachments: " + value + " is not a valid boolean");
    }
  }

  protected static IllegalArgumentException getSetterExceptionMessage(final DefaultAttachment failingObject,
      final String propertyName, final String givenValue, final String... allowedValues) {
    final StringBuilder sb = new StringBuilder();
    sb.append(failingObject.getClass().getName()).append(": ").append(failingObject.getName()).append(": property ")
        .append(propertyName).append(" must be either ");
    sb.append(allowedValues[0]);
    for (int i = 1; i < allowedValues.length; ++i) {
      sb.append(" or ").append(allowedValues[i]);
    }
    return new IllegalArgumentException(sb + " ([Not Allowed] Given: " + givenValue + ")");
  }

  protected String thisErrorMsg() {
    return "   for: " + this.toString();
  }

  /**
   * @return null or the toString() of the field value.
   */
  public String getRawPropertyString(final String property) {
    final Object obj = getPropertyMap().get(property);
    if (obj == null) {
      return null;
    }
    return obj.toString();
  }

  @Override
  public Attachable getAttachedTo() {
    return m_attachedTo;
  }

  @Override
  @InternalDoNotExport
  public void setAttachedTo(final Attachable attachable) {
    m_attachedTo = attachable;
  }

  @Override
  public String getName() {
    return m_name;
  }

  @Override
  @InternalDoNotExport
  public void setName(final String name) {
    m_name = name;
  }

  /**
   * Any overriding method for toString needs to include at least the Class, m_attachedTo, and m_name.
   * Or call super.toString()
   */
  @Override
  public String toString() {
    return getClass().getSimpleName() + " attached to:" + m_attachedTo + " with name:" + m_name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_attachedTo, m_name);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final DefaultAttachment other = (DefaultAttachment) obj;
    if (m_attachedTo == null) {
      if (other.m_attachedTo != null) {
        return false;
      }
    } else if (!m_attachedTo.toString().equals(other.m_attachedTo.toString())) {
      return false;
    }
    // else if (!m_attachedTo.equals(other.m_attachedTo)) // m_attachedTo does not override equals, so we should not
    // test it
    // return false;
    if (m_name == null) {
      if (other.m_name != null) {
        return false;
      }
    } else if (!m_name.equals(other.m_name)) {
      return false;
    }
    return this.toString().equals(other.toString());
  }
}
