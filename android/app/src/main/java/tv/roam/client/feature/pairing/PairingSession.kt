package tv.roam.client.feature.pairing

import java.security.SecureRandom

data class PairingSession(
    val id: String,
    val code: String,
    val key: ByteArray,
    val expiresAtMillis: Long,
) {
    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis >= expiresAtMillis
}

object PairingSessionFactory {
    const val DEFAULT_TTL_MILLIS = 120_000L
    private const val ID_BYTES = 16
    private val random = SecureRandom()

    fun create(
        nowMillis: Long = System.currentTimeMillis(),
        ttlMillis: Long = DEFAULT_TTL_MILLIS,
    ): PairingSession {
        val idBytes = ByteArray(ID_BYTES).also(random::nextBytes)
        return PairingSession(
            id = PairingCrypto.base64Url(idBytes),
            code = (random.nextInt(1_000_000)).toString().padStart(6, '0'),
            key = PairingCrypto.newKey(),
            expiresAtMillis = nowMillis + ttlMillis,
        )
    }
}
