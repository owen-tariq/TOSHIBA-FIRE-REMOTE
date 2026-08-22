package com.owentariq.emberlink.ir

/**
 * Pulse encoders for the IR protocols a TV panel might actually be listening for.
 *
 * Emberlink shipped assuming NECext 02 7D because three independent captures agreed on
 * it. When a panel does not respond there is no error to read — infrared is one-way —
 * so the only way forward is to widen the search. These are the alternatives worth
 * trying before concluding the hardware is at fault.
 *
 * Every encoder returns durations in microseconds, alternating ON/OFF, starting with
 * ON, which is the contract ConsumerIrManager expects.
 */
enum class IrProtocol(val label: String, val carrierHz: Int) {
    NEC("NEC", 38_000),
    SAMSUNG("Samsung", 38_000),
    RC5("Philips RC5", 36_000),
    SIRC12("Sony SIRC 12", 40_000),
    SIRC15("Sony SIRC 15", 40_000),
    ;

    companion object {
        fun fromName(name: String?): IrProtocol =
            entries.firstOrNull { it.name == name } ?: NEC
    }
}

object Protocols {

    fun encode(protocol: IrProtocol, address: Int, subAddress: Int, command: Int): IntArray =
        when (protocol) {
            IrProtocol.NEC -> NecEncoder.frame(address, subAddress, command)
            IrProtocol.SAMSUNG -> samsung(address, command)
            IrProtocol.RC5 -> rc5(address, command)
            IrProtocol.SIRC12 -> sirc(address, command, bits = 12)
            IrProtocol.SIRC15 -> sirc(address, command, bits = 15)
        }

    /**
     * Samsung 32-bit. Same bit timing as NEC but a symmetric 4500/4500 header and the
     * address byte repeated instead of inverted.
     */
    private fun samsung(address: Int, command: Int): IntArray {
        val bytes = intArrayOf(
            address and 0xFF,
            address and 0xFF,
            command and 0xFF,
            command.inv() and 0xFF,
        )
        val out = ArrayList<Int>(68)
        out.add(4_500); out.add(4_500)
        for (b in bytes) {
            for (i in 0 until 8) {
                out.add(560)
                out.add(if ((b shr i) and 1 == 1) 1_690 else 560)
            }
        }
        out.add(560)
        out.add(20_000)
        return out.toIntArray()
    }

    /**
     * Philips RC5, 14 bits Manchester encoded at 889 us per half-bit.
     *
     * Manchester means every bit is a level transition rather than a pulse of a given
     * width, so the bit stream is built as a list of half-bit levels and then collapsed
     * into alternating ON/OFF runs.
     */
    private fun rc5(address: Int, command: Int): IntArray {
        val half = 889
        val bits = ArrayList<Int>(14)
        bits.add(1)                                   // start
        bits.add(1)                                   // field (command < 64)
        bits.add(0)                                   // toggle
        for (i in 4 downTo 0) bits.add((address shr i) and 1)
        for (i in 5 downTo 0) bits.add((command shr i) and 1)

        // RC5 bit 1 = low then high; bit 0 = high then low.
        val levels = ArrayList<Int>(28)
        for (b in bits) {
            if (b == 1) { levels.add(0); levels.add(1) } else { levels.add(1); levels.add(0) }
        }
        return levelsToPattern(levels, half)
    }

    /**
     * Sony SIRC. Header 2400 us, bit 1 = 1200 us mark, bit 0 = 600 us mark,
     * every mark followed by a 600 us space. Command first, then address, LSB first.
     */
    private fun sirc(address: Int, command: Int, bits: Int): IntArray {
        val addressBits = bits - 7
        val out = ArrayList<Int>(2 + bits * 2)
        out.add(2_400); out.add(600)
        for (i in 0 until 7) {
            out.add(if ((command shr i) and 1 == 1) 1_200 else 600)
            out.add(600)
        }
        for (i in 0 until addressBits) {
            out.add(if ((address shr i) and 1 == 1) 1_200 else 600)
            out.add(600)
        }
        // Trailing gap so consecutive frames read as separate presses.
        out[out.size - 1] = 20_000
        return out.toIntArray()
    }

    /**
     * Collapse a sequence of half-bit levels into alternating ON/OFF durations.
     *
     * The pattern must begin with an ON period, so a leading low run is dropped rather
     * than emitted as a zero-length mark, which some HALs reject outright.
     */
    private fun levelsToPattern(levels: List<Int>, unit: Int): IntArray {
        val runs = ArrayList<Pair<Int, Int>>()
        var current = levels.first()
        var count = 0
        for (l in levels) {
            if (l == current) count++ else { runs.add(current to count); current = l; count = 1 }
        }
        runs.add(current to count)

        val out = ArrayList<Int>(runs.size + 2)
        var expectOn = true
        for ((level, n) in runs) {
            val on = level == 1
            if (expectOn && !on) continue          // drop a leading space
            if (on != expectOn) {
                // Shouldn't happen with well-formed Manchester, but never emit two
                // same-polarity runs in a row — and never index into an empty list.
                if (out.isEmpty()) continue
                out[out.size - 1] = out.last() + n * unit
                continue
            }
            out.add(n * unit)
            expectOn = !expectOn
        }
        if (out.size % 2 == 1) out.add(20_000) else out[out.size - 1] = 20_000
        return out.toIntArray()
    }
}
