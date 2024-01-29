# Versioning

## Terminology

**Version, Project Version, ProductVersion**: 
  Refers to a two digit value, eg: "2.6".

**Build Version**: 
  Refers to the product version plus the current GIT SHA. A 'build version'
  uniquely identifies a software version, eg: `2.6+ABC123`

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
If the next release version is not compatible, it would be '4.0'



## Build Version Semantics

```[product version]+[Git SHA]```

- **product version** - See above
- **Git SHA** - The latest Git SHA (commit version)

Eg:
```
2.5+abc214aa293fb
2.6+8805b3aa293fb
```

## Discussions and background

* <https://github.com/triplea-game/triplea/issues/4875>
* <https://github.com/triplea-game/triplea/issues/1739>
