package games.strategy.engine.data.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.IAttachment;
import games.strategy.triplea.attachments.CanalAttachment;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.RelationshipTypeAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.util.IntegerMap;
import games.strategy.util.PropertyUtil;

/**
 * A test that validates that all attachment classes have properties with valid setters and getters
 */
public class ValidateAttachmentsTest {
  /**
   * Test that the Example Attachment is valid
   */
  @Test
  public void testExample() {
    final String errors = validateAttachment(ExampleAttachment.class);
    assertEquals(0, errors.length());
  }

  /**
   * Tests that the algorithm finds invalidly named field
   */
  @Test
  public void testInvalidField() {
    final String errors = validateAttachment(InvalidFieldNameExample.class);
    assertTrue(errors.length() > 0);
    assertTrue(errors.contains("missing field for setter"));
  }

  /**
   * tests that the algorithm will find invalid annotation on a getters
   */
  @Test
  public void testAnnotationOnGetter() {
    final String errors = validateAttachment(InvalidGetterExample.class);
    assertTrue(errors.length() > 0);
    assertTrue(errors.contains("begins with 'set' so must have either InternalDoNotExport or GameProperty annotation"));
  }

  /**
   * Tests that the algorithm will find invalid return types
   */
  @Test
  public void testInvalidReturnType() {
    final String errors = validateAttachment(InvalidReturnTypeExample.class);
    assertTrue(errors.length() > 0);
    assertTrue(errors.contains("property field is type"));
  }

  /**
   * Tests that the algorithm will find invalid clear method
   */
  @Test
  public void testInvalidClearMethod() {
    final String errors = validateAttachment(InvalidClearExample.class);
    assertTrue(errors.length() > 0);
    assertTrue(errors.contains("doesn't have a clear method"));
  }

  /**
   * Tests that the algorithm will find invalid clear method
   */
  @Test
  public void testInvalidResetMethod() {
    final String errors = validateAttachment(InvalidResetExample.class);
    assertTrue(errors.length() > 0);
    assertTrue(errors.contains("doesn't have a resetter method"));
  }

  /**
   * Tests that the algorithm will find adders that doesn't have type IntegerMap
   */
  @Test
  public void testInvalidFieldType() {
    final String errors = validateAttachment(InvalidFieldTypeExample.class);
    assertTrue(errors.length() > 0);
    assertTrue(errors.contains("is not a Collection or Map or IntegerMap"));
  }

  private static List<Class<? extends IAttachment>> getKnownAttachmentClasses() {
    List<Class<? extends IAttachment>> result = new ArrayList<>();
    result.add(DefaultAttachment.class);
    result.add(CanalAttachment.class);
    result.add(PlayerAttachment.class);
    result.add(PoliticalActionAttachment.class);
    result.add(RelationshipTypeAttachment.class);
    result.add(RulesAttachment.class);
    result.add(TechAttachment.class);
    result.add(TerritoryAttachment.class);
    result.add(TerritoryEffectAttachment.class);
    result.add(TriggerAttachment.class);
    result.add(UnitAttachment.class);
    result.add(UnitSupportAttachment.class);
    result.add(TechAbilityAttachment.class);
    // result.add(AbstractConditionsAttachment.class);
    // result.add(AbstractPlayerRulesAttachment.class);
    // result.add(AbstractRulesAttachment.class);
    // result.add(AbstractTriggerAttachment.class);
    return result;
  }

  /**
   * When testAllAttachments doesn't work, we can test specific attachments here.
   */
  @Test
  public void testSpecificAttachments() {
    final StringBuilder sb = new StringBuilder();
    for (final Class<? extends IAttachment> clazz : getKnownAttachmentClasses()) {
      sb.append(validateAttachment(clazz));
    }
    if (sb.length() > 0) {
      System.out.println(sb.toString());
      // fail(sb.toString());
    }
  }

