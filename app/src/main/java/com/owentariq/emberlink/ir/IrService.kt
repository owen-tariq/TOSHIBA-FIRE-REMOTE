package com.owentariq.emberlink.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import android.util.Log
import com.owentariq.emberlink.data.IrCommand
import com.owentariq.emberlink.data.Settings
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thin wrapper over ConsumerIrManager.
 *
 * No runtime permission is required to transmit IR, and none is declared. On the
 * POCO F3 (and Xiaomi handsets generally) the stock ConsumerIrManager is wired
 * straight to the blaster, so nothing vendor-specific is needed here.
 *
 * Because IR is one-way, a successful transmit proves only that the phone emitted
 * light — never that the TV understood it. Everything this class reports should be
 * read in that light.
 */
class IrService(context: Context, private val settings: Settings) {

    private val manager: ConsumerIrManager? = try {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    } catch (t: Throwable) {
        Log.w(TAG, "ConsumerIrManager unavailable", t)
        null
    }

    /** Last transmit failure, surfaced in Diagnostics so failures aren't silent. */
    @Volatile
    var lastError: String? = null
        private set

    /** Total frames actually handed to the hardware, for the Diagnostics counter. */
    val framesSent = AtomicInteger(0)

    val hasEmitter: Boolean
        get() = try {
            manager?.hasIrEmitter() == true
        } catch (t: Throwable) {
            false
        }

    val managerPresent: Boolean get() = manager != null

    /** Carrier ranges the hardware advertises, as human-readable text. */
    val carrierSummary: String
        get() = try {
            manager?.carrierFrequencies
                ?.joinToString(", ") { "${it.minFrequency}-${it.maxFrequency} Hz" }
                ?.ifBlank { "none reported" }
                ?: "unavailable"
        } catch (t: Throwable) {
            "query failed: ${t.javaClass.simpleName}"
        }

    /** True when the configured carrier falls inside a range the hardware claims. */
    val carrierSupported: Boolean
        get() = try {
            val hz = settings.carrierHz
            manager?.carrierFrequencies?.any { hz >= it.minFrequency && hz <= it.maxFrequency }
                ?: false
        } catch (t: Throwable) {
            false
        }

    val deviceLabel: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

    /**
     * transmit() blocks the calling thread for the length of the pulse train, so every
     * send goes through one background thread. Single-threaded on purpose: it keeps
     * frames strictly ordered, which matters for Code Lab sweeps and for keyboard
     * routing where order is the whole point.
     */
    private val transmitter: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "emberlink-ir").apply { isDaemon = true }
    }

    /**
     * Send one command, honouring the configured address override and repeat count.
     *
     * Returns false only when there is no IR hardware. True means the frames were
     * queued — infrared has no acknowledgement, so it can never mean the TV heard it.
     */
    fun send(address: Int, subAddress: Int, command: Int, applyOverride: Boolean = true): Boolean {
        val mgr = manager ?: run {
            lastError = "No ConsumerIrManager on this device"
            return false
        }

        val addr = if (applyOverride) settings.addressOverride else address
        val sub = if (applyOverride) settings.subAddressOverride else subAddress
        val proto = settings.protocol
        val pattern = Protocols.encode(proto, addr, sub, command)
        // The protocol dictates the carrier; the manual override only applies to NEC,
        // where 38 kHz is the norm but a few panels want something slightly different.
        val carrier = if (proto == IrProtocol.NEC) settings.carrierHz else proto.carrierHz
        val repeats = settings.framesPerPress
        val gap = settings.frameGapMs.toLong()

        transmitter.execute {
            repeat(repeats) { i ->
                try {
                    mgr.transmit(carrier, pattern)
                    framesSent.incrementAndGet()
                    lastError = null
                } catch (t: Throwable) {
                    lastError = "${t.javaClass.simpleName}: ${t.message ?: "no detail"}"
                    Log.w(TAG, "transmit failed", t)
                    return@execute
                }
                if (i < repeats - 1 && gap > 0) {
                    try {
                        Thread.sleep(gap)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return@execute
                    }
                }
            }
        }
        return true
    }

    fun send(command: IrCommand): Boolean =
        send(command.address, command.subAddress, command.command)

    /**
     * Fire one hunt candidate exactly as specified — its own protocol, address and
     * carrier, ignoring every saved setting. Auto-Hunt depends on this: the entire
     * point is to test combinations the current settings would otherwise override.
     */
    fun sendCandidate(
        protocol: IrProtocol,
        address: Int,
        subAddress: Int,
        command: Int,
    ): Boolean {
        val mgr = manager ?: run {
            lastError = "No ConsumerIrManager on this device"
            return false
        }
        val pattern = Protocols.encode(protocol, address, subAddress, command)
        transmitter.execute {
            try {
                mgr.transmit(protocol.carrierHz, pattern)
                framesSent.incrementAndGet()
                lastError = null
            } catch (t: Throwable) {
                lastError = "${t.javaClass.simpleName}: ${t.message ?: "no detail"}"
                Log.w(TAG, "candidate transmit failed", t)
            }
        }
        return true
    }

    /** Raw send that ignores the address override — used by Code Lab sweeps. */
    fun sendRaw(address: Int, subAddress: Int, command: Int): Boolean =
        send(address, subAddress, command, applyOverride = false)

    fun clearError() {
        lastError = null
    }

    private companion object {
        const val TAG = "Emberlink"
    }
}
