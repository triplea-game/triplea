package games.strategy.engine.data.properties;

import javax.swing.JComponent;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public interface IEditableProperty
{
  public String getName();
  public Object getValue();

  /**
   *
   * @return component used to edit this property
   */
  public JComponent getEditorComponent();

  public JComponent getViewComponent();


}
