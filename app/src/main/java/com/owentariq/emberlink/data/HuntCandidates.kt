package com.owentariq.emberlink.data

import com.owentariq.emberlink.ir.IrProtocol

/**
 * One thing to try while hunting for a TV that responds.
 *
 * A candidate is a complete guess: protocol, address and command together. Nothing
 * about it is assumed from the others, because the whole point of the hunt is that we
 * no longer trust any single assumption.
 */
data class HuntCandidate(
    val label: String,
    val protocol: IrProtocol,
    val address: Int,
    val subAddress: Int,
    val command: Int,
) {
    val addressHex: String get() = "%02X %02X".format(address, subAddress)
    val commandHex: String get() = "%02X".format(command)
}

/**
 * The search order for "my TV does nothing".
 *
 * Power is the target throughout: it is the only command whose effect is unmistakable
 * from across a room, and it works whether or not the TV is currently on.
 *
 * Ordered cheapest-first — the handful of documented power codes for panels in this
 * family come before any brute force, so the common case resolves in seconds rather
 * than minutes.
 */
object HuntCandidates {

    /** Documented power codes for TVs this remote plausibly faces. ~20 shots. */
    val KNOWN_POWER: List<HuntCandidate> = listOf(
        HuntCandidate("Fire TV Edition", IrProtocol.NEC, 0x02, 0x7D, 0x46),
        HuntCandidate("Fire TV Edition alt", IrProtocol.NEC, 0x02, 0x7D, 0x45),
        HuntCandidate("Toshiba (dev 64)", IrProtocol.NEC, 0x40, 0xBF, 0x12),
        HuntCandidate("Toshiba classic", IrProtocol.NEC, 0x02, 0xFD, 0x12),
        HuntCandidate("Toshiba alt", IrProtocol.NEC, 0x40, 0xBF, 0x48),
        HuntCandidate("Insignia", IrProtocol.NEC, 0x86, 0x05, 0x0F),
        HuntCandidate("Insignia alt", IrProtocol.NEC, 0x04, 0xFB, 0x08),
        HuntCandidate("Amazon Omni", IrProtocol.NEC, 0x02, 0x7D, 0xC5),
        HuntCandidate("LG", IrProtocol.NEC, 0x04, 0xFB, 0x08),
        HuntCandidate("Samsung", IrProtocol.SAMSUNG, 0x07, 0x07, 0x02),
        HuntCandidate("Philips RC5", IrProtocol.RC5, 0x00, 0x00, 0x0C),
        HuntCandidate("Sony 12-bit", IrProtocol.SIRC12, 0x01, 0x00, 0x15),
        HuntCandidate("Sony 15-bit", IrProtocol.SIRC15, 0x01, 0x00, 0x15),
        HuntCandidate("Vizio", IrProtocol.NEC, 0x20, 0xDF, 0x10),
        HuntCandidate("Hisense", IrProtocol.NEC, 0x00, 0xBF, 0x1B),
        HuntCandidate("TCL", IrProtocol.NEC, 0x08, 0xF7, 0x0C),
        HuntCandidate("Panasonic-ish", IrProtocol.NEC, 0x40, 0x04, 0x3D),
        HuntCandidate("Sharp-ish", IrProtocol.NEC, 0x02, 0xFD, 0x48),
    )

    /**
     * Exhaustive NEC sweep on one address. 256 shots, roughly two minutes at a pace
     * slow enough to notice the TV reacting.
     *
     * These are the addresses worth exhausting, in order of how likely they are to be
     * right for a Fire TV Edition panel.
     */
    val SWEEP_ADDRESSES: List<Pair<String, Pair<Int, Int>>> = listOf(
        "Fire TV Edition 02 7D" to (0x02 to 0x7D),
        "NEC device 64  40 BF" to (0x40 to 0xBF),
        "Toshiba 02 FD" to (0x02 to 0xFD),
        "Insignia 86 05" to (0x86 to 0x05),
    )

    fun sweep(address: Int, subAddress: Int, label: String): List<HuntCandidate> =
        (0..255).map { cmd ->
            HuntCandidate(label, IrProtocol.NEC, address, subAddress, cmd)
        }

    /** Stage 1 followed by a full sweep of the most likely address. */
    fun defaultPlan(): List<HuntCandidate> =
        KNOWN_POWER + SWEEP_ADDRESSES.flatMap { (label, addr) ->
            sweep(addr.first, addr.second, label)
        }
}
