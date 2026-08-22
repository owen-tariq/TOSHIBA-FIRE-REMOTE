package com.owentariq.emberlink.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * On-device store for codes discovered in Code Lab.
 *
 * Plain JSON in the app's files dir. No network, no database, no cloud. The file is
 * human readable on purpose so it can be copied out and pasted into a bug report,
 * a Flipper .ir file, or a fresh install.
 */
class ProfileStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)

    private val discovered = LinkedHashMap<String, IrCommand>()

    init {
        load()
    }

    /** Discovered code for a slot id, if the user has found one. */
    fun get(id: String): IrCommand? = discovered[id]

    fun all(): List<IrCommand> = discovered.values.toList()

    fun isEmpty(): Boolean = discovered.isEmpty()

    fun save(command: IrCommand) {
        discovered[command.id] = command
        persist()
    }

    fun remove(id: String) {
        discovered.remove(id)
        persist()
    }

    fun clear() {
        discovered.clear()
        persist()
    }

    /**
     * Resolve a slot to a sendable command: the built-in code if there is one,
     * otherwise whatever the user discovered.
     */
    fun resolve(id: String): IrCommand? =
        Commands.ALL.firstOrNull { it.id == id } ?: discovered[id]

    /** The whole profile as pretty JSON, for copying to the clipboard. */
    fun toJson(): String {
        val arr = JSONArray()
        discovered.values.forEach { cmd ->
            arr.put(
                JSONObject().apply {
                    put("id", cmd.id)
                    put("label", cmd.label)
                    put("address", cmd.address)
                    put("subAddress", cmd.subAddress)
                    put("command", cmd.command)
                }
            )
        }
        return JSONObject().apply {
            put("profile", "Emberlink")
            put("protocol", "NECext")
            put("commands", arr)
        }.toString(2)
    }

    /** The profile rendered as a Flipper Zero .ir file, so it is portable off the phone. */
    fun toFlipperIr(): String = buildString {
        appendLine("Filetype: IR signals file")
        appendLine("Version: 1")
        appendLine("#")
        appendLine("# Emberlink - discovered Fire TV Edition codes")
        appendLine("#")
        discovered.values.forEach { cmd ->
            appendLine("name: ${cmd.label.replace(' ', '_')}")
            appendLine("type: parsed")
            appendLine("protocol: NECext")
            appendLine("address: %02X %02X 00 00".format(cmd.address, cmd.subAddress))
            appendLine("command: %02X %02X 00 00".format(cmd.command, cmd.command.inv() and 0xFF))
            appendLine("#")
        }
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())
            val arr = root.optJSONArray("commands") ?: return
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val cmd = IrCommand(
                    id = o.getString("id"),
                    label = o.optString("label", o.getString("id")),
                    command = o.getInt("command"),
                    address = o.optInt("address", IrCommand.FIRE_TV_ADDRESS),
                    subAddress = o.optInt("subAddress", IrCommand.FIRE_TV_SUBADDRESS),
                )
                discovered[cmd.id] = cmd
            }
        } catch (t: Throwable) {
            Log.w("Emberlink", "profile load failed, starting fresh", t)
        }
    }

    private fun persist() {
        try {
            file.writeText(toJson())
        } catch (t: Throwable) {
            Log.w("Emberlink", "profile save failed", t)
        }
    }

    private companion object {
        const val FILE_NAME = "emberlink_profile.json"
    }
}
