# Research Report

## Corda Open Source Blockchain

### Summary of Work

The research below is about the Corda open source blockchain to assess its usability. I will be reading their documentation website and following a tutorial
within this document that should act as a simple currency.

### Motivation

Our project is based around transacting a cryptographic currency on a marketplace. Corda being one of the only options as an open source block chain in
Java exclusively, not requiring knowledge of solidity or any other language such as JavaScript or Golang could be a good candidate for the basis of our currency.

### Time Spent

<!--Explain how your time was spent-->
~15 minutes skimming Corda documentaiton to assess usefullness
~30 minutes reading Corda Documentation and watching documenting videos
~45 minutes following Corda generic template tutorial
~30 minutes trying to setup coin based example.

## Results

#### Feasability

Corda itself seems like a feasible option, however, based on my interaction with the project it largely seems the examples and documentation is outdated.
Installation is clunky. If we go with this option it will take lots of troubleshooting to get things in order and ensure their tech stack works with little help from
documentation or forms. Online examples are scarce and the ones that do exist don't function typically even on initial clone with no modification.

#### Other Information

This is the information I otherwise gathered on Corda.

Corda Blockchain has multiple key concepts that will be explained below.

#### Ledger

In a nutshell "a ledger is a database of facts that’s replicated, shared, and synchronized across multiple participants on a network."
Participants in a ledger are called nodes. And they store a copy of the ledger.

Within corda like other blockchains there is no central store of data, instead each node maintains its own database and database of facts.
If multiple different nodes A and B are on a singular network (loose definition here not like a singular localhost) they share their ledgers with eachother
shared facts (or rather data points), will have identical data between node A and B whenever either has a shared data point which is updated.

#### States

A state is simply a fact or "datapoint" on the ledger, states are also immutable and may be known to multiple nodes or only a singular one.
To keep track of states sequencing is used. These sequences are used to track states as they evolve. As the example on the official documentation states
let's imagine that there are 2 parties, Alice and Bob and Alice owes Bob $10. Such a state sequence might look like: `No Agreement -> Alice owes Bob $10`.
If Alice then pays Bob 5 of the 10 dollars the sequence would evolve to look more like `No Agreement -> Alice owes Bob $10 (Historic) -> Alice owes Bob $10 ($5 paid)`. As you see the previous head of the sequence is marked as historic and an updated head is placed on the sequence. 

Each node of each agreement is stored in a node's vault (which is just a fancy database). This vault stores current and historic states where as the node's ledger
is specifically the "current" states.

#### Flows

Flows are a communication stream from node to node. Flows are used to automate the process of agreeing to ledger updates (confirming validity) between multiple different
nodes. Rather than a global broadcast like a lot of blockchains do corda broadcasts these changes node to node. Here is a good visiual from the official documentation:
![Image showing flows in action as communication occurs between Alice and Bob](https://docs.r3.com/en/images/flow.gif)

A flow tells the ledger how it can achieve some sort of update.

Node Operators (Just the person who owns the node) use RPC (Remote Procedure Call) to tell their given nodes how to handle specific flows. This abstracts a way
networking I/O, and other things.

There are also subflows which can be triggered by a flow (this is analogous to subtasks or threads). the parent flow waits until all subflows have compelted to finish.

### Sources

- Ledger[^1]
- States[^2]
- Flows[^3]
- Token[^4]

[^1]: https://docs.r3.com/en/platform/corda/4.8/enterprise/key-concepts-ledger.html
[^2]: https://docs.r3.com/en/platform/corda/4.8/enterprise/key-concepts-states.html
[^3]: https://docs.r3.com/en/platform/corda/4.8/enterprise/key-concepts-flows.html
[^4]: https://github.com/corda/corda5-samples/blob/V5_2/java-samples/shinny-tokens
