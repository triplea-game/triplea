package games.strategy.engine.data.export;

/**
 * Exception used by the AttachmentExporters
 */
public class AttachmentExportException extends Exception {
  private static final long serialVersionUID = 6134166689856636372L;

  public AttachmentExportException(final String error) {
    super(error);
  }
}
