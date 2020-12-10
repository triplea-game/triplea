# Building Installers

### Pre-Requisite
- install4j license key (can get from maintainers or an open-source one from install4j)

## Building installers
 - Install [Install4j 8](https://www.ej-technologies.com/download/install4j/files)
- Create a `triplea/gradle.properties` file with:
```bash
install4jHomeDir = /path/to/install4j8/
```

- Export install4j key:
```bash
export INSTALL4J_LICENSE_KEY={License key here}
```

- Run release task
```bash
./gradlew release
```

Installers will be created in `triplea/build/releases/`

