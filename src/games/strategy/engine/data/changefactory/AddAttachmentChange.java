package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;

class AddAttachmentChange extends Change {
  private static final long serialVersionUID = -21015135248288454L;
  private final IAttachment m_attachment;
  private final String m_originalAttachmentName;
  private final Attachable m_originalAttachable;
  private final Attachable m_attachable;
  private final String m_name;

  public AddAttachmentChange(final IAttachment attachment, final Attachable attachable, final String name) {
    m_attachment = attachment;
    m_originalAttachmentName = attachment.getName();
    m_originalAttachable = attachment.getAttachedTo();
    m_attachable = attachable;
    m_name = name;
  }

  @Override
  protected void perform(final GameData data) {
    m_attachable.addAttachment(m_name, m_attachment);
    m_attachment.setName(m_name);
    m_attachment.setAttachedTo(m_attachable);
  }

  @Override
  public Change invert() {
    return new RemoveAttachmentChange(m_attachment, m_originalAttachable, m_originalAttachmentName);
  }
}
