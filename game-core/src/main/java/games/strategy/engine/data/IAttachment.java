package games.strategy.engine.data;

import java.io.Serializable;

public interface IAttachment extends Serializable, DynamicallyModifiable {
  /** each implementing class NEEDS to have such an constructor, otherwise the parsing in GameParser won't work */
  Class<?>[] attachmentConstructorParameter = new Class<?>[] {String.class, Attachable.class, GameData.class};

  /**
   * Called after ALL attachments are created. IF an error occurs should throw an exception to halt the parsing.
   *
   * @param data
   *        game data
   * @throws GameParseException
   *         an error has occurred while validation
   */
  void validate(GameData data) throws GameParseException;

  Attachable getAttachedTo();

  void setAttachedTo(Attachable attachable);

  String getName();

  void setName(String name);
}
