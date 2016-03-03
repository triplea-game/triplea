package games.strategy.engine.data.export;

import games.strategy.engine.data.IAttachment;

/**
 * This class returns the right AttachmentExporter based on the
 */
public class AttachmentExporterFactory {
  /**
   * @param attachment
   *        the attachment to base the exporter on
   * @return an Exporter to export the attachment
   * @throws AttachmentExportException
   */
  public static IAttachmentExporter getExporter(final IAttachment attachment) throws AttachmentExportException {
    if (attachment.getClass() == games.strategy.triplea.attachments.CanalAttachment.class) {
      return new DefaultAttachmentExporter();
    }
    if (attachment.getClass() == games.strategy.triplea.attachments.RulesAttachment.class) {
      return new RulesAttachmentExporter();
    }
    if (attachment.getClass() == games.strategy.triplea.attachments.TechAttachment.class) {
      return new TechAttachmentExporter();
    }
    if (attachment.getClass() == games.strategy.triplea.attachments.TerritoryAttachment.class) {
      return new TerritoryAttachmentExporter();
    }
    if (attachment.getClass() == games.strategy.triplea.attachments.TriggerAttachment.class) {
      return new TriggerAttachmentExporter();
    }
    if (attachment.getClass() == games.strategy.triplea.attachments.UnitAttachment.class) {
      return new UnitAttachmentExporter();
    }
    if (attachment.getClass() == games.strategy.triplea.attachments.UnitSupportAttachment.class) {
      return new UnitSupportAttachmentExporter();
    }
    if (attachment.getClass() == games.strategy.triplea.attachments.PlayerAttachment.class) {
      return new PlayerAttachmentExporter();
    }
    throw new AttachmentExportException("No exportor defined for: " + attachment.getClass().getCanonicalName());
  }
}
