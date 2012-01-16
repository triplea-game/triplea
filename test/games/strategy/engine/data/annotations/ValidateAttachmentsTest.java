package games.strategy.engine.data.annotations;

import games.strategy.engine.data.IAttachment;
import games.strategy.util.IntegerMap;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * A test that validates that all attachment classes have properties with valid setters and getters
 * 
 * @author Klaus Groenbaek
 */
public class ValidateAttachmentsTest extends TestCase
{
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	/**
	 * Test that the Example Attachment is valid
	 * 
	 * @throws Exception
	 *             on error
	 */
	public void testExample() throws Exception
	{
		validateAttachment(ExampleAttachment.class);
	}
	
	/**
	 * Tests that the algorithm finds invalidly named field
	 */
	public void testInvalidField()
	{
		try
		{
			validateAttachment(InvalidFieldNameExample.class);
			fail("Test should fail, as the field name doesn't match the setter name");
		} catch (final Exception e)
		{
			assertTrue("incorrect error message", e.getMessage().contains("Missing field for setter"));
			// expected
		}
	}
	
	/**
	 * tests that the algorithm will find invalid annotation on a getters
	 */
	public void testAnnotationOnGetter()
	{
		try
		{
			validateAttachment(InvalidGetterExample.class);
			fail("Test should fail, as we have annotated a getter");
		} catch (final Exception e)
		{
			assertTrue("incorrect error message", e.getMessage().contains("with @GameProperty which is not a setter"));
			// expected
		}
	}
	
	/**
	 * Tests that the algorithm will find invalid return types
	 */
	public void testInvalidReturnType()
	{
		try
		{
			validateAttachment(InvalidReturnTypeExample.class);
			fail("Test should fail, as we have an invalid return type");
		} catch (final Exception e)
		{
			assertTrue("incorrect error message", e.getMessage().contains("has incorrect return type"));
			// expected
		}
	}
	
	/**
	 * Tests that the algorithm will find invalid clear method
	 */
	public void testInvalidClearMethod()
	{
		try
		{
			validateAttachment(InvalidClearExample.class);
			fail("Test should fail, as we have an invalid clear method");
		} catch (final Exception e)
		{
			assertTrue("incorrect error message", e.getMessage().contains(" has doesn't have a clear method for property"));
			// expected
		}
	}
	
	/**
	 * Tests that the algorithm will find adders that doesn't have type IntegerMap
	 */
	public void testInvalidFieldType()
	{
		try
		{
			validateAttachment(InvalidFieldTypeExample.class);
			fail("Test should fail, as we have an invalid field type");
		} catch (final Exception e)
		{
			assertTrue("incorrect error message: " + e.getMessage(), e.getMessage().contains("is not an IntegerMap"));
			// expected
		}
	}
	
	/**
	 * Scans the compiled /classes folder and finds all classes that implement IAttachment to verify that
	 * all @GameProperty have valid setters and getters
	 * 
	 * @throws Exception
	 *             on error
	 */
	public void testAllAttachments() throws Exception
	{
		// find the classes folder
		final URL url = getClass().getResource("/");
		final File file = new File(url.toURI());
		findAttachmentsAndValidate(file);
	}
	
	// file to find classes or directory
	FileFilter classOrDirectory = new FileFilter()
	{
		public boolean accept(final File file)
		{
			return file.isDirectory() || file.getName().endsWith(".class");
		}
	};
	
	/**
	 * Recursive method to find all classes that implement IAttachment and validate that they use the @GameProperty
	 * annotation correctly
	 * 
	 * @param file
	 *            the file or directory
	 * @throws Exception
	 *             on error
	 */
	private void findAttachmentsAndValidate(final File file) throws Exception
	{
		if (file.isDirectory())
		{
			final File[] files = file.listFiles(classOrDirectory);
			for (final File aFile : files)
			{
				findAttachmentsAndValidate(aFile);
			}
		}
		else
		{
			final String fileName = file.getAbsolutePath();
			final String classesRoot = File.separatorChar + "classes" + File.separatorChar;
			final int index = fileName.indexOf(classesRoot) + classesRoot.length();
			String className = fileName.substring(index);
			className = className.replace(File.separator, ".");
			className = className.substring(0, className.lastIndexOf(".class"));
			
			if (isSkipClass(className))
			{
				return;
			}
			
			final Class<?> clazz = Class.forName(className);
			if (!clazz.isInterface() && IAttachment.class.isAssignableFrom(clazz))
			{
				final Class<? extends IAttachment> attachmentClass = (Class<? extends IAttachment>) clazz;
				validateAttachment(attachmentClass);
			}
			
		}
	}
	