  /**
   * Scans the compiled /classes folder and finds all classes that implement IAttachment to verify that
   * all @GameProperty have valid setters and getters
   */
  @Test
  public void testAllAttachments() {
    // find the classes folder
    final URL url = getClass().getResource("/");
    File file = null;
    try {
      file = new File(url.toURI());
      file = new File(file.getParent(), "classes");
    } catch (final URISyntaxException e) {
      fail(e.getMessage());
      ClientLogger.logQuietly(e);
    }
    final String errors = findAttachmentsAndValidate(file);
    if (errors.length() > 0) {
      System.out.println(errors);
      // fail("\n" + errors);
    }
  }

  // file to find classes or directory
  static FileFilter s_classOrDirectory = new FileFilter() {
    @Override
    public boolean accept(final File file) {
      return file.isDirectory() || file.getName().endsWith(".class");
    }
  };

  /**
   * Recursive method to find all classes that implement IAttachment and validate that they use the @GameProperty
   * annotation correctly
   *
   * @param file
   *        the file or directory
   */
  private static String findAttachmentsAndValidate(final File file) {
    final StringBuilder sb = new StringBuilder("");
    if (file.isDirectory()) {
      final File[] files = file.listFiles(s_classOrDirectory);
      for (final File aFile : files) {
        sb.append(findAttachmentsAndValidate(aFile));
      }
    } else {
      final String fileName = file.getAbsolutePath();
      final String classesRoot = File.separatorChar + "classes" + File.separatorChar;
      final int index = fileName.indexOf(classesRoot) + classesRoot.length();
      String className = fileName.substring(index);
      className = className.replace(File.separator, ".");
      if (!className.endsWith(".class")) {
        return "";
      }
      className = className.substring(0, className.lastIndexOf(".class"));
      if (isSkipClass(className)) {
        return "";
      }
      Class<?> clazz;
      try {
        clazz = Class.forName(className);
        if (!clazz.isInterface() && IAttachment.class.isAssignableFrom(clazz)) // &&
                                                                               // !Modifier.isAbstract(clazz.getModifiers())
        {
          @SuppressWarnings("unchecked")
          final Class<? extends IAttachment> attachmentClass = (Class<? extends IAttachment>) clazz;
          // sb.append("Testing class: " + attachmentClass.getCanonicalName());
          sb.append(validateAttachment(attachmentClass));
        }
      } catch (final ClassNotFoundException e) {
        sb.append("Warning: Class ").append(className).append(" not found. Error Message: ").append(e.getMessage())
            .append("\n");
      } catch (final Throwable e) {
        sb.append("Warning: Class ").append(className).append(" could not be loaded. Error Message: ")
            .append(e.getMessage()).append("\n");
      }
    }
    return sb.toString();
  }

  /**
   * todo(kg) fix this
   * ReliefImageBreaker and TileImageBreaker has a static field that opens a save dialog!!!
   * "InvalidGetterExample", "InvalidFieldNameExample", "InvalidReturnTypeExample" are skipped because they are
   * purposely invalid, and use
   * to test the validation algorithm
   */
  public static final List<String> SKIPCLASSES = Arrays.asList("ReliefImageBreaker", "TileImageBreaker",
      "InvalidGetterExample", "InvalidFieldNameExample", "InvalidReturnTypeExample", "InvalidClearExample",
      "InvalidFieldTypeExample", "ChatPlayerPanel", "GUID", "Node");

  /**
   * Contains a list of classes which has static initializes, unfortunately you can't reflect this, since loading the
   * class triggers
   * the initializer
   *
   * @param className
   *        the class name
   * @return true if this class has a static initializer
   */
  private static boolean isSkipClass(final String className) {
    for (final String staticInitClass : SKIPCLASSES) {
      if (className.contains(staticInitClass)) {
        return true;
      }
    }
    return false;
  }

