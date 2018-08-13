package games.strategy.engine.data;

import java.io.Serializable;

/**
 * Behavior or data that can be attached to a game object. Permits open-ended extension of game objects without
 * modifying their code.
 */
public interface IAttachment extends Serializable, DynamicallyModifiable {
  /** each implementing class NEEDS to have such an constructor, otherwise the parsing in GameParser won't work */
  Class<?>[] attachmentConstructorParameter = new Class<?>[] {String.class, Attachable.class, GameData.class};

  /**
   * Called after ALL attachments are created.
   */
  void validate(GameData data) throws GameParseException;

  Attachable getAttachedTo();

  void setAttachedTo(Attachable attachable);

  String getName();

  void setName(String name);
}
