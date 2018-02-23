package games.strategy.engine.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

/**
 * All the info neccassary to describe a method call in one handy
 * serializable package.
 */
public class RemoteMethodCall implements Externalizable {
  private static final long serialVersionUID = 4630825927685836207L;
  private String remoteName;
  private String methodName;
  private Object[] args;
  // to save space, we dont serialize method name/types
  // instead we just serialize a number which can be transalted into
  // the correct method.
  private int methodNumber;
  // stored as a String[] so we can be serialzed
  private String[] argTypes;

  public RemoteMethodCall() {}

  public RemoteMethodCall(final String remoteName, final String methodName, final Object[] args,
      final Class<?>[] argTypes, final Class<?> remoteInterface) {
    if (argTypes == null) {
      throw new IllegalArgumentException("ArgTypes are null");
    }
    if ((args == null) && (argTypes.length != 0)) {
      throw new IllegalArgumentException("args but no types");
    }
    if ((args != null) && (args.length != argTypes.length)) {
      throw new IllegalArgumentException("Arg and arg type lengths dont match");
    }
    this.remoteName = remoteName;
    this.methodName = methodName;
    this.args = args;
    this.argTypes = classesToString(argTypes, args);
    methodNumber = RemoteInterfaceHelper.getNumber(methodName, argTypes, remoteInterface);
  }

  /**
   * @return Returns the channelName.
   */
  public String getRemoteName() {
    return remoteName;
  }

  /**
   * @return Returns the methodName.
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * @return Returns the args.
   */
  public Object[] getArgs() {
    return args;
  }

  /**
   * @return Returns the argTypes.
   */
  public Class<?>[] getArgTypes() {
    return stringsToClasses(argTypes, args);
  }

  private static Class<?>[] stringsToClasses(final String[] strings, final Object[] args) {
    final Class<?>[] classes = new Class<?>[strings.length];
    for (int i = 0; i < strings.length; i++) {
      classes[i] = stringToClass(strings[i], args[i]);
    }
    return classes;
  }

  @VisibleForTesting
  static Class<?> stringToClass(final @Nullable String string, final Object arg) {
    // null if we skipped writing because the arg is the expected class.
    // this saves some space since generally the arg will be of the correct type.
    if (string == null) {
      return arg.getClass();
    }

    switch (string) {
      case "int":
        return Integer.TYPE;
      case "short":
        return Short.TYPE;
      case "byte":
        return Byte.TYPE;
      case "long":
        return Long.TYPE;
      case "float":
        return Float.TYPE;
      case "double":
        return Double.TYPE;
      case "boolean":
        return Boolean.TYPE;
      default:
        try {
          return Class.forName(string);
        } catch (final ClassNotFoundException e) {
          throw new IllegalStateException(e);
        }
    }
  }

  private static String[] classesToString(final Class<?>[] classes, final Object[] args) {
    // as an optimization, if args[i].getClass == classes[i] then leave classes[i] as null
    // this will reduce the amount of info we write over the network in the common
    // case where the object is the same type as its arg
    final String[] string = new String[classes.length];
    for (int i = 0; i < classes.length; i++) {
      if ((args != null) && (args[i] != null) && (classes[i] == args[i].getClass())) {
        string[i] = null;
      } else {
        string[i] = classes[i].getName();
      }
    }
    return string;
  }

  @Override
  public String toString() {
    return "Remote method call, method name:" + methodName + " remote name:" + remoteName;
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeUTF(remoteName);
    out.writeByte(methodNumber);
    if (args == null) {
      out.writeByte(Byte.MAX_VALUE);
    } else {
      out.writeByte(args.length);
      for (final Object arg : args) {
        out.writeObject(arg);
      }
    }
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    remoteName = in.readUTF();
    methodNumber = in.readByte();
    final byte count = in.readByte();
    if (count != Byte.MAX_VALUE) {
      args = new Object[count];
      for (int i = 0; i < count; i++) {
        args[i] = in.readObject();
      }
    }
  }

  /**
   * After we have been de-serialized, we do not transmit enough
   * information to determine the method without being told
   * what class we operate on.
   */
  public void resolve(final Class<?> remoteType) {
    if (methodName != null) {
      return;
    }
    final Method method = RemoteInterfaceHelper.getMethod(methodNumber, remoteType);
    methodName = method.getName();
    argTypes = classesToString(method.getParameterTypes(), args);
  }
}
