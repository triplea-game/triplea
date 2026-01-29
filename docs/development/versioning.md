# Versioning

## Terminology

**Version, Project Version, ProductVersion**:
  Refers to a two digit value, eg: "2.6".

**Build Version**:
  Refers to the product version plus the current commit count. Because
  we enforce squash-commits & have a linear history, a 'build version'
  uniquely identifies a software version, eg: `2.6.12300`

## Product Version Semantics

Simplified 2 number versioning system based on semantic versioning:

```[compatibility].[release]```

- **compatibility** - Incremented when a new version is not backwards compatibility.
- **Release** - Incremented when the TripleA teams recommends users to upgrade
  to the latest version.

EG:
```
3.0
2.6
2.3
```

### Example Scenarios for Product Version Increments

For example, let's say 3.0 is current. If the team releases a new version, it would be '3.1'
If the next release version is not compatible, it would be '4.0'  (TODO: what exactlky
does "not compatible" mean? For now that means at least save-game compatibility, where
for example 3.0 save games could not be read, then the next version would be a 4.0)

## Build Version Semantics

```[product version].[Commit Number]```

- **product version** - See above
- **Commit Number** - How many commits there are on master. Never goes down.

Eg:
```
2.5.1000
2.6.2000
```

## Discussions and background

* <https://github.com/triplea-game/triplea/issues/4875>
* <https://github.com/triplea-game/triplea/issues/1739>
