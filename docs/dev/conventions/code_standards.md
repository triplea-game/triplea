
# Java Conventions
- Follow: [Google java style](http://google.github.io/styleguide/javaguide.html)
- Install and use IDE checkstyle and formatting, see: [IDE setup notes](/docs/dev/setup/ide/)


### `null` handling
- avoid returning 'null' values
- avoid passing 'null' values as parameters to public APIs


### Deprecate Correctly
To deprecate add both a `@Deprecated` annotation  _and_ a `@deprecated` documentation 
in the javadocs. Include a comment on what can be done to avoid the deprecated call.

Example:
```
/**
 * @Deprecated Use 'fooBar()' instead
 */
 @deprecated
 public void foo(int param) {
 :
 :
```

## Depth first method ordering

For full details, please see *Chapter 5 'Formatting'* in [Clean Code](http://ricardogeek.com/docs/clean_code.html)

Example:
```java

public void firstPublicMethod() {
  firstPrivateMethodInvoked();
  secondPrivateMethodInvoked();
}

private void firstPrivateMethodInvoked() { }
private void secondPrivateMethodInvoked() { }

public void secondPublicMethod() { }
```

Note:
 - goal is to keep vertical distance between first usage and declaration reasonably short / minimized. 
 - checkstyle will require method overloads to be declared next to each other


## Variables
Define variables as close to their usage as possible.