	/**
	 * todo(kg) fix this
	 * ReliefImageBreaker and TileImageBreaker has a static field that opens a save dialog!!!
	 * "InvalidGetterExample", "InvalidFieldNameExample", "InvalidReturnTypeExample" are skipped because they are purposely invalid, and use to test the validation algorithm
	 * 
	 */
	public static List<String> SkipClasses = Arrays.asList("ReliefImageBreaker", "TileImageBreaker",
				"InvalidGetterExample", "InvalidFieldNameExample", "InvalidReturnTypeExample", "InvalidClearExample", "InvalidFieldTypeExample");
	
	/**
	 * Contains a list of classes which has static initializes, unfortunately you can't reflect this, since loading the class triggers
	 * the initializer
	 * 
	 * @param className
	 *            the class name
	 * @return true if this class has a static initializer
	 */
	private boolean isSkipClass(final String className)
	{
		for (final String staticInitClass : SkipClasses)
		{
			if (className.contains(staticInitClass))
			{
				return true;
			}
		}
		return false;
	}
	
	private void validateAttachment(final Class<? extends IAttachment> clazz) throws Exception
	{
		for (final Method setter : clazz.getMethods())
		{
			Method getter;
			if (setter.isAnnotationPresent(GameProperty.class))
			{
				final GameProperty annotation = setter.getAnnotation(GameProperty.class);
				String propertyName = null;
				
				// the property name must be derived from the method name
				if (!setter.getName().startsWith("set"))
				{
					throw new Exception("Class " + clazz.getCanonicalName() + " has " + setter.getName() + " with @GameProperty which is not a setter");
				}
				
				propertyName = getPropertyName(setter);
				
				// validate that there is a field and a getter
				Field field = null;
				try
				{
					field = clazz.getDeclaredField("m_" + propertyName);
					// adders must have a field of type IntegerMap, for regular setters we don't know the type
					if (annotation.adds())
					{
						if (!field.getType().equals(IntegerMap.class))
						{
							throw new Exception("Class " + clazz.getCanonicalName() + " has a setters which adds but the field " + field.getName() + " is not an IntegerMap");
						}
					}
				} catch (final NoSuchFieldException e)
				{
					throw new Exception("Missing field for setter " + setter.getName() + " with @GameProperty ");
				}
				
				final String getterName = "get" + capitalizeFirstLetter(propertyName);
				try
				{
					// getter must return same type as the field
					final Class<?> type = field.getType();
					
					getter = clazz.getMethod(getterName);
					if (!type.equals(getter.getReturnType()))
					{
						throw new Exception("Class " + clazz.getCanonicalName() + " has a Getter for property " + propertyName + " which has incorrect return type");
					}
				} catch (final NoSuchMethodException e)
				{
					throw new Exception("The attachment " + clazz.getCanonicalName() + " doesn't have a valid clear method for property: " + propertyName);
				}
				
				if (annotation.adds())
				{
					// check that there is a clear method
					final String clearName = "clear" + capitalizeFirstLetter(propertyName);
					Method clearMethod = null;
					try
					{
						clearMethod = clazz.getMethod(clearName);
					} catch (final NoSuchMethodException e)
					{
						throw new Exception("The attachment " + clazz.getCanonicalName() + " has doesn't have a clear method for property " + propertyName);
					}
					if (!clearMethod.getReturnType().equals(void.class))
					{
						throw new Exception("The attachment " + clazz.getCanonicalName() + " has a clear method that doesn't return void");
					}
				}
				else
				{
					
					// check the symmetry of regular setters
					try
					{
						final Constructor<? extends IAttachment> constructor = clazz.getConstructor();
						final IAttachment attachment = constructor.newInstance();
						Object value = null;
						if (field.getType().equals(Integer.TYPE))
						{
							value = 5;
						}
						else if (field.getType().equals(Boolean.TYPE))
						{
							value = true;
						}
						else if (field.getType().equals(String.class))
						{
							value = "aString";
						}
						else
						{
							// we do not handle complex types for now
							continue;
						}
						
						setter.invoke(attachment, String.valueOf(value));
						final Object getterValue = getter.invoke(attachment);
						if (!value.equals(getterValue))
						{
							throw new Exception("The attachment " + clazz.getCanonicalName() + ", value set could not be obtained using " + getterName);
						}
						field.setAccessible(true);
						if (!getterValue.equals(field.get(attachment)))
						{
							throw new Exception("The attachment " + clazz.getCanonicalName() + ", value obtained through " + getterName + " doesn't match field");
						}
						
					} catch (final NoSuchMethodException e)
					{
						System.out.println("Warning, class " + clazz.getCanonicalName() + " has no default constructor");
					}
					
				}
			}
		}
	}
	
	private String getPropertyName(final Method method)
	{
		final String propertyName = method.getName().substring("set".length());
		char first = propertyName.charAt(0);
		first = Character.toLowerCase(first);
		return first + propertyName.substring(1);
	}
	
	private String capitalizeFirstLetter(final String aString)
	{
		char first = aString.charAt(0);
		first = Character.toUpperCase(first);
		return first + aString.substring(1);
	}
	
}
