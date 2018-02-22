package games.strategy.engine.data;

import java.util.Map;

interface DynamicallyModifiable {
  Map<String, AttachmentProperty<?>> getPropertyMap();
}
