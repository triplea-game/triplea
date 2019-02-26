package games.strategy.engine.history;

class RootHistoryNode extends HistoryNode {
  private static final long serialVersionUID = 625147613043836829L;

  RootHistoryNode(final String title) {
    super(title);
  }

  @Override
  public SerializationWriter getWriter() {
    throw new IllegalStateException("Not implemented");
  }
}
