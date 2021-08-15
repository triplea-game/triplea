# Developer Setup Guide

## Getting Started

To get setup with IDE, building and running code, see:
[how-to/local-build-and-testing.md](./how-to/local-build-and-testing.md)


 ## Why Work on TripleA?

 * If you like the game and want to improve it
 * To get practice coding and working within an open source team
 * General coding practice and the reward of building something 



# Pitfalls and Pain Points to be aware of

TripleA can be a really rewarding project but has its difficulties. Development
on TripleA started in the early 2000s and has been ongoing since. Most difficulties
are from the codebase being larger and older.


## Save-Game Compatibility

- Game saves are done via object serialization. It is important therefore for object deserialization
  to work. The `GameData` class and anything that it references is saved, most of these classes extend
  `GameDataComponent`. To deserialize properly, all of the class fields may not (type and name).

- Network compatibility, '@RemoteMethod' indicates methods invoked over network. The API of these
  methods may not change.

## General Complexity

There is a lot of duct tape applied to the code, it is deceivingly complex and overly-complex.
To overcome this we are extracting and cleaning code as much as possible and retrofitting in
tests.

## Lack of Testing

There is a significant lack of test coverage, even with decent test coverage there can be non-obvious
behavior that you will probably only find via manual testing. Manual test changes in addition to
building as much automated testing as can be done reasonably..

