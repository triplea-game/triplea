package games.strategy.engine.data.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;

/**
 * User editable property representing a collection.
 *
 * @param <T> The type of elements in the collection.
 */
public class CollectionProperty<T> extends AbstractEditableProperty<List<T>> {
  private static final long serialVersionUID = 5338055034530377261L;

  private List<T> values;

  /**
   * Initializes a new instance of the CollectionProperty class.
   *
   * @param name name of the property.
   * @param description description of the property.
   * @param values collection of values.
   */
  public CollectionProperty(final String name, final String description, final Collection<T> values) {
    super(name, description);
    this.values = new ArrayList<>(values);
  }

  @Override
  public List<T> getValue() {
    return values;
  }

  @Override
  public void setValue(final List<T> value) {
    values = value;
  }

  @Override
  public int getRowsNeeded() {
    return (values == null ? 1 : Math.max(1, values.size()));
  }

  @Override
  public JComponent getEditorComponent() {
    if (values == null) {
      return new JTable();
    }
    final Object[][] tableD = new Object[values.size()][1];
    for (int i = 0; i < values.size(); i++) {
      tableD[i][0] = values.get(i);
    }
    return new JTable(tableD, new Object[] {"Values: "});
  }

  @Override
  public boolean validate(final List<T> value) {
    return value != null;
  }
}
