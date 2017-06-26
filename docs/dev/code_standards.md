Unless specified otherwise, follow: [Google java style](http://google.github.io/styleguide/javaguide.html)

## Checkstyle

Introducing new checkstyle violations will fail the build, 
for details see [Checkstyle]({{ "/dev_docs/dev/checkstyle/" | prepend: site.baseurl }}) 

## Code is formatted

Auto-formatter has been applied, for auto-formatter setup see:
[New Dev Setup]({{ "/dev_docs/dev/new_dev_setup/" | prepend: site.baseurl }})


## Guidelines and Preferences

###  Avoid copy paste
DRY: [do not copy and paste](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself). 

### Prefer to spell out variable names
Notable exceptions, loop index 'i', exception 'e'.

### Prefer library implementations when available
Do not re-invent the wheel. Prefer first `java.lang`, then we have available guava, apache commons


### Avoid using `null` as part of public APIs

Said another way, do not pass `null` arguments, and do not return null. 
Some techniques to avoid `null` are: 
 - add multiple method signatures to omit `null` parameters
 - use naturally empty objects, like an empty list instead of null. 
 - `Optional` can sometimes be a good choice for return values.

### Deprecate Correctly
When you have a good reason to deprecate a method or class, add both a `@Deprecated` annotation 
_and_ a `@deprecated` documentation in the javadocs. Always include a comment about how to handle
the deprecation, can callers remove the altogether, should they replace it with a different API?

Example:
```
/**
 *  <...javadocs....>
 * @Deprecated Use 'fooBar()' instead
 */
 @deprecated
 public void foo(int param) {
 :
 :
```

### Handle exceptions

- `ClientLogger` is a utility class to log errors. Can be used as: `ClientLogger.logError(e)`
- Always log surrounding context, if there are any args or relevant variable values, log their values, for example:

```
public int processXmlData(Unit unit, XmlTransmitter transmitter) {
  try {
     transmitter.send(unit)
  } catch (IOException e ) {
     String failMsg = String.format("Failed to send unit data: %s, using transmitter: %s", unitData, transmitter);
     ClientLogger.logError(failMsg, e);
  }
}
```

In the above, note that we are logging the values of the two method arguments. If there were any other interesting
 variable values in the method or class, we would log those too. Without this information, if we ever do get an 
 exception, and it is related to data, we'll be scratching our heads on how to reproduce the problem.

### Mock Objects
Prefer replacing hand crafted mock objects with mockito.


## Variable and Method Ordering


### Methods

Dependent methods are grouped together.  For some good background reading and details on how to do this, 
please see Chapter 5 'Formatting' in [Clean Code](http://ricardogeek.com/docs/clean_code.html)



#### IntelliJ Formatter Option for Method Ordering
![keep_dependents_first](https://user-images.githubusercontent.com/12397753/27557429-72fb899c-5a6e-11e7-8f9f-59cc508ba86c.png)

#### Method ordering summary

- private methods are defined as soon as possible after first usage. For example, the constructor as the first
code block will be followed by any private methods that are used in the constructor. Following that we will define
the first public method, and then any private methods that it uses. 


Example:
```
public class Foo {
  // variables
  
  // constructor
  
  // private methods called by constructor
  
  // first public method
  public void firstPublicMethod() {
    firstPrivateMethodCalled();
    secondPrivateMethodCalled();
  }

  // private methods called by first public method, in order in which they are called
  private void firstPrivateMethodCalled() { }
  private void secondPrivateMethodCalled() { }


  // second public method
  
  // any new private methods called by second public method
}



```

### Variables

This is similar to methods, they are defined as close to their usage as possible. Rule of thumb, minimize the number
of lines between declaration and first usage. Another way to think of this, do not declare variables all at top,
declare them before usage.

Example:

```
int first = 2;
int firstSquared = first * first;

int second = 3;
int secondSquared = second * second;

double distance = Math.sqrt(firstSquared + secondSquared);</code></pre>
```

Instead of:
```
int first = 2;
int second = 3;

int firstSquared = first * first;
int secondSquared = second * second;

double distance = Math.sqrt(firstSquared + secondSquared);</code></pre>
```
