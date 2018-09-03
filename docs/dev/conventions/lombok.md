## Using Lombok
- Use narrow scopes, if you don't need public getters, set them to the most restrictive scope possible, eg:
```@Getter -> @Getter(access = AccessLevel.PACKAGE)```
- Avoid `@Data`, it is a mutable data structure, avoid the mutability when you can.
- Beware of circular loops with @ToString and @EqualsAndHashcode, if two classes have a circular dependency, and then you toString that, you'll get a loop. The insidious part is you only get this when `toString` is actually called, that may only be in for example a logging statement or on error.
