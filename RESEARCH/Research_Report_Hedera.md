# Research Report

## Intro to Hedera Hashgraph Token Deployment

### Summary of Work

I researched how to utilize the Hedera Hashgraph network and its Java SDK to deploy a demo cryptocurrency token. I followed the getting started documentation to configure gradle and add needed dependencies, set up a testnet account on the Hedera testnet, and successfully minted a fungible token using the Hedera Token Service.

### Motivation

I immediately knew building a blockchain from scratch was not feasable and previously researched Corda did not cut it due to the buggyness. Hedera provides a pretty nice Java SDK and a built-in token service that aligns with the project without requiring learning Solidity meaning all team members could participate and to some extent read and understand the Hedera contract and deployment code smart contract.

### Time Spent

~30 minutes researching Hedera documentation and configuring my testnet environment via the developer portal
~30 minutes writing the Java deployment script and troubleshooting cryptographic key parsing errors

### Results

I started by setting up a testnet account through the Hedera Developer Portal[^3] to obtain my `OPERATOR_ID` and a private key.
First, I created a new Java project with gradle init and added the Hedera Java SDK as a dependency as well as deps needed for the tutorial such as guava. Then, I attempted to initialize the Hedera client and run the token creation demo[^2]:
```java
AccountId operatorId = AccountId.fromString(System.getenv("OPERATOR_ID"));
PrivateKey operatorKey = PrivateKey.fromString(System.getenv("OPERATOR_KEY"));

Client client = Client.forTestnet().setOperator(operatorId, operatorKey);
```
When executing the transaction, I encountered a `PrecheckStatusException` with an `INVALID_SIGNATURE` status. I realized that because my portal had generated a hex-encoded key, the standard `fromString()` method was parsing it as an ED25519 key instead of an ECDSA key, causing the mathematical signature to fail on the network.
I corrected the key parsing method to explicitly handle the ECDSA hex string (ENSURE YOU USE PrivateKey#fromStringECDSA):
```java
PrivateKey operatorKey = PrivateKey.fromStringECDSA(System.getenv("OPERATOR_KEY"));
```
With the client authenticated, I used the `TokenCreateTransaction` class to define and deploy the "DEMO" token directly to the ledger:
```java
PrivateKey supplyKey = PrivateKey.generateED25519();
TokenCreateTransaction transaction = new TokenCreateTransaction()
        .setTokenName("Example Token")
        .setTokenSymbol("DEMO")
        .setDecimals(2)
        .setInitialSupply(100_000)
        .setTreasuryAccountId(operatorId)
        .setAdminKey(supplyKey.getPublicKey())
        .freezeWith(client);

TransactionResponse txResponse = transaction.sign(supplyKey).execute(client);
TransactionReceipt receipt = txResponse.getReceipt(client);
System.out.println("Fungible token created: " + receipt.tokenId);
```
The transaction executed successfully, returning a new Token ID of `0.0.7928809`. I then verified that the token was live on the network by looking up the ID on the testnet HashScan block explorer[^4]. It confirmed the total supply, token name, and treasury account assignment. 

Now we run some excess code that can be found the the Research-Hedera repository on gitlab that will confirm our coins existance and deployment on the testnet the
final output is
```
Fungible token created: 0.0.7929435                                                                                                                                      
Waiting for Mirror Node to update...
Treasury holds: 100000 DEMO
```

#### Summary

For now this is all the research needed to be done on Hedera overall. Being able to produce a token in around 100 lines of code is very efficient and effective. It seems that transactions flow very well with the Builder-like style and the coin seems like it will be easy to integrate into our current plans for the backend and front end seeing as we need only interact with some java code and a REST API from Hedera.

### Sources
- Hedera Testnet Networks[^1]
- Hedera Token Creation Guide[^2]
- Hedera Developer Portal[^3]
- HashScan Testnet DEMO Token Explorer[^4]
- Hedera Sample Code[^5]

[^1]: https://docs.hedera.com/hedera/networks/testnet
[^2]: https://docs.hedera.com/hedera/getting-started-hedera-native-developers/create-a-token#common-error-messages-and-solutions
[^3]: https://portal.hedera.com/dashboard
[^4]: https://hashscan.io/testnet/account/0.0.7928809
[^5]: https://git.doit.wisc.edu/cdis/cs/courses/cs506/sp2026/team/t_32/research-hedera
