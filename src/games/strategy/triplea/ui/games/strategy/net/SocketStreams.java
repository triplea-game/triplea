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

package games.strategy.net;

import java.io.*;
import java.net.Socket;

/**
 * A holder for all the streams associated with a socket.<p>
 * 
 * We need this because we can only buffer a socket once (otherwise you have issues
 * when the first buffer reads bytes that weren't meant for it)<p>
 * 
 * @author sgb
 */
public class SocketStreams
{

    private final InputStream m_socketIn;
    private final OutputStream m_socketOut;
    private final BufferedOutputStream m_bufferedOut;
    private final BufferedInputStream m_bufferedIn;
    
    
    public SocketStreams(Socket s) throws IOException
    {
        m_socketIn = s.getInputStream();
        m_socketOut = s.getOutputStream();
        m_bufferedIn = new BufferedInputStream(m_socketIn);
        m_bufferedOut = new BufferedOutputStream(m_socketOut);
    }

    public BufferedInputStream getBufferedIn()
    {
        return m_bufferedIn;
    }


    public BufferedOutputStream getBufferedOut()
    {
        return m_bufferedOut;
    }


    public InputStream getSocketIn()
    {
        return m_socketIn;
    }


    public OutputStream getSocketOut()
    {
        return m_socketOut;
    }
    
    
    
}
