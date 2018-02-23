package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;

class RemoveAttachmentChange extends Change {
  private static final long serialVersionUID = 6365648682759047674L;
  private final IAttachment m_attachment;
  private final String m_originalAttachmentName;
  private final Attachable m_originalAttachable;
  private final Attachable m_attachable;
  private final String m_name;

  public RemoveAttachmentChange(final IAttachment attachment, final Attachable attachable, final String name) {
    m_attachment = attachment;
    m_originalAttachmentName = attachment.getName();
    m_originalAttachable = attachment.getAttachedTo();
    m_attachable = attachable;
    m_name = name;
  }

  @Override
  protected void perform(final GameData data) {
    m_originalAttachable.removeAttachment(m_originalAttachmentName);
    m_attachment.setAttachedTo(m_attachable);
    m_attachment.setName(m_name);
    if ((m_attachable != null) && (m_name != null)) {
      m_attachable.addAttachment(m_name, m_attachment);
    }
  }

  @Override
  public Change invert() {
    return new AddAttachmentChange(m_attachment, m_originalAttachable, m_originalAttachmentName);
  }
}
