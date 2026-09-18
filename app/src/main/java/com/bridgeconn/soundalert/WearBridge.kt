package com.bridgeconn.soundalert

import android.content.Context
import com.google.android.gms.wearable.Wearable

class WearBridge(private val context: Context) {
    fun send(kind: AlertKind) {
        if (kind == AlertKind.NONE) return
        try {
            Wearable.getNodeClient(context).connectedNodes
                .addOnSuccessListener { nodes ->
                    val payload = kind.name.toByteArray(Charsets.UTF_8)
                    nodes.forEach { node ->
                        Wearable.getMessageClient(context)
                            .sendMessage(node.id, PATH, payload)
                    }
                }
        } catch (_: Throwable) {
            // Watch support is opportunistic and never blocks phone alerts.
        }
    }

    companion object {
        const val PATH = "/soundalert/alert"
    }
}
