package games.strategy.engine.xml;

import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.AttachmentProperty;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;

public class TestAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 4886924951201479496L;
  private static final Map<String, Function<IAttachment, AttachmentProperty<?>>> propertyMap = createPropertyMap();

  private String m_value;

  /** Creates new TestAttachment. */
  public TestAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  @Override
  public Attachable getAttachedTo() {
    return null;
  }

  @InternalDoNotExport
  @Override
  public void setAttachedTo(final Attachable unused) {}

  @Override
  public String getName() {
    return null;
  }

  @InternalDoNotExport
  @Override
  public void setName(final String name) {}

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setValue(final String value) {
    m_value = value;
  }

  public String getValue() {
    return m_value;
  }

  public void resetValue() {
    m_value = null;
  }

  @Override
  public void validate(final GameData data) {}


  private static Map<String, Function<IAttachment, AttachmentProperty<?>>> createPropertyMap() {
    return ImmutableMap.<String, Function<IAttachment, AttachmentProperty<?>>>builder()
        .put("value", ofCast(a -> AttachmentProperty.of(a::setValue, a::getValue, a::resetValue)))
        .build();
  }

  @Override
  public Map<String, Function<IAttachment, AttachmentProperty<?>>> getPropertyMap() {
    return propertyMap;
  }

  private static Function<IAttachment, AttachmentProperty<?>> ofCast(
      final Function<TestAttachment, AttachmentProperty<?>> function) {
    return function.compose(TestAttachment.class::cast);
  }
}
