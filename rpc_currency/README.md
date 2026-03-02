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

## Account setup for Hedera
The operator account ID is the Hedera account that acts as the treasury for our token. It serves these key purposes:
    - Holds the initial supply of tokens when the token is created
    - Manages token distribution — all minted tokens go to this account
    - Required for every token creation transaction

It's simply a standard Hedera account ID (e.g., 0.0.47938602) that you designate as the token's treasury.



## Token Creation
We create our own cryptocurrency $RPC on Hedera using the Hedera Token Service (HTS). We use a function called TokenCreateTransaction to create our token, which HTS supports natively.

