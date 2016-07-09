package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.triplea.attachments.TechAttachment;

class GenericTechChange extends Change {
  private static final long serialVersionUID = -2439447526511535571L;
  private final Attachable m_attachedTo;
  private final String m_attachmentName;
  private final Boolean m_newValue;
  private final Boolean m_oldValue;
  private final String m_property;

  public Attachable getAttachedTo() {
    return m_attachedTo;
  }

  public String getAttachmentName() {
    return m_attachmentName;
  }

  GenericTechChange(final TechAttachment attachment, final Boolean newValue, final String property) {
    if (attachment == null) {
      throw new IllegalArgumentException("No attachment, newValue:" + newValue + " property:" + property);
    }
    m_attachedTo = attachment.getAttachedTo();
    m_attachmentName = attachment.getName();
    m_oldValue = Boolean.valueOf(attachment.hasGenericTech(property));
    m_newValue = Boolean.valueOf(newValue);
    m_property = property;
  }

  public GenericTechChange(final Attachable attachTo, final String attachmentName, final Boolean newValue,
      final Boolean oldValue, final String property) {
    m_attachmentName = attachmentName;
    m_attachedTo = attachTo;
    m_newValue = newValue;
    m_oldValue = oldValue;
    m_property = property;
  }

  @Override
  public void perform(final GameData data) {
    final TechAttachment attachment = (TechAttachment) m_attachedTo.getAttachment(m_attachmentName);
    attachment.setGenericTech(m_property, m_newValue);
  }

  @Override
  public Change invert() {
    return new GenericTechChange(m_attachedTo, m_attachmentName, m_oldValue, m_newValue, m_property);
  }

  @Override
  public String toString() {
    return "GenericTechChange attached to:" + m_attachedTo + " name:" + m_attachmentName + " new value:" + m_newValue
        + " old value:" + m_oldValue;
  }
}
