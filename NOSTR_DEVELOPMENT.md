# Nostr Development Guide

This document outlines the development practices and reference materials for implementing Nostr functionality in CloudStream.

## Primary Reference Implementation

**All Nostr-related features should reference the [Amethyst Android](https://github.com/vitorpamplona/amethyst.git) codebase.**

Amethyst is the canonical Android Nostr client and provides battle-tested implementations of:
- Relay communication and WebSocket handling
- Event verification and cryptographic operations
- Event construction and signing
- NIP (Nostr Implementation Possibilities) standards
- Filter construction and optimization

### What to Reference from Amethyst

✅ **DO use Amethyst as reference for:**
- How to send REQ/EVENT/CLOSE messages to relays
- How to construct and parse Nostr filters
- How to verify event signatures (Schnorr verification)
- How to construct and sign events properly
- How to parse and handle different event kinds (Kind 0, 1, 30311, 1311, etc.)
- WebSocket connection management and retry logic
- Relay pool implementation patterns
- Cryptographic key management (secp256k1)
- NIP-01, NIP-53 (live streaming), and other NIP implementations

❌ **DO NOT implement:**
- **Outbox Model** - We skip the outbox model (fetching relays from user profiles and using those hints to request notes)
- User relay list discovery and management
- Dynamic relay selection based on user metadata

### Example: How to Use Amethyst as Reference

When implementing a new Nostr feature:

1. **Find the relevant code in Amethyst:**
   - Events: `amethyst/app/src/main/java/com/vitorpamplona/amethyst/model/`
   - Relay communication: `amethyst/quartz/src/main/java/com/vitorpamplona/quartz/`
   - Crypto: `amethyst/quartz/src/main/java/com/vitorpamplona/quartz/crypto/`

2. **Understand the implementation:**
   - Study how Amethyst handles the feature
   - Note any NIP compliance requirements
   - Check for edge cases and error handling

3. **Adapt to CloudStream architecture:**
   - Convert Compose UI to XML layouts
   - Use CloudStream's DataStore instead of Room database
   - Follow CloudStream's MVVM patterns
   - Use in-memory caching where appropriate

## CloudStream-Specific Integration Notes

### Architecture Patterns

```
nostr/
├── crypto/          # secp256k1 key management and signing
├── models/          # Nostr event data classes (Parcelable)
├── network/         # WebSocket relay pool and connections
├── repositories/    # Data layer with in-memory caching
└── ui/              # XML fragments and ViewModels
```

### Key Technologies

- **Cryptography:** `secp256k1-kmp` (same as Amethyst)
- **WebSockets:** OkHttp WebSocketListener
- **JSON:** Jackson with null-field exclusion
- **Reactive:** Kotlin Flows (StateFlow/SharedFlow)
- **Storage:** CloudStream DataStore (NOT Room)
- **UI:** XML layouts + Fragments (NOT Compose)

### Current Implementation Status

#### ✅ Implemented
- [x] Relay pool with multi-relay connections
- [x] WebSocket message parsing (EVENT, EOSE, NOTICE, OK)
- [x] Kind 30311 (live stream) event parsing
- [x] Stream discovery with filters (kinds, since, limit)
- [x] Keypair generation and storage
- [x] Event signature verification (basic)
- [x] Stream playback integration
- [x] Settings UI for relay management

#### 🚧 Partially Implemented
- [ ] Kind 1311 (live chat) - infrastructure ready, not integrated
- [ ] Kind 0 (profile metadata) - repository exists, not used
- [ ] Event signing for publishing - EventSigner exists, not used

#### ❌ Not Implemented
- [ ] Publishing events (commenting, reactions)
- [ ] Profile avatars and metadata display
- [ ] Relay health monitoring
- [ ] Event caching persistence
- [ ] NIP-05 verification
- [ ] NIP-07 browser extension support
- [ ] Zaps (NIP-57)

### Critical Implementation Details

#### 1. Event Filters Must Exclude Extra Fields

**Problem:** Kotlin's `@Parcelize` adds a `stability` field that breaks Nostr protocol.

**Solution:**
```kotlin
@Parcelize
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true, value = ["stability", "dtags"])
data class NostrFilter(...)
```

**Reference:** Check Amethyst's filter implementation to ensure compliance.

#### 2. Relay Connection Timing

**Problem:** Subscriptions sent before WebSocket connections complete are lost.

**Solution:**
```kotlin
suspend fun connectToRelays(relayUrls: List<String>) {
    // Start connections
    relayUrls.forEach { url ->
        val connection = RelayConnection(...)
        connections[url] = connection
        connection.connect()
    }

    // Wait for at least one relay to connect
    withTimeoutOrNull(5000) {
        while (connections.values.none { it.connectionState.value is CONNECTED }) {
            delay(100)
        }
    }
}
```

**Reference:** Amethyst's relay pool likely has better connection management patterns.

#### 3. Audio in Video Streams

**Problem:** Using deprecated `ExtractorLink()` constructor breaks audio.

**Solution:** Always use `newExtractorLink()`:
```kotlin
val link = newExtractorLink(
    source = "Nostr",
    name = streamTitle,
    url = streamUrl,
    type = INFER_TYPE  // Auto-detect M3U8/DASH/VIDEO
) {
    this.referer = ""
    this.quality = Qualities.Unknown.value
}
```

#### 4. In-Memory Caching Pattern

CloudStream uses in-memory caching (not Room database):

```kotlin
private val cache = ConcurrentHashMap<String, CachedStream>()

data class CachedStream(val stream: Stream, val timestamp: Long)

// Evict stale entries (> 30 seconds old)
val fresh = cache.values
    .filter { now - it.timestamp < STALE_THRESHOLD }
    .map { it.stream }
```

**Reference:** Check if Amethyst has optimizations for cache management.

## Default Relay Configuration

Current relay list (optimized for live streaming):
```kotlin
val DEFAULT_RELAYS = listOf(
    "wss://relay.snort.social",
    "wss://relay.damus.io",
    "wss://nos.lol",
    "wss://relay.nostr.band",  // Note: SSL cert issues
    "wss://nostr.wine",        // Note: SSL cert issues
    "wss://relay.primal.net",
    "wss://purplepag.es"
)
```

**Reference:** Check Amethyst's default relay list for potential improvements.

## Testing Checklist

When implementing new Nostr features:

- [ ] Test with multiple relays (at least 3)
- [ ] Handle relay connection failures gracefully
- [ ] Verify event signatures before processing
- [ ] Test with malformed Nostr events
- [ ] Check for memory leaks with long-running connections
- [ ] Validate JSON serialization (no extra fields)
- [ ] Test WebSocket reconnection logic
- [ ] Verify filter parameters match NIP-01 spec

## Useful Amethyst Code References

### Event Verification
`amethyst/quartz/src/main/java/com/vitorpamplona/quartz/crypto/CryptoUtils.kt`
- Schnorr signature verification
- Event ID calculation (SHA-256)

### Relay Communication
`amethyst/quartz/src/main/java/com/vitorpamplona/quartz/events/`
- Event parsing and construction
- Filter builders

### WebSocket Handling
`amethyst/app/src/main/java/com/vitorpamplona/amethyst/service/`
- Connection pooling
- Message routing
- Subscription management

## Resources

- [Nostr Protocol (NIPs)](https://github.com/nostr-protocol/nips)
- [NIP-01: Basic Protocol](https://github.com/nostr-protocol/nips/blob/master/01.md)
- [NIP-53: Live Activities](https://github.com/nostr-protocol/nips/blob/master/53.md)
- [Amethyst Source Code](https://github.com/vitorpamplona/amethyst)
- [secp256k1-kmp Library](https://github.com/ACINQ/secp256k1-kmp)

## Questions or Issues?

When encountering Nostr-specific problems:
1. First check how Amethyst handles it
2. Consult the relevant NIP specification
3. Test against multiple relays
4. Verify event format with online Nostr tools

---

**Last Updated:** 2025-01-01
**CloudStream Version:** Custom Nostr Fork
**Amethyst Reference Version:** Latest main branch
