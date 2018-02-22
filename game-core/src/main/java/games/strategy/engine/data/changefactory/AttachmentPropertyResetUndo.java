package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;

class AttachmentPropertyResetUndo extends Change {
  private static final long serialVersionUID = 5943939650116851332L;
  private final Attachable m_attachedTo;
  private final String m_attachmentName;
  private final Object m_newValue;
  private final String m_property;

  AttachmentPropertyResetUndo(final Attachable attachTo, final String attachmentName, final Object newValue,
      final String property) {
    m_attachmentName = attachmentName;
    m_attachedTo = attachTo;
    m_newValue = newValue;
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
    attachment.getPropertyMap().get(m_property).setObjectValue(m_newValue);
  }

  @Override
  public Change invert() {
    return new AttachmentPropertyReset(m_attachedTo, m_attachmentName, m_newValue, m_property);
  }

  @Override
  public String toString() {
    return "AttachmentPropertyClearUndo attached to:" + m_attachedTo + " name:" + m_attachmentName + " new value:"
        + m_newValue;
  }
}
