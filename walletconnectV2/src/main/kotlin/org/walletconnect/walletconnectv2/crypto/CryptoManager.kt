package org.walletconnect.walletconnectv2.crypto

import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.data.EncryptionPayload
import org.walletconnect.walletconnectv2.crypto.data.PublicKey

interface CryptoManager {

    fun hasKeys(tag: String): Boolean

    fun generateKeyPair(): PublicKey

    fun generateSettledTopic(self: PublicKey, peer: PublicKey, overrideTopic: String? = null): Topic

    fun generateSharedKey(self: PublicKey, peer: PublicKey): String
}