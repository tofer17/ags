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

> WIP.

AGS uses encryption and hashing algorithms as provided by the HTML Web Crypto API (``window.crypto.subtle.*``).

There are a variety of ways to encrypt, sign and hash-- and for each a variety of intensities (key lengths and other options).

AGS _wants_ to always use the highest level available-- but for some that might be computationally prohibitive. It's up to the participants to agree on the level of encryption to be used.

What is more important than key/hash strength is algorithm selection.

These notes oversimplify encryption and hashing, and tend to just use the phrase "key" when terms like symmetric or asymmetric _could/should_ be used. So please don't gawk at that.

One tenant of AGS is player disconnect. Alice might start the game on her laptop in a coffee shop, later play a few rounds on her cell phone on the bus, all before winding down with her tablet. But she's **not** porting-about a collection of files and public/private keys. She starts the game with a secret (password or pass-phrase), and that secret is used to seed entropy for keys (symmetric or asymmetric), signatures and hashes.

That said, there should be no reason that AGS cannot support importing keys to the extent the player is willing to obtain and manage them; and that the Web Crypto API supports it.

AGS does not, however, store any session data beyond a cookie for server-session management.

### Securing the Playbook

In the above example, after Bob's first move, Alice could rewrite the playbook and alter her first move. We can't allow her to do that.

AGS uses an enhanced blockchain for the playbook. Common blockchains employed by crypto currencies rely on a _proof of work_ principle which is not entirely appropriate.

