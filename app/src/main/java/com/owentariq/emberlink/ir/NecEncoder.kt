package com.owentariq.emberlink.ir

/**
 * NEC / NECext pulse-train encoder.
 *
 * Fire TV Edition panels (Insignia NS-*DF*, Toshiba Fire TV) and Amazon Omni / 4-Series
 * sets all listen on NECext address 02 7D — device 2, subdevice 125.
 *
 * Frame layout:
 *   9000 us mark, 4500 us space          (header)
 *   32 data bits, LSB first, in the byte order: address, subAddress, command, ~command
 *   560 us mark                          (trailer)
 *
 * Bit encoding:
 *   0 -> 560 us mark + 560 us space
 *   1 -> 560 us mark + 1690 us space
 *
 * ConsumerIrManager wants alternating on/off durations in microseconds, starting with on.
 */
object NecEncoder {

    const val CARRIER_HZ = 38_000

    private const val HEADER_MARK = 9_000
    private const val HEADER_SPACE = 4_500
    private const val BIT_MARK = 560
    private const val ONE_SPACE = 1_690
    private const val ZERO_SPACE = 560

    /**
     * Idle gap appended after the trailer so consecutive frames are seen as separate
     * key presses rather than one smeared burst.
     *
     * transmit() blocks for the full length of the pattern, so this value is also the
     * floor on how fast a held key can repeat. 20 ms is comfortably above the ~10 ms a
     * receiver needs to resynchronise, while keeping a whole frame under 50 ms.
     */
    private const val TRAIL_SPACE = 20_000

    /** A NEC "repeat" frame, sent while a key is held down. */
    private const val REPEAT_MARK = 9_000
    private const val REPEAT_SPACE = 2_250

    /**
     * Build a single complete NECext frame.
     *
     * @param address     first byte (0x02 for Fire TV Edition)
     * @param subAddress  second byte (0x7D for Fire TV Edition). For plain NEC1 with no
     *                    subdevice this is the bitwise inverse of [address].
     * @param command     the command byte / OBC
     */
    fun frame(address: Int, subAddress: Int, command: Int): IntArray {
        val bytes = intArrayOf(
            address and 0xFF,
            subAddress and 0xFF,
            command and 0xFF,
            command.inv() and 0xFF,
        )

        // header (2) + 32 bits (64) + trailer mark (1) + trailing gap (1)
        val out = IntArray(2 + 64 + 2)
        var i = 0
        out[i++] = HEADER_MARK
        out[i++] = HEADER_SPACE

        for (byte in bytes) {
            for (bit in 0 until 8) {
                out[i++] = BIT_MARK
                out[i++] = if ((byte shr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE
            }
        }

        out[i++] = BIT_MARK
        out[i] = TRAIL_SPACE
        return out
    }

    /** The short NEC repeat burst used when a button is held. */
    fun repeatFrame(): IntArray = intArrayOf(REPEAT_MARK, REPEAT_SPACE, BIT_MARK, TRAIL_SPACE)

    /**
     * For plain NEC1 with no subdevice, the second byte is the inverse of the first.
     * Handy when sweeping alternate device addresses in Code Lab.
     */
    fun inverseSubAddress(address: Int): Int = address.inv() and 0xFF
}
