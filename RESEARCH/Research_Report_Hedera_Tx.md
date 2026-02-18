# Research Report

## Hedera Account Lifecycle and HBAR Transfers

### Summary of Work
I researched and implemented a lifecycle application for Hedera accounts using their Java SDK. 
This involved creating new accounts with some initial funding from an operator account, executing some HBAR transactions,
(HBAR) being Hedera's default currency, then querying the ledger via their mirror node for an updated set of balanced. 
Finally I delete the accounts to recover the money sent upon their creations.

### Motivation
In order to scaffold out the cryptocurrency system for this project, we need to go beyond token minting and understand
how to transact with and manager user entities. Specifically, we need to validate how to "onboard" our users, and allow
them to facilitate ptp (peer to peer) payments. Finally we need to figure out how to query user balances for other
business logic. I also wanted to make sure I can clean up test accounts to preserver the Operator's testnet funds. This
will be helpful for building out our code testing.

### Time Spent
~30 minutes of research
~45 minutes of reading and following various tutorials
~10 minutes of troubleshooting
~20 minutes of writing this report

### Results
I successfully built up a Java simulation app that does some transactions that flow between two made up users,
Alice and Bob. You can find the full source on the project repository[^4]

#### Account Creation & Funding
I learned that creating an account and funding it is really easy and can be done in a single transaction.
By using `AccountCreateTransaction#setInitialBalance()`m the network automatically works to deduct funds
frmo the Operator and sends them to the new account.
```java
AccountCreateTransaction transaction = new AccountCreateTransaction()
        .setKeyWithAlias(pub)
        .setAccountMemo(nickname)
        // funds the account with 20 Hbar out of the Operator account
        .setInitialBalance(new Hbar(20));
```

#### Peer-to-Peer Transfers
I implemented the `TransferTransaction` to test moving HBAR between Alice and Bob. This is pretty pivotal fo rour
project that Hedera transfers nicely since its largely built around transactions. Hedera makes sure the sum of
inputs and outputs is zero before it sends off the transaction.
```java
TransferTransaction transaction = new TransferTransaction()
        .addHbarTransfer(sender.id(), new Hbar(-20)) // Deduct from sender
        .addHbarTransfer(receiver.id(), new Hbar(20))  // Add to receiver
        .freezeWith(client)
        .sign(sender.priv()); // Sender must sign to authorize
```

#### Mirror Node Querying with Latency Handling
Since `AccountBalanceQuery` is deprecated for removal around April 2026. I instead implemented a REST client
using the HttpClient, this is the reccomended secondary approach for recieving a balance. There are also block listeners
which are good for constnat reactive updates. This is actually probably smarter as a marketplace to explore this more.
I discovered that Mirror Nodes are going to have a delay in the propagation compared to the Consensus Node. To handle this
i did a naive `Thread#sleep`. Not really smart in production though so we definitely need to look into this more.
```java
// Wait for Mirror Node to ingest the transaction record
try {
    Thread.sleep(5000);
} catch (InterruptedException e) {
    throw new RuntimeException(e);
}
// Query the REST API
String mirrorNodeUrl = "[https://testnet.mirrornode.hedera.com/api/v1/balances?account.id=](https://testnet.mirrornode.hedera.com/api/v1/balances?account.id=)" + account.id();
```

#### Account Cleanup
Finally I explored cleaning up accounts, this will also be useful if a user deletes their account off our platform if we
plan to support that. Also this is just polite to clean up after yourself on the testnet. I implemented `AccountDeleteTransaction`.
This one will remove an account from the ledger and transfers the remaining HBAR back to our operator (guarantor). 
```java
AccountDeleteTransaction transaction = new AccountDeleteTransaction()
        .setAccountId(account.id())
        .setTransferAccountId(guarantor.id()) // Send remaining funds here
        .freezeWith(client)
        .sign(account.priv());
```

### Sources
- Hedera Account Creation Docs[^1]
- Hedera Transfer Transaction Docs[^2]
- Mirror Node REST API[^3]
- Project Repository[^4]
- Signing Transactions[^5]

[^1]: https://docs.hedera.com/hedera/sdks-and-apis/sdks/accounts/create-an-account
[^2]: https://docs.hedera.com/hedera/sdks-and-apis/sdks/transactions/transfer-cryptocurrency
[^3]: https://docs.hedera.com/hedera/sdks-and-apis/rest-api/balances
[^4]: https://git.doit.wisc.edu/cdis/cs/courses/cs506/sp2026/team/t_32/research-hedera/-/tree/research/transacting?ref_type=heads
[^5]: https://docs.hedera.com/hedera/sdks-and-apis/sdks/transactions/manually-sign-a-transaction
