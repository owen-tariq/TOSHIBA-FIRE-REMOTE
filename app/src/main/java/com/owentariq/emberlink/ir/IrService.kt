package com.owentariq.emberlink.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import android.util.Log
import com.owentariq.emberlink.data.IrCommand
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Thin wrapper over ConsumerIrManager.
 *
 * No runtime permission is required to transmit IR, and none is declared. On the
 * POCO F3 (and Xiaomi handsets generally) the stock ConsumerIrManager is wired
 * straight to the blaster, so nothing vendor-specific is needed here.
 */
class IrService(context: Context) {

    private val manager: ConsumerIrManager? = try {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    } catch (t: Throwable) {
        Log.w(TAG, "ConsumerIrManager unavailable", t)
        null
    }

    /** True when this handset actually has an IR emitter. Everything is gated on it. */
    val hasEmitter: Boolean
        get() = try {
            manager?.hasIrEmitter() == true
        } catch (t: Throwable) {
            false
        }

    /** Human-readable list of carrier frequencies the hardware advertises. */
    val carrierSummary: String
        get() = try {
            manager?.carrierFrequencies
                ?.joinToString(", ") { "${it.minFrequency / 1000}-${it.maxFrequency / 1000} kHz" }
                ?: "unknown"
        } catch (t: Throwable) {
            "unknown"
        }

    val deviceLabel: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}"

    /**
     * ConsumerIrManager.transmit() is synchronous — it blocks the calling thread for the
     * entire length of the pulse train, around 47 ms per NEC frame. Doing that on the
     * main thread drops frames on every key press, so all sends go through one
     * single-threaded executor. Single-threaded on purpose: it keeps shots strictly
     * ordered, which matters when Code Lab is sweeping.
     */
    private val transmitter: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "emberlink-ir").apply { isDaemon = true }
    }

    /**
     * Queue one NECext frame for transmission.
     *
     * Returns false only when there is no IR hardware to talk to. A true result means
     * the frame was queued, not that the TV heard it — infrared has no ack.
     */
    fun send(address: Int, subAddress: Int, command: Int): Boolean {
        val mgr = manager ?: return false
        val pattern = NecEncoder.frame(address, subAddress, command)
        return try {
            transmitter.execute {
                try {
                    mgr.transmit(NecEncoder.CARRIER_HZ, pattern)
                } catch (t: Throwable) {
                    Log.w(TAG, "transmit failed", t)
                }
            }
            true
        } catch (t: Throwable) {
            Log.w(TAG, "could not queue transmit", t)
            false
        }
    }

    fun send(command: IrCommand): Boolean =
        send(command.address, command.subAddress, command.command)

    private companion object {
        const val TAG = "Emberlink"
    }
}
