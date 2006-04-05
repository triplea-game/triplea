/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package games.strategy.engine.data;

import java.lang.reflect.Method;


public class ChangeAttachmentChange extends Change
{
  private final Attachable m_attatchedTo;
  private final String m_attatchmentName;
  private final String m_newValue;
  private String m_oldValue;
  private final String m_property;

  public Attachable getAttatchedTo()
  {
    return m_attatchedTo;
  }

  public String getAttatchmentName()
  {
    return m_attatchmentName;
  }

  ChangeAttachmentChange(IAttachment attatchment, String newValue, String property)
  {
    m_attatchedTo = attatchment.getAttatchedTo();
    m_attatchmentName = attatchment.getName();
    m_newValue = newValue;
    m_property = property;

    try
     {
       Method getter = attatchment.getClass().getMethod("get" + capitalizeFirstLetter(property), new Class[0]);
       m_oldValue = (String) getter.invoke(attatchment, new Object[0]);
     }
     catch(Exception e)
     {
       e.printStackTrace();
     }
  }

  public ChangeAttachmentChange(Attachable attatchTo, String attatchmentName, String newValue, String oldValue, String property)
  {
    m_attatchmentName = attatchmentName;
    m_attatchedTo = attatchTo;
    m_newValue = newValue;
    m_oldValue = oldValue;
    m_property = property;

  }


  private String capitalizeFirstLetter(String aString)
  {
    char first = aString.charAt(0);
    first = Character.toUpperCase(first);
    return first + aString.substring(1);
  }


  public void perform(GameData data)
  {
    try
    {
      IAttachment attachment = m_attatchedTo.getAttachment(m_attatchmentName);
      Method setter = attachment.getClass().getMethod("set" + capitalizeFirstLetter(m_property), new Class[]
        {String.class});
      setter.invoke(attachment, new Object[] {m_newValue});
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }


  public Change invert()
  {
    return new ChangeAttachmentChange(m_attatchedTo, m_attatchmentName, m_oldValue, m_newValue, m_property);
  }

  public String toString()
  {
      return "ChangAttatchmentChange attatched to:" + m_attatchedTo + " name:" + m_attatchmentName + " new value:" + m_newValue + " old value:" + m_oldValue;
  }

}
