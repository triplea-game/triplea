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

package games.strategy.engine.message;

import java.io.Serializable;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.HashMap;
import java.util.Map;

import games.strategy.net.*;
import games.strategy.net.IMessenger;
import games.strategy.net.IRemote;
import games.strategy.net.IRemoteMessenger;

/**
 * An implementation of IRemoteMessenger based on MessageManager and Messenger.
 * 
 * 
 * @author Sean Bridges
 */
public class RemoteMessenger implements IRemoteMessenger
{
    private final IMessageManager m_messageManager;
    private final IMessenger m_messenger;
    
    //maps remote name -> interface for that remote
    //this is a global map
    private final Map m_namesToInterfaces = Collections.synchronizedMap(new HashMap());
    //the remotes that have been registered locally
    private final Map m_localRemotes = Collections.synchronizedMap(new HashMap());
    
    
    /* 
     * @see games.strategy.net.IRemoteMessenger#getRemote(java.lang.String)
     */
    public RemoteMessenger(IMessageManager messageManager, IMessenger messenger)
    {   
        m_messageManager = messageManager;
        m_messenger = messenger;
        m_messenger.addMessageListener(m_messageListener);
        m_messenger.broadcast(new RemoteInitRequest());
    }
    
  
    public IRemote getRemote(String name)
    {
        //what is the type
        Class remoteClass = (Class) m_namesToInterfaces.get(name);
        if(remoteClass == null)
            throw new IllegalStateException("Nothing known about destination:" + name);
        
        InvocationHandler ih = new RemoteInvocationHandler(name, m_messageManager);
        Object rVal = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {remoteClass}, ih);
        //since remoteClass must be an interface that extends IRemote
        //this cast is valid
        return (IRemote) rVal; 
        
    }

    /* 
     * @see games.strategy.net.IRemoteMessenger#registerRemote(java.lang.Class, java.lang.Object, java.lang.String)
     */
    public void registerRemote(Class remoteInterface, Object implementor,
            String name)
    {
         if(!IRemote.class.isAssignableFrom(remoteInterface))
             throw new IllegalArgumentException(remoteInterface.getName() +  "does not implement IRemote");
         if(!remoteInterface.isAssignableFrom(implementor.getClass()))
             throw new IllegalArgumentException(implementor + " does not implement " + remoteInterface.getName());
         if(!remoteInterface.isInterface())
             throw new IllegalArgumentException(remoteInterface.getName() +  " must be an interface");
         
        RemoteMessengerKeyAdded keyAdded = new RemoteMessengerKeyAdded(name,remoteInterface.getName());
        //add globaly
        m_messenger.broadcast(keyAdded);
        //add localy
       registerLocal(keyAdded);
       
       RemoteDestination dest = new RemoteDestination(name, (IRemote) implementor);
       m_messageManager.addDestination(dest);
       m_localRemotes.put(name, dest);
    }

    /* 
     * @see games.strategy.net.IRemoteMessenger#unregisterRemote(java.lang.String)
     */
    public void unregisterRemote(String name)
    {
        RemoteMessengerKeyRemoved keyRemoved = new RemoteMessengerKeyRemoved(name);
        m_messenger.broadcast(keyRemoved);
        unregisterLocal(keyRemoved);
        
        m_messageManager.removeDestination((IDestination) m_localRemotes.get(name));

    }
    
    /**
     * Keeps an up to date list of what interfaces are registered under 
     * what names.  This info cant be carred by the MessageManager
     */
    private IMessageListener m_messageListener = new IMessageListener()
    {

        public void messageReceived(Serializable msg, INode from)
        {
           if(msg instanceof RemoteMessengerKeyAdded)
           {
               registerLocal((RemoteMessengerKeyAdded) msg);
               
           }
           else if(msg instanceof RemoteMessengerKeyRemoved)
           {
               unregisterLocal((RemoteMessengerKeyRemoved) msg);
           }
           if(msg instanceof RemoteInitRequest)
           {
               //only the server should respond
               if(!m_messenger.isServer())
                   return;
               Map mapping = new HashMap(m_namesToInterfaces);
               Iterator iter = mapping.keySet().iterator();
               while(iter.hasNext())
               {
                  String name = (String) iter.next();
                  String className = ((Class) mapping.get(name) ).getName();
                  m_messenger.send(new RemoteMessengerKeyAdded(name, className), from);
               }
           }
        }
    };
    
    /**
     * @param msg
     */
    private void registerLocal(RemoteMessengerKeyAdded keyAdded)
    {
           try
           {
               Class clazz = Class.forName(keyAdded.m_remoteInterface);
               m_namesToInterfaces.put(keyAdded.m_remoteName, clazz);
           } catch (ClassNotFoundException e)
           {
               e.printStackTrace();
           }
    }
    
    private void unregisterLocal(RemoteMessengerKeyRemoved keyRemoved)
    {
        m_namesToInterfaces.remove(keyRemoved.m_remoteName);
    }


    /* 
     * @see games.strategy.net.IRemoteMessenger#hasRemote(java.lang.String)
     */
    public boolean hasRemote(String name)
    {
        return m_namesToInterfaces.containsKey(name);
    }
}

