# Time Based Encrypter (TBE)

---

The Time Based Encrypter (TBE) serves as a third party for time based events in games played between participants. So long as participants trust the server, the TBE permits participants to embargo (escrow) objects to the future, and allow players to prove point-in-time accomplishments.

## Background (Why Is This Needed?)

Many games have one or both kinds of time components:
* ...don't start until some future point in time
    * _when the timer starts you may look at your hand_
* ...prove you completed activities by a future point in time 
    * _I found 25 words in 3 minutes_

Because neither Alice nor Bob's execution environments can be guaranteed, they have to trust a 3rd party for timing detail. Even supposing Alice can see Bob's wall clock, she cannot trust that he has no control of it (and vice versa). However, the two star-crossed players might could agree that the clock in the hallway-- that neither have direct control of-- would suffice. Of course, each believes the other could be in cahoots with Eve, the prison guard who does have control of the clock. Your milage may vary...

## Neat, so How's It Work (Narrative)

It has three features:
1. Players can post to it any object and a specific point in the future (and only the future); it'll encrypt that object.
1. Players can request the key do decrypt objects that the server has encrypted-- but only for past points in time.
1. It can provide anyone with a Signed Timestamp of the current time.

> "Point in time" always breaks down into a Epoch Time-- that is, a specific number. A _timestamp_ is such a number.

> "Now" is considered the **future**, for what it's worth. You can post an object to it with no timestamp; the server will use its current timestamp.

The first two points are the key points. 

Of the first, I can ask the server to encrypt something based on a future point in time (or the very present moment); it will. If anyone wants to decrypt it, they'll need the key from the server (for the specific future point in time I requested).

Of the second, the server only hands out keys for **past** points in time.

It does this by programmatically generating encryption keys based on the timestamp provided (assumed *now* if not). The server won't  encrypt something using a past timestamp. Moreover, when an object is to be encrypted, it adds a server-signed timestamp that anyone can verify. The server neither stores the key used for encryption (*), nor the data it was asked to encrypt; the server does not provide the player requesting encryption, at the time the request is made, the key used for encryption.

> *) Well, ok. So maybe it stores the key in memory so it needn't recompute it-- however, it doesn't need to. It still will not hand it out until the point has elapsed.

### Gameplay situations...

When the case is, "_don't start until some future point in time_", it works like this. Alice embargoes an object-- she gets back an encrypted object and publishes that to Bob. Bob cannot decrypt it; but, when the future point in time elapses, he can ask the server for the key-- which he'll get; then only at/after that point can he decrypt it. Neither he nor anyone can "start" before that point in time.

When the case is, "_.prove you completed activities by a future point in time_", it works like this. Alice completes her task and posts her results as an object to the server-- it encrypts it. She publishes that and everyone can verify the results were complete in time (well, if she was late she proves that): because it would be impossible to request something be published in the past.

### But This Relies on the Server as a Trusted Third Party-- Isn't That Lame?!

It is. Sorry. Bottom line is that Alice and Bob **must** agree to note **time** some way and, at least, the server is the same one messaging back and forth between them.

### What About Distributing "TBE" Across the Players?

Intriguing thought. It could work such that 51% would need to be in cahoots. Thus, TBE makes this work in even-partied games.

### Advanced Gameplay Example

> This example shows both situations; it will be helpful to understand how Mental Poker is used for randomization, the Playbook concept, and how it's passed around.

Lets envision a game of Boggle between Alice, Bob and Chris (of course). In boggle, the play-grid is a 4 x 4 set if 6 sided dice => 4 x 4 game pieces, of which each has a 1-in-6 face showing.

Using Mental Poker, we create 16 dice, and then for each we randomly select (also using MP) a showing-face. That makes for 96 x 3 = 288 MP passes but we get'er all done.

Alice, Bob and Chris each contact the server and embargo their keys (each has 96 keys) until "board reveal time" which is at precisely 7:00 pm in the future-- and all of this is transacted in the playbook.

Alice contacts the server at 6:59 pm and requests the key-- but she's denied!

Eve has taken interest-- but everything's encrypted and she can't get any more information than any other, nor can she tamper.

Right just after 7:00 pm, Alice, Bob and Chris each request **the** key for 7:00 pm-- and only one key, once. They can use that single key and decrypt **all** keys and thus decrypt all 16 pieces (and 6 faces per dice). Now the clock is ticking.

Each types in words and when they are satisfied, each one: compiles their list of words into a collection object, then posts that up to the server to be embargoed (at the present, "now", time), and publishes the result into the playbook.

They can prove that their list of words was completed on time (or not if they were late). No one can have started before 7:00 pm; neither Eve nor anyone cannot have tampered; and everyone's results are firmly established as complete by the given time they posted them to the server (if not, the server would issue a denial)... if they were late then they were late.

## Some Archi-Technical Details

To generate a timestamp based key, the server encrypts the timestamp with its Private Key such that the cipher-text produced is always the same. It uses that cipher-text (of the timestamp) to create a (symmetric) key which it uses to encrypt the data-to-embargo with. There are additional technical details about this (esp. truncation) for discussion later...

When needed, it simply encrypts a timestamp to produce key-generation-data to recreate the key. The server will cache keys in memory for performance reasons only. It does not hand out keys prematurely, and may cull in-memory data from time to time.

You would have to know the server's private key in order to reproduce key-generation-data used to create the key used to encrypt something. That's game-over for everyone if the server's private key is compromised.

AES-GCM is used so that input-vectors can help make it such that when items are encrypted they always appear different (same time-based key + same data = different cipher text results). The server decides the input-vectors.

When it creates and signs a timestamp, a "mode" property is set to indicate if the signed-timestamp was simply requested, or if it was generated as part of an embargo event. The signature portion of the signed-timestamp attests to that (it is incorporated into the digest that is signed) and cannot be tampered with.

The server stamps the embargoed objects with a timestamp (not signed) for convenience. However, when asked for a key, the server only provides exactly that-- there is no way to correlate an embargoed object (encrypted) with a server supplied key. This means that to post-verify an embargoed object (or decrypt it at all), a present request to the server for the key, for that specific time, is needed. Of course, it's astronomically unlikely that a "random" key could decrypt an embargoed object **and** produce within it a signed timestamp. 

Much is encoded into and out of Base64. Also bear in mind that Java works on unsigned 8-bit byte arrays-- unlike JavaScript. So JavaScript has to convert responses from the server appropriately (it's usually signed-bytes).


