package com.owentariq.emberlink.data

/**
 * One IR command.
 *
 * Address is carried per-command rather than globally, because a Fire TV Edition set
 * answers Fire OS keys on NECext 02 7D while some panel-level functions on certain
 * model years live on a different NEC device. Never store a bare command byte.
 */
data class IrCommand(
    val id: String,
    val label: String,
    val command: Int,
    val address: Int = FIRE_TV_ADDRESS,
    val subAddress: Int = FIRE_TV_SUBADDRESS,
) {
    val hex: String get() = "%02X".format(command)
    val addressHex: String get() = "%02X %02X".format(address, subAddress)

    companion object {
        const val FIRE_TV_ADDRESS = 0x02
        const val FIRE_TV_SUBADDRESS = 0x7D
    }
}

/**
 * The confirmed Fire TV Edition / Omni command set.
 *
 * Cross-checked across three independent Flipper-IRDB captures which agree byte for byte:
 *   TVs/Amazon/FireTV_Omni_Series_4K.ir
 *   TVs/Toshiba/toshiba_firetv_v2.ir
 *   TVs/Insignia/Insignia_NS_RCFNA_21.ir
 *
 * Anything not in this table is discoverable with Code Lab: NEC device 2.125 has only
 * 256 possible command bytes, so a full sweep takes a couple of minutes.
 */
object Commands {

    // --- power -------------------------------------------------------------
    val POWER = IrCommand("power", "Power", 0x46)

    // --- navigation --------------------------------------------------------
    val UP = IrCommand("up", "Up", 0x48)
    val DOWN = IrCommand("down", "Down", 0x4D)
    val LEFT = IrCommand("left", "Left", 0x4E)
    val RIGHT = IrCommand("right", "Right", 0x49)
    val SELECT = IrCommand("select", "OK", 0x4A)

    val BACK = IrCommand("back", "Back", 0x0D)
    val HOME = IrCommand("home", "Home", 0x9F)
    val MENU = IrCommand("menu", "Menu", 0x45)

    // --- transport ---------------------------------------------------------
    val REWIND = IrCommand("rewind", "Rewind", 0x16)
    val PLAY_PAUSE = IrCommand("play_pause", "Play / Pause", 0x5B)
    val FAST_FORWARD = IrCommand("fast_forward", "Fast forward", 0x17)

    // --- volume ------------------------------------------------------------
    val VOL_UP = IrCommand("vol_up", "Volume up", 0x0C)
    val VOL_DOWN = IrCommand("vol_down", "Volume down", 0x19)
    val MUTE = IrCommand("mute", "Mute", 0x4C)

    // --- tuner -------------------------------------------------------------
    val CHANNEL_UP = IrCommand("channel_up", "Channel up", 0x0F)
    val CHANNEL_DOWN = IrCommand("channel_down", "Channel down", 0x5A)
    val GUIDE = IrCommand("guide", "Guide / Live TV", 0x14)

    // --- apps --------------------------------------------------------------
    val NETFLIX = IrCommand("netflix", "Netflix", 0x5F)
    val PRIME_VIDEO = IrCommand("prime_video", "Prime Video", 0xA1)
    val DISNEY_PLUS = IrCommand("disney_plus", "Disney+", 0xA2)
    val HULU = IrCommand("hulu", "Hulu", 0xA5)
    val FREEVEE = IrCommand("freevee", "Freevee", 0xD2)

    // --- system ------------------------------------------------------------
    val SETTINGS = IrCommand("settings", "Settings", 0x96)
    val RECENT_APPS = IrCommand("recent_apps", "Recent apps", 0xB1)
    val ALEXA = IrCommand("alexa", "Alexa", 0xA0)
    val MAGNIFIER = IrCommand("magnifier", "Magnifier / Search", 0xC8)
    val VOICE_VIEW = IrCommand("voice_view", "VoiceView", 0xC0)
    val BLUETOOTH = IrCommand("bluetooth", "Bluetooth menu", 0xA6)
    val RESOLUTION = IrCommand("resolution", "Resolution", 0xCE)
    val NETWORKING = IrCommand("networking", "Network menu", 0xCF)

    // --- destructive: never place these on the main pad --------------------
    val REBOOT = IrCommand("reboot", "Reboot", 0xCA)
    val FACTORY_RESET = IrCommand("factory_reset", "Factory reset", 0xC9)

    /** Codes that do something irreversible or disruptive the instant they land. */
    val DANGEROUS = setOf(REBOOT.id, FACTORY_RESET.id)

    /** Everything confirmed, for the reference table and the export. */
    val ALL: List<IrCommand> = listOf(
        POWER,
        UP, DOWN, LEFT, RIGHT, SELECT,
        BACK, HOME, MENU,
        REWIND, PLAY_PAUSE, FAST_FORWARD,
        VOL_UP, VOL_DOWN, MUTE,
        CHANNEL_UP, CHANNEL_DOWN, GUIDE,
        NETFLIX, PRIME_VIDEO, DISNEY_PLUS, HULU, FREEVEE,
        SETTINGS, RECENT_APPS, ALEXA, MAGNIFIER, VOICE_VIEW,
        BLUETOOTH, RESOLUTION, NETWORKING,
        REBOOT, FACTORY_RESET,
    )

    /**
     * Slots that exist on the physical remote or that people commonly want, but whose
     * codes are not yet published for this panel family. Each one is a Code Lab target;
     * once found, the discovered code is saved into the user profile and the button on
     * the remote comes alive automatically.
     */
    val UNMAPPED_SLOTS: List<UnmappedSlot> = listOf(
        UnmappedSlot("search", "Search"),
        UnmappedSlot("live_tv", "Live TV"),
        UnmappedSlot("hbo", "HBO"),
        UnmappedSlot("ps_vue", "PS Vue"),
        UnmappedSlot("input", "Input / Source"),
        UnmappedSlot("hdmi1", "HDMI 1"),
        UnmappedSlot("hdmi2", "HDMI 2"),
        UnmappedSlot("hdmi3", "HDMI 3"),
        UnmappedSlot("power_on", "Power ON (discrete)"),
        UnmappedSlot("power_off", "Power OFF (discrete)"),
        UnmappedSlot("digit_0", "0"),
        UnmappedSlot("digit_1", "1"),
        UnmappedSlot("digit_2", "2"),
        UnmappedSlot("digit_3", "3"),
        UnmappedSlot("digit_4", "4"),
        UnmappedSlot("digit_5", "5"),
        UnmappedSlot("digit_6", "6"),
        UnmappedSlot("digit_7", "7"),
        UnmappedSlot("digit_8", "8"),
        UnmappedSlot("digit_9", "9"),
        UnmappedSlot("dash", "Dash (-)"),
    )
}

data class UnmappedSlot(val id: String, val label: String)