/*
 * Broadcast when a remote has been de registered
 */
class RemoteMessengerKeyRemoved implements Serializable
{
    public final String m_remoteName;
    
    public RemoteMessengerKeyRemoved(final String remoteName)
    {
        m_remoteName = remoteName;
    }
}

/**
 * Broadcast whan a remote has been added.  Allows the RemoteMessenger
 * to track what interfaces are registered with each channel
 */
class RemoteMessengerKeyAdded implements Serializable
{
    public final String m_remoteName;
    public final String m_remoteInterface;
    
    public RemoteMessengerKeyAdded(final String remoteName, final String remoteInterface)
    {
        m_remoteName = remoteName;
        m_remoteInterface = remoteInterface;
    }
}

/**
 * We have just joined the network, request the registered keys
 */
class RemoteInitRequest implements Message
{
    
}

/**
 * Listens for RemoteMethodCall, and returns RemoteMethodCallResults.
 * 
 * This handles execution of the method. 
 * 
 * This is an IDesintation since we rely on MessageManager to block
 * the calling threads and to route the message and reply.
 * 
 */
class RemoteDestination implements IDestination
{
    private final String m_remoteName;
    private final IRemote m_implementation;
    
    
    public RemoteDestination(final String remoteName,
            final IRemote implementation)
    {
        m_remoteName = remoteName;
        m_implementation = implementation;
    }
    
    /* 
     * @see games.strategy.engine.message.IDestination#sendMessage(games.strategy.engine.message.Message)
     */
    public Message sendMessage(Message message)
    {
        if(! (message instanceof RemoteMethodCall))
            throw new IllegalArgumentException("Expecting a RemoteMethodCall, not:" + message);
        
        RemoteMethodCall remoteMethod = (RemoteMethodCall) message;
        
        Method method = null;
        try
        {
            method = m_implementation.getClass().getMethod(remoteMethod.getMethodName(), remoteMethod.getArgTypes());
            method.setAccessible(true);
        } catch (SecurityException e)
        {
            e.printStackTrace();
            return new RemoteMethodCallResults(new RuntimeException(e.getMessage()));
        } catch (NoSuchMethodException e)
        {
            e.printStackTrace();
            return new RemoteMethodCallResults(new RuntimeException(e.getMessage()));
        }
        
        try
        {
            Object rVal =  method.invoke(m_implementation, remoteMethod.getArgs());
            return new RemoteMethodCallResults(rVal);
        } catch (IllegalArgumentException e1)
        {
            e1.printStackTrace();
            return new RemoteMethodCallResults(new RuntimeException(e1.getMessage()));            
        } catch (IllegalAccessException e1)
        {
            e1.printStackTrace();
            return new RemoteMethodCallResults(new RuntimeException(e1.getMessage()));
        } catch (InvocationTargetException e1)
        {
            //an exception in implementor occured while executing, just return this
            //no need to print the exception here since we have nothing 
            //to do with this, its between implementor and its caller
            return new RemoteMethodCallResults(e1.getTargetException());
        }
    }


    /* 
     * @see games.strategy.engine.message.IDestination#getName()
     */
    public String getName()
    {
        return m_remoteName;
    }
    
}

/**
 * The results of a method execution.
 * 
 * Note that either one of m_rVal or m_exception will be null,
 * since the method can either throw or return
 * 
 */
class RemoteMethodCallResults implements Message
{
    private final Object m_rVal;
    //throwable implements Serializable
    private final Throwable m_exception;
    
    public RemoteMethodCallResults(final Object rVal)
    {
        m_rVal = rVal;
        m_exception = null;
    }

    public RemoteMethodCallResults(Throwable exception)
    {
        m_rVal = null;
        m_exception = exception;
    }

    public Throwable getException()
    {
        return m_exception;
    }
    public Object getRVal()
    {
        return m_rVal;
    }
}

/**
 * 
 * Sends the method call over the net
 */
class RemoteInvocationHandler implements InvocationHandler
{
    private final String m_remoteName;
    private final IMessageManager m_messageManager;
    
    public RemoteInvocationHandler(final String remoteName,
            final IMessageManager messageManager)
    {
        m_remoteName = remoteName;
        m_messageManager = messageManager;
    }
    /* 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object params, Method arg1, Object[] arg2) throws Throwable
    {
        RemoteMethodCall call = new RemoteMethodCall(m_remoteName,
                                                     arg1.getName(),
                                                     arg2,
                                                     arg1.getParameterTypes());
        
        RemoteMethodCallResults results = (RemoteMethodCallResults) m_messageManager.send(call, m_remoteName);
        
        if(results.getException() != null)
            throw results.getException();
        return results.getRVal();
        
    }
}
