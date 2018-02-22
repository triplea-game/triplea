package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;

/**
 * Resets the value to the default value.
 */
class AttachmentPropertyReset extends Change {
  private static final long serialVersionUID = 9208154387325299072L;
  private final Attachable m_attachedTo;
  private final String m_attachmentName;
  private final Object m_oldValue;
  private final String m_property;

  AttachmentPropertyReset(final IAttachment attachment, final String property) {
    if (attachment == null) {
      throw new IllegalArgumentException("No attachment, property:" + property);
    }
    m_attachedTo = attachment.getAttachedTo();
    m_attachmentName = attachment.getName();
    m_oldValue = attachment.getOrError(property).getValue();
    m_property = property;
  }

  AttachmentPropertyReset(final Attachable attachTo, final String attachmentName, final Object oldValue,
      final String property) {
    m_attachmentName = attachmentName;
    m_attachedTo = attachTo;
    m_oldValue = oldValue;
    m_property = property;
  }

  public Attachable getAttachedTo() {
    return m_attachedTo;
  }

  public String getAttachmentName() {
    return m_attachmentName;
  }

  @Override
  public void perform(final GameData data) {
    final IAttachment attachment = m_attachedTo.getAttachment(m_attachmentName);
    attachment.getOrError(m_property).resetValue();
  }

  @Override
  public Change invert() {
    return new AttachmentPropertyResetUndo(m_attachedTo, m_attachmentName, m_oldValue, m_property);
  }

  @Override
  public String toString() {
    return "AttachmentPropertyClear attached to:" + m_attachedTo + " name:" + m_attachmentName + ", reset old value:"
        + m_oldValue;
  }
}
