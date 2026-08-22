package com.owentariq.emberlink.data

import android.content.Context
import android.content.SharedPreferences
import com.owentariq.emberlink.ir.IrProtocol

/**
 * Tunables that exist because infrared is a one-way medium.
 *
 * The app never learns whether the TV heard anything, so when nothing happens there
 * is no error to read — only variables to change. These are those variables, exposed
 * to the user rather than buried as constants.
 */
class Settings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("emberlink_settings", Context.MODE_PRIVATE)

    /**
     * How many times each frame is sent per button press.
     *
     * A single NEC frame is technically valid, but a good number of receivers treat one
     * frame as noise and only act once they have seen the code repeat. Two is the safe
     * default; raise it if the TV responds intermittently.
     */
    var framesPerPress: Int
        get() = prefs.getInt(KEY_FRAMES, 2).coerceIn(1, 5)
        set(v) = prefs.edit().putInt(KEY_FRAMES, v.coerceIn(1, 5)).apply()

    /** Gap between repeated frames of the same press, in milliseconds. */
    var frameGapMs: Int
        get() = prefs.getInt(KEY_GAP, 40).coerceIn(0, 200)
        set(v) = prefs.edit().putInt(KEY_GAP, v.coerceIn(0, 200)).apply()

    /**
     * Global address override.
     *
     * Every built-in code assumes NECext 02 7D. If this particular panel answers on a
     * different device address, nothing works and there is no feedback to say why —
     * so the address is switchable without rebuilding.
     */
    var addressOverride: Int
        get() = prefs.getInt(KEY_ADDR, IrCommand.FIRE_TV_ADDRESS)
        set(v) = prefs.edit().putInt(KEY_ADDR, v and 0xFF).apply()

    var subAddressOverride: Int
        get() = prefs.getInt(KEY_SUB, IrCommand.FIRE_TV_SUBADDRESS)
        set(v) = prefs.edit().putInt(KEY_SUB, v and 0xFF).apply()

    /** Carrier frequency in Hz. Standard NEC is 38 kHz; a few panels prefer 38.4 or 40. */
    var carrierHz: Int
        get() = prefs.getInt(KEY_CARRIER, 38_000)
        set(v) = prefs.edit().putInt(KEY_CARRIER, v).apply()

    /**
     * Protocol used for every normal button press. Set automatically when Auto-Hunt
     * finds a code that works, so the user never has to know what NEC means.
     */
    var protocol: IrProtocol
        get() = IrProtocol.fromName(prefs.getString(KEY_PROTOCOL, IrProtocol.NEC.name))
        set(v) = prefs.edit().putString(KEY_PROTOCOL, v.name).apply()

    /** True once Auto-Hunt has confirmed a working code, so the UI can stop nagging. */
    var hasConfirmedWorkingCode: Boolean
        get() = prefs.getBoolean(KEY_CONFIRMED, false)
        set(v) = prefs.edit().putBoolean(KEY_CONFIRMED, v).apply()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    /** True when the address is still the documented Fire TV Edition one. */
    val isDefaultAddress: Boolean
        get() = addressOverride == IrCommand.FIRE_TV_ADDRESS &&
            subAddressOverride == IrCommand.FIRE_TV_SUBADDRESS

    companion object {
        private const val KEY_FRAMES = "frames_per_press"
        private const val KEY_GAP = "frame_gap_ms"
        private const val KEY_ADDR = "address"
        private const val KEY_SUB = "sub_address"
        private const val KEY_CARRIER = "carrier_hz"
        private const val KEY_PROTOCOL = "protocol"
        private const val KEY_CONFIRMED = "confirmed_working"

        /** Address presets worth trying when nothing responds. */
        val ADDRESS_PRESETS = listOf(
            AddressPreset("Fire TV Edition", 0x02, 0x7D, "The documented default. Try this first."),
            AddressPreset("NEC device 64", 0x40, 0xBF, "Some model years put panel functions here."),
            AddressPreset("Toshiba TV", 0x02, 0xFD, "Classic Toshiba panel address."),
            AddressPreset("Insignia TV", 0x86, 0x05, "Non-Fire Insignia panels."),
        )
    }
}

data class AddressPreset(
    val label: String,
    val address: Int,
    val subAddress: Int,
    val note: String,
)
