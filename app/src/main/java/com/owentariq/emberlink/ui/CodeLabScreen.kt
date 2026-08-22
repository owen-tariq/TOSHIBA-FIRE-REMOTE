package com.owentariq.emberlink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owentariq.emberlink.data.Commands
import com.owentariq.emberlink.data.IrCommand
import com.owentariq.emberlink.data.UnmappedSlot
import kotlinx.coroutines.delay

/**
 * Brute-force code discovery.
 *
 * NEC has 256 possible command bytes per device address, so an exhaustive sweep of the
 * Fire TV address is minutes of work rather than the multi-hour hunt the Sony SIRC
 * sweep needed.
 *
 * The interaction that matters: the sweeper keeps a rolling log of what it just sent.
 * When the TV reacts you tap the code in that log — you are never asked to hit a
 * "capture" button at the exact right instant, which is impossible with human reaction
 * time and an IR round trip.
 */
private data class Shot(val address: Int, val subAddress: Int, val command: Int)

@Composable
fun CodeLabScreen(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    targetSlot: String?,
    onRawSend: (Int, Int, Int) -> Unit,
    onSaveDiscovery: (IrCommand) -> Unit,
    savedCount: Int,
    onExport: () -> Unit,
) {
    var addressText by remember { mutableStateOf("02") }
    var subAddressText by remember { mutableStateOf("7D") }
    var start by remember { mutableStateOf(0) }
    var end by remember { mutableStateOf(255) }
    var intervalMs by remember { mutableStateOf(900f) }

    var running by remember { mutableStateOf(false) }
    var cursor by remember { mutableStateOf(0) }
    val log: SnapshotStateList<Shot> = remember { mutableListOf<Shot>().toMutableStateList() }
    var claiming by remember { mutableStateOf<Shot?>(null) }

    val address = addressText.toIntOrNull(16)?.and(0xFF) ?: 0x02
    val subAddress = subAddressText.toIntOrNull(16)?.and(0xFF) ?: 0x7D

    // Preselect whichever button sent the user here.
    var slotChoice by remember(targetSlot) {
        mutableStateOf(
            Commands.UNMAPPED_SLOTS.firstOrNull { it.id == targetSlot }
                ?: Commands.UNMAPPED_SLOTS.first()
        )
    }

    // The sweep itself.
    LaunchedEffect(running, address, subAddress, intervalMs) {
        if (!running) return@LaunchedEffect
        while (running && cursor <= end) {
            onRawSend(address, subAddress, cursor)
            log.add(0, Shot(address, subAddress, cursor))
            if (log.size > 12) log.removeAt(log.lastIndex)
            cursor++
            delay(intervalMs.toLong())
        }
        if (cursor > end) running = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            "Code Lab",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Point the phone at the TV and start a sweep. The moment the TV reacts, tap " +
                "that code in the log below and give it a name. Two or three codes back is " +
                "normal — reaction time is slower than the sweep.",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        // --- what are we looking for ------------------------------------------
        SectionLabel("Looking for")
        Spacer(Modifier.height(8.dp))
        SlotPicker(slotChoice) { slotChoice = it }

        Spacer(Modifier.height(18.dp))

        // --- address ------------------------------------------------------------
        SectionLabel("NEC address")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = addressText,
                onValueChange = { addressText = it.take(2).uppercase() },
                label = { Text("addr") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = subAddressText,
                onValueChange = { subAddressText = it.take(2).uppercase() },
                label = { Text("sub") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            "02 7D is the confirmed Fire TV Edition address. If a full sweep there finds " +
                "nothing, try 40 BF — some model years put panel functions on NEC device 64.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(18.dp))

        // --- pacing ---------------------------------------------------------------
        SectionLabel("Gap between shots — ${intervalMs.toInt()} ms")
        Slider(
            value = intervalMs,
            onValueChange = { intervalMs = it },
            valueRange = 400f..2000f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Slower is easier to follow. Faster gets through 256 codes quicker but you " +
                "will overshoot more often.",
            color = TextMuted,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(18.dp))

        // --- progress + controls ----------------------------------------------------
        val total = (end - start + 1).coerceAtLeast(1)
        val done = (cursor - start).coerceIn(0, total)
        LinearProgressIndicator(
            progress = { done.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = Ember,
        )
        Text(
            "at 0x%02X  ·  %d of %d".format(cursor.coerceAtMost(255), done, total),
            color = TextMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { running = !running },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = Ember, contentColor = Color.Black),
                modifier = Modifier.weight(1f),
            ) { Text(if (running) "Pause" else "Start sweep", fontWeight = FontWeight.Bold) }

            OutlinedButton(
                onClick = {
                    running = false
                    cursor = start
                    log.clear()
                },
                modifier = Modifier.weight(1f),
            ) { Text("Reset") }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { if (cursor > 0) cursor-- },
                modifier = Modifier.weight(1f),
            ) { Text("◀ Step back") }
            OutlinedButton(
                onClick = {
                    onRawSend(address, subAddress, cursor)
                    log.add(0, Shot(address, subAddress, cursor))
                },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { Text("Re-send") }
            OutlinedButton(
                onClick = { if (cursor < 255) cursor++ },
                modifier = Modifier.weight(1f),
            ) { Text("Step ▶") }
        }

        Spacer(Modifier.height(20.dp))

        // --- rolling log --------------------------------------------------------------
        SectionLabel("Just sent — tap the one that worked")
        Spacer(Modifier.height(8.dp))

        if (log.isEmpty()) {
            Text(
                "Nothing sent yet.",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            log.forEachIndexed { index, shot ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .background(
                            if (index == 0) KeyFaceHot else KeyFace,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { claiming = shot }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "%02X %02X  ·  cmd 0x%02X".format(shot.address, shot.subAddress, shot.command),
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                        )
                        Text(
                            if (index == 0) "latest" else "$index back",
                            color = if (index == 0) Ember else TextMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$savedCount discovered", color = TextMuted, fontSize = 13.sp)
            TextButton(onClick = onExport) { Text("Export profile", color = Ember) }
        }

        Spacer(Modifier.height(40.dp))
    }

    claiming?.let { shot ->
        AlertDialog(
            onDismissRequest = { claiming = null },
            title = { Text("Save 0x%02X".format(shot.command)) },
            text = {
                Text(
                    "Assign this code to \"${slotChoice.label}\"? The matching button will " +
                        "light up on the remote straight away."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveDiscovery(
                        IrCommand(
                            id = slotChoice.id,
                            label = slotChoice.label,
                            command = shot.command,
                            address = shot.address,
                            subAddress = shot.subAddress,
                        )
                    )
                    claiming = null
                }) { Text("Save", color = Ember) }
            },
            dismissButton = { TextButton(onClick = { claiming = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SlotPicker(current: UnmappedSlot, onPick: (UnmappedSlot) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(current.label)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Commands.UNMAPPED_SLOTS.forEach { slot ->
                DropdownMenuItem(
                    text = { Text(slot.label) },
                    onClick = {
                        onPick(slot)
                        open = false
                    },
                )
            }
        }
    }
}
