package com.owentariq.emberlink.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owentariq.emberlink.data.Commands
import com.owentariq.emberlink.data.IrCommand

/**
 * Everything that isn't on the plastic remote: the tuner keypad, discrete inputs,
 * discrete power, the Fire OS system menus, and a walled-off danger zone.
 *
 * Keys whose codes haven't been discovered yet are drawn dimmed and route to Code Lab
 * on tap, so the screen doubles as a to-do list of what's left to find.
 */
@Composable
fun KeypadScreen(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    resolve: (String) -> IrCommand?,
    onSend: (IrCommand) -> Unit,
    onUnmappedTapped: (String) -> Unit,
) {
    var confirming by remember { mutableStateOf<IrCommand?>(null) }

    val fire: (String) -> Unit = { id ->
        val cmd = resolve(id)
        if (cmd == null) onUnmappedTapped(id) else onSend(cmd)
    }

    fun known(id: String) = resolve(id) != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // --- number pad ------------------------------------------------------
        SectionLabel("Number pad")
        Text(
            "For the built-in antenna tuner. Codes are not published for this panel — " +
                "tap a dimmed key to hunt it down in Code Lab.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        listOf(
            listOf("digit_1", "digit_2", "digit_3"),
            listOf("digit_4", "digit_5", "digit_6"),
            listOf("digit_7", "digit_8", "digit_9"),
            listOf("dash", "digit_0", "input"),
        ).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { id ->
                    val label = when (id) {
                        "dash" -> "–"
                        "input" -> "INPUT"
                        else -> id.removePrefix("digit_")
                    }
                    RoundKey(
                        size = 62.dp,
                        label = label,
                        contentDescription = label,
                        enabled = enabled,
                        tint = if (known(id)) TextPrimary else TextMuted,
                        onFire = { fire(id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // --- inputs ------------------------------------------------------------
        SectionLabel("Inputs")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf("hdmi1" to "HDMI 1", "hdmi2" to "HDMI 2", "hdmi3" to "HDMI 3").forEach { (id, label) ->
                PillKey(
                    modifier = Modifier.weight(1f),
                    label = label,
                    enabled = enabled,
                    tint = if (known(id)) TextPrimary else TextMuted,
                    onFire = { fire(id) },
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // --- discrete power ------------------------------------------------------
        SectionLabel("Discrete power")
        Text(
            "Unlike the toggle on the main pad, these force a specific state — useful " +
                "when you can't see the TV.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillKey(
                modifier = Modifier.weight(1f),
                label = "POWER ON",
                enabled = enabled,
                tint = if (known("power_on")) TextPrimary else TextMuted,
                onFire = { fire("power_on") },
            )
            PillKey(
                modifier = Modifier.weight(1f),
                label = "POWER OFF",
                enabled = enabled,
                tint = if (known("power_off")) TextPrimary else TextMuted,
                onFire = { fire("power_off") },
            )
        }

        Spacer(Modifier.height(18.dp))

        // --- channels ------------------------------------------------------------
        SectionLabel("Channels")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillKey(
                modifier = Modifier.weight(1f),
                label = "CH +",
                enabled = enabled,
                onFire = { fire(Commands.CHANNEL_UP.id) },
            )
            PillKey(
                modifier = Modifier.weight(1f),
                label = "CH −",
                enabled = enabled,
                onFire = { fire(Commands.CHANNEL_DOWN.id) },
            )
            PillKey(
                modifier = Modifier.weight(1f),
                label = "GUIDE",
                enabled = enabled,
                onFire = { fire(Commands.GUIDE.id) },
            )
        }

        Spacer(Modifier.height(18.dp))

        // --- system --------------------------------------------------------------
        SectionLabel("Fire OS menus")
        Spacer(Modifier.height(10.dp))

        listOf(
            Commands.SETTINGS,
            Commands.RECENT_APPS,
            Commands.NETWORKING,
            Commands.BLUETOOTH,
            Commands.RESOLUTION,
            Commands.VOICE_VIEW,
            Commands.ALEXA,
        ).chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { cmd ->
                    PillKey(
                        modifier = Modifier.weight(1f),
                        label = cmd.label,
                        enabled = enabled,
                        onFire = { onSend(cmd) },
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- danger zone -----------------------------------------------------------
        SectionLabel("Danger zone")
        Text(
            "These fire instantly on the TV with no on-screen prompt. Confirmation here " +
                "is the only safety net.",
            color = Color(0xFFFF8A80),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillKey(
                modifier = Modifier.weight(1f),
                label = "REBOOT",
                enabled = enabled,
                face = Color(0xFF3A2020),
                tint = Color(0xFFFFAB91),
                onFire = { confirming = Commands.REBOOT },
            )
            PillKey(
                modifier = Modifier.weight(1f),
                label = "FACTORY RESET",
                enabled = enabled,
                face = Color(0xFF3A2020),
                tint = Color(0xFFFF8A80),
                onFire = { confirming = Commands.FACTORY_RESET },
            )
        }

        Spacer(Modifier.height(40.dp))
    }

    confirming?.let { cmd ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(cmd.label) },
            text = {
                Text(
                    if (cmd.id == Commands.FACTORY_RESET.id) {
                        "This wipes the TV back to out-of-box state. Every app, login and " +
                            "setting on the panel is gone, and the TV will not ask you to confirm. " +
                            "Are you certain?"
                    } else {
                        "This restarts the TV immediately. Anything playing will stop."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSend(cmd)
                    confirming = null
                }) { Text("Send it", color = Color(0xFFFF8A80)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text("Cancel") }
            },
        )
    }
}
