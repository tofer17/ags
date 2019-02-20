# Notes

---

## Background/History

I was playing Kittens Game and it struck me how easy it is to cheat and at about the same time my kid mentioned how to hack Quizlets. If you're not familiar, students can create and share "flash cards" for various topics. And they can compete to see who can get the highest score in the shortest time. Simple, since that game is JavaScript based, anyone can alter in-memory variables and post miraculous scores.

So that got me to thinking about how any game could be openly played yet with fully trusted outcomes despite the participants.

And that's Adversarial Gaming!

## Principle Concepts

### The Players

We assume that all participants are physically separated from one another. All participants receive every message sent by any single player.

We also **must** assume that players have other means of communicating with each other, might not be physically separated from other players, and, of course, are bent on cheating through surveillance, hacking, influence or coercion.

We can demonstrate when a message is sent successfully (in so far as it transmitted), but we cannot demonstrate that the message was otherwise digested by the recipient until the recipient replies. With that in mind, no player should fully trust the message-passer (the server) as it could have been compromised.

Thankfully, we **will** assume that:
* Each player's version of the software is not and cannot be compromised against that player:
    * Alice may hack her system for her benefit, but she can rest assured that no one has hacked it _against_ her, or others.
* Encryption (and hashing) cannot be broken or defeated in and of itself:
    * ...but should Alice choose a weak password and Bob figures it out, well, that's not the failure of the encryption.
* Players absolutely do **not** want to be called-out and proved-up as cheats:
    * Naturally, players could intentionally throw games, gang up on other players (collude), "peek" (at computer screens), or use social engineering; as a group the players should decide how to account for such instances.

### The Playbook

AGS relies on the concept that gameplay can be described as a series of recordable actions in accordance with the game's rules; and inspection of such a game-log would reveal invalid actions. This log is hereby known as The Playbook.

#### Example 1: Tic-tac-toe

Supposing that Alice and Bob decide to play tic-tac-toe and (for simplicity's sake) decide that Alice goes first. Alice and Bob agree to "view" the grid as if it were on a wall...

Thus, Alice makes the first move and it is recorded:
1. Alice places an "X" in the upper-left-corner cell.

Then Bob moves-- by adding to the log and so it becomes:
1. Alice places an "X" in the upper-left-corner cell.
1. Bob places an "O" in the center-most cell.

Then Alice...
1. Alice places an "X" in the upper-left-corner cell.
1. Bob places an "O" in the center-most cell.
1. Alice places an "X" in the upper-center cell.

And gameplay would commence until a victor or stalemate emerges. 

Lets now include, at the start of the game, what was left tacit in the example, like so:
1. Alice suggests a game of _tic-tac-toe_  using _common notation_, with her starting first.
1. Bob agrees.
1. Alice places an "X" in the upper-left-corner cell.
1. Bob places an "O" in the center-most cell.
1. Alice places an "X" in the upper-center cell.

Key Notes (for this simple example):
* Rules of the game, requirements and stipulations are set forth and agreed (Alice's agreement is implicit).
* The entire game can be recreated (and validated) at any point by either player.
* It would be evident if an illegal play was made by either player...
    * ...assuming it is impossible to alter previous entries in the playbook.
    * More on that following.

Some games would necessitate more expansive interplay messages-- such as chess, which is entirely feasible (using an agreed-upon [notation](https://en.wikipedia.org/wiki/Chess_notation)).

### A Little-Bit About Encryption

WIP. 

### Securing the Playbook

In the above example, after Bob's first move, Alice could rewrite the playbook and alter her first move. We can't allow her to do that.

AGS uses an enhanced blockchain for the playbook. Common blockchains employed by crypto currencies rely on a _proof of work_ principle which is not entirely appropriate.

Instead, the blockchain used in AGS relies on a nonce created by a signature of the previous hash; the nonce is hashed in the block, too. To recreate an earlier block, the hacker would need the signing keys of all (*) the signers ahead of it.
* (* or, rather, [51%](https://en.wikipedia.org/wiki/Double-spending#51%_attack))

Each _block_ in our Playbook holds one or more transactions made by a single player. The block does not represent the player's turn in so much as it represents one or more actions up until an action needs to be made by another player. For example, consider a round in _Magic - The Gathering (Portal)_ where first Alice would un-tap resources, summon, and then declare attackers (again, this is in MtG Portal)-- since no other player can be involved, it would be a variable list of transactions: un-tap Swamp, un-tap Island, tap Island, tap Island, summon Coral Eel, attack with Storm Crow. Alice's relinquishment of the playbook indicates, implicitly, that she is done declaring attackers; it's now Bob's turn to chose interceptors (blockers) and so forth.

The playbook includes an informational section that the messenger updates (with each transmission).

### Randomization

A simplicity made was that Alice simply decided to go first, and in tic-tac-toe that represents an unfair advantage. To be fair, Alice and Bob should flip a coin (draw a card, roll dice, etc.).

But Alice cannot simply say, "well I chose heads and when I flipped the coin it landed heads!" because Bob wouldn't buy that for a millisecond. Don't forget that we don't know if Alice and Bob are in the same room and can witness an actual coin flip together. 

WIP

### Timed Events

WIP

