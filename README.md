# AGS: Adversarial Gaming System

A web-based HTML/JavaScript client and J2EE server for playing games between completely untrusting participants.

---

#### An Analogy from WWII

Alice is a Russian POW and Bob is an American POW and neither trusts each other. They are incarcerated in a prison in Germany. They have agreed to settle their differences by playing a game of cards ([Crazy Eights](https://en.wikipedia.org/wiki/Crazy_Eights))-- however, each are locked in solitary confinement and must pass messages via the prison guard, Eve-- whom neither trusts. Alice and Bob have to agree to specific rules of the game; and besides winning, neither Alice nor Bob want to get caught cheating.

AGS explores this concept such that the players connect to the server only (primarily) to pass messages ([Comet-style](https://en.wikipedia.org/wiki/Comet_(programming)) in such a way that they cannot be tampered with by any party ([Web Crypto API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API)). AGS goes beyond simple [Mental Poker](https://en.wikipedia.org/wiki/Mental_poker) and incorporates a block chain play book, server generated time based encryption, and a gaming DSL. 

In theory, any conceivable game can be played this way.
