# rpc_currency

This readme is specific instructions for the rpc_currency module.

## Building and Running

To run the project in the command line or IDE use:

```bash
# On Linux/Mac
./gradlew run
# On Windows Powershell
.\gradlew.bat run
```

To build the project into a jar file
```bash
# On Linux/Mac
./gradlew build
# On Windows Powershell
.\gradlew.bat build
```

two separate jars are found in the `build/libs/` directory. `app.jar` and `app-bundle.jar`. `app.jar` will not run
and does not include the needed dependencies. It is a flat source jar. `app-bundle.jar` includes all dependencies and
may be run from the command line using `java -jar app-bundle.jar`