  private static String validateAttachment(final Class<? extends IAttachment> clazz) {
    final StringBuilder sb = new StringBuilder();
    for (final Method setter : clazz.getMethods()) {
      final boolean internalDoNotExportAnnotation = setter.isAnnotationPresent(InternalDoNotExport.class);
      final boolean startsWithSet = setter.getName().startsWith("set");
      final boolean gamePropertyAnnotation = setter.isAnnotationPresent(GameProperty.class);
      if (internalDoNotExportAnnotation && gamePropertyAnnotation) {
        sb.append("WARNING: Class ").append(clazz.getCanonicalName()).append(" setter ").append(setter.getName())
            .append(": cannot have both InternalDoNotExport and GameProperty annotations");
        continue;
      } else if (startsWithSet && !(internalDoNotExportAnnotation || gamePropertyAnnotation)) {
        sb.append("WARNING: Class ").append(clazz.getCanonicalName()).append(" setter ").append(setter.getName())
            .append(": begins with 'set' so must have either InternalDoNotExport or GameProperty annotation");
        continue;
      } else if (!startsWithSet && gamePropertyAnnotation) {
        sb.append("WARNING: Class ").append(clazz.getCanonicalName()).append(" setter ").append(setter.getName())
            .append(": does not begin with 'set' but has GameProperty annotation");
        continue;
      } else if (!startsWithSet || internalDoNotExportAnnotation) {
        // no error, we are supposed to ignore things that are labeled as ignore, or do not start with 'set'
        continue;
      } else if (!startsWithSet && !gamePropertyAnnotation) {
        sb.append("WARNING: Class ").append(clazz.getCanonicalName()).append(" setter ").append(setter.getName())
            .append(": I must have missed a possibility");
        continue;
      }
      Method getter;
      final GameProperty annotation = setter.getAnnotation(GameProperty.class);
      if (annotation == null) {
        sb.append("Class ").append(clazz.getCanonicalName()).append(" has ").append(setter.getName())
            .append(" and it doesn't have the GameProperty annotation on it\n");
      }
      if (!setter.getReturnType().equals(void.class)) {
        sb.append("Class ").append(clazz.getCanonicalName()).append(" has ").append(setter.getName())
            .append(" and it doesn't return void\n");
      }
      // the property name must be derived from the method name
      final String propertyName = getPropertyName(setter);
      // For debug purposes only
      // sb.append("TESTING: Class " + clazz.getCanonicalName() + ", setter property " + propertyName + "\n");
      // if this is a deprecated setter, we skip it now
      if (setter.getAnnotation(Deprecated.class) != null) {
        continue;
      }
      // validate that there is a field and a getter
      Field field = null;
      try {
        field = PropertyUtil.getFieldIncludingFromSuperClasses(clazz, "m_" + propertyName, false);
        // adders must have a field of type IntegerMap, or be a collection of sorts
        if (annotation.adds()) {
          if (!(Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType())
              || IntegerMap.class.isAssignableFrom(field.getType()))) {
            sb.append("Class ").append(clazz.getCanonicalName()).append(" has a setter ").append(setter.getName())
                .append(" which adds but the field ").append(field.getName())
                .append(" is not a Collection or Map or IntegerMap\n");
          }
        }
      } catch (final IllegalStateException e) {
        sb.append("Class ").append(clazz.getCanonicalName()).append(" is missing field for setter ")
            .append(setter.getName()).append(" with @GameProperty\n");
        continue;
      }
      final String resetterName = "reset" + capitalizeFirstLetter(propertyName);
      Method resetterMethod = null;
      try {
        resetterMethod = clazz.getMethod(resetterName);
        if (!resetterMethod.getReturnType().equals(void.class)) {
          sb.append("Class ").append(clazz.getCanonicalName()).append(" has a reset method ")
              .append(resetterMethod.getName()).append(" that doesn't return void\n");
        }
      } catch (final NoSuchMethodException e) {
        sb.append("Class ").append(clazz.getCanonicalName()).append(" doesn't have a resetter method for property: ")
            .append(propertyName).append("\n");
        continue;
      }
      final String getterName = "get" + capitalizeFirstLetter(propertyName);
      try {
        // getter must return same type as the field
        final Class<?> type = field.getType();
        getter = clazz.getMethod(getterName);
        if (!type.equals(getter.getReturnType())) {
          sb.append("Class ").append(clazz.getCanonicalName()).append(". ").append(getterName).append(" returns type ")
              .append(getter.getReturnType().getName()).append(" but property field is type ").append(type.getName())
              .append("\n");
        }
      } catch (final NoSuchMethodException e) {
        sb.append("Class ").append(clazz.getCanonicalName())
            .append(" doesn't have a valid getter method for property: ").append(propertyName).append("\n");
        continue;
      }
      if (annotation.adds()) {
        // check that there is a clear method
        final String clearName = "clear" + capitalizeFirstLetter(propertyName);
        Method clearMethod = null;
        try {
          clearMethod = clazz.getMethod(clearName);
        } catch (final NoSuchMethodException e) {
          sb.append("Class ").append(clazz.getCanonicalName())
              .append(" doesn't have a clear method for 'adder' property ").append(propertyName).append("\n");
          continue;
        }
        if (!clearMethod.getReturnType().equals(void.class)) {
          sb.append("Class ").append(clazz.getCanonicalName()).append(" has a clear method ")
              .append(clearMethod.getName()).append(" that doesn't return void\n");
        }
      } else if (!Modifier.isAbstract(clazz.getModifiers())) {
        // check the symmetry of regular setters
        @SuppressWarnings("unused")
        String method = null;
        try {
          final Constructor<? extends IAttachment> constructor =
              clazz.getConstructor(IAttachment.attachmentConstructorParameter);
          method = constructor.toString();
          final IAttachment attachment = constructor.newInstance("testAttachment", null, null);
          Object value = null;
          if (field.getType().equals(Integer.TYPE)) {
            value = 5;
          } else if (field.getType().equals(Boolean.TYPE)) {
            value = true;
          } else if (field.getType().equals(String.class)) {
            value = "aString";
          } else {
            // we do not handle complex types for now
            continue;
          }
          method = setter.toString();
          if (setter.getParameterTypes()[0] == String.class) {
            setter.invoke(attachment, String.valueOf(value));
          } else {
            setter.invoke(attachment, value);
          }
          method = getter.toString();
          final Object getterValue = getter.invoke(attachment);
          if (!value.equals(getterValue)) {
            sb.append("Class ").append(clazz.getCanonicalName()).append(", value set could not be obtained using ")
                .append(getterName).append("\n");
          }
          field.setAccessible(true);
          final Object fieldValue = field.get(attachment);
          if (!getterValue.equals(fieldValue)) {
            sb.append("Class ").append(clazz.getCanonicalName()).append(", ").append(getterName)
                .append(" returns type ").append(getterValue.getClass().getName()).append(" but field is of type ")
                .append(fieldValue.getClass().getName());
          }
        } catch (final NoSuchMethodException e) {
          sb.append("Warning, Class ").append(clazz.getCanonicalName()).append(" testing '").append(propertyName)
              .append("', has no default constructor\n");
        } catch (final IllegalArgumentException e) {
          sb.append("Warning, Class ").append(clazz.getCanonicalName()).append(" testing '").append(propertyName)
              .append("', has error: IllegalArgumentException: ").append(e.getMessage()).append("\n");
        } catch (final InstantiationException e) {
          sb.append("Warning, Class ").append(clazz.getCanonicalName()).append(" testing '").append(propertyName)
              .append("', has error: InstantiationException: ").append(e.getMessage()).append("\n");
        } catch (final IllegalAccessException e) {
          sb.append("Warning, Class ").append(clazz.getCanonicalName()).append(" testing '").append(propertyName)
              .append("', has error: IllegalAccessException: ").append(e.getMessage()).append("\n");
        } catch (final InvocationTargetException e) {
          // this only occurs if the constructor/getter or setter throws an exception, Usually it is because we pass
          // null to the constructor
          // sb.append("Warning calling " + method + " threw exception " + e.getTargetException().getClass() + "\n");
        }
      }
    }
    return sb.toString();
  }

  private static String getPropertyName(final Method method) {
    final String propertyName = method.getName().substring("set".length());
    char first = propertyName.charAt(0);
    first = Character.toLowerCase(first);
    return first + propertyName.substring(1);
  }

  private static String capitalizeFirstLetter(final String aString) {
    char first = aString.charAt(0);
    first = Character.toUpperCase(first);
    return first + aString.substring(1);
  }
}