Instead, the blockchain used in AGS relies on a nonce created by a signature of the previous hash; the nonce is hashed in the block, too. To recreate an earlier block, the hacker would need the signing keys of all (*) the signers ahead of it.
* (* or, rather, [51%](https://en.wikipedia.org/wiki/Double-spending#51%_attack))

Each _block_ in our Playbook holds one or more transactions made by a single player. The block does not represent the player's turn in so much as it represents one or more actions up until an action needs to be made by another player. For example, consider a round in _Magic - The Gathering (Portal)_ where first Alice would un-tap resources, summon, and then declare attackers (again, this is in MtG Portal)-- since no other player can be involved, it would be a variable list of transactions: un-tap Swamp, un-tap Island, tap Island, tap Island, summon Coral Eel, attack with Storm Crow. Alice's relinquishment of the playbook indicates, implicitly, that she is done declaring attackers; it's now Bob's turn to chose interceptors (blockers) and so forth.

The playbook includes an informational section that the messenger updates (with each transmission).

### Randomization (of a set)

A simplicity made was that Alice simply decided to go first, and in tic-tac-toe that represents an unfair advantage. To be fair, Alice and Bob should flip a coin (draw a card, roll dice, etc.).

But Alice cannot simply say, "well I chose heads and when I flipped the coin it landed heads!" because Bob wouldn't buy that for a millisecond. Don't forget that we don't know if Alice and Bob are in the same room and can witness an actual coin flip together. 

Instead, a Mental Poker technique is used:
1. Alice creates two messages (strings): "Alice goes First" and "Bob goes First".
1. She signs each (so now each object is a compound of the string and her signature.
1. Then Bob reviews and accepts (he wants to ensure fairness); he signs each, too (each is now a compound of the string and a list of Alice's and Bob's signatures).
1. Alice then "shuffles" the two objects and encrypts them. She now has two encrypted objects; she knows which is which, but Bob doesn't.
1. Bob "shuffles" the two objects and encrypts each. The two objects are still encrypted objects, and neither Bob nor Alice know which is which.
1. Alice decrypts each object-- leaving behind encrypted objects that she cannot know because they are still encrypted by Bob's key. She then re-encrypts each object.
1. Bob now decrypts each object-- leaving behind two encrypted objects that he cannot know as they are still encrypted by Alice's key.
1. Either Alice or Bob can perform the "coin flip" by requesting of the other their key to one object or the other. Let's suppose Alice asks Bob, "please give me the key for the object finger-printed "17-af...". 
1. Bob does so: Alice can now decrypt the chosen object with her key, and again with Bob's key (that he just supplied to her), and reveal the dual-signed object instructing who goes first.
10. Alice knows Bob didn't tamper with the outcome because she can verify her signature-- as can Bob.

Pretty slick!

The same technique can be used for cards, dominoes, dice, and so on.

### Random Numbers


> This is a WIP and hasn't been fully worked out let alone implemented.


The Mental Poker technique works for sets; but for random numbers of n-bits another technique is better suited-- lets suppose for example that we need a 64 bit floating point number:
1. Alice decides off the top of her head a number between 0 and 2048 via string "I choose 17"; she encrypts it to keep it secret. The encryption will be an array of numbers.
2. Bob decides off the top of his head a number between 0 and 2048 and staples that onto Alice's encrypted note; he then signs it. Now the object has a payload of an array of numbers (Alice's encrypted object), a number Bob chose, and his signature (of the combined payload...).
3. Alice now furnishes the key she used in the first place-- her number becomes visible to both. She adds Bob's number to her's as the starting bit index (of the array of numbers) and collects 64-bits from there (modulating).
4. If the number Alice arrived at needed to be kept secret, then Alice need only embargo her key until it's needed.
5. Bob can check that, using her key, to get her number which he adds to his and then computes the same sequence of bits.

In the example above, "2048" should be replaced with a number closer to the expected length (in bits) of Alice's ciphertext.

Alternatives could be to use Bob's signature, or some product of Alice's array and Bob's signature (XOR?) as the source to collect bits from. 

What's left to figure out, although: once Alice picks her number and encrypts that, both Alice and Bob can easily derive every possible 64 bit combination-- neither will know which combination is ultimately chosen, however. 

### Timed Events (and Escrow)

Having mentioned the [Kitten's Game](http://bloodrizer.ru/games/kittens/wiki/index.php?page=Time) earlier:

> "Time is an illusion. Lunchtime, doubly so."
> Douglas Adams

Many games incorporate a time component that can be summarized as "some specific future point in time." Boggle players have 3 minutes once the board is revealed to sleuth words. In some instances there needs to be "wait until a certain time" before something is allowed to happen; e.g., "...when the timer starts you may look at your cards."

Time is tricky because we have no guarantees of execution environment standards. Alice and Bob can each claim mutually exclusive points of view on any given time-- each can manipulate their clocks ahead or behind.

Because of that, simply put, Alice and Bob **must** agree to observe time from a third party source and trust that it has not been compromised.

In AGS, the messenger-- the server-- can play that role to the extent that participants trust it. Participants can send a message to the server with a timestamp. If, and only if, the timestamp is in the future (from the POV of the server), the server will return that very message encrypted (based on the timestamp). But if the timestamp is the present (unlikely) or past, then the server will respond with a key that it would have used, in the past, to encrypt whatever. Lastly, if the timestamp is missing, the server will return the current time digitally signed and along with a public key for verification.

In this way, players can escrow (or embargo) into the playbook their keys encrypted encrypted by the server, up to a future point in time.

Suppose the situation where we want to say, "When the timer starts at exactly 12:34:01.0001 UTC, you may look at your cards" where the cards each player holds are MP-signed. Between now and then, each player would contact the server to embargo their key that is needed for other players to unlock the cards-- and each player would add the embargoed results to the playbook. By 12:34:01.0001, all players would have all the keys needed to unlock their hand, less the key to actually do that-- so right at, or thereabouts, 12:34:01.0001, each player would ask the server for the key (for that specific time) and the server would give it to them (presuming that time from the server's POV has elapsed)-- and then they could decrypt the keys and use that to unlock the cards in their hand. The time has to have elapsed, however.

In the situation where the player needs to prove completion of some task by a certain future point in time: once the player is satisfied, he messages his results to the server referencing the future point in time (assuming that has not elapsed)-- he gets back an encrypted digest that he puts into the playbook. Once the time has elapsed, he can prove that there was no way to have encrypted his results before that time had elapsed.

Another feature: suppose a gaggle of players are enjoying a rousing game of poker with high stakes-- when one player up-and-leaves in frustration. Players could have agreed each that each round would "time out" at some specific time and embargoed their keys. When the player storms out, once the time out transpires, the players will be able to access the keys in order to resume play.














