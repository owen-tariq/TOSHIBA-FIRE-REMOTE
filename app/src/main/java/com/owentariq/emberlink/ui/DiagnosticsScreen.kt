package com.owentariq.emberlink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owentariq.emberlink.data.AddressPreset
import com.owentariq.emberlink.data.HuntCandidate
import com.owentariq.emberlink.data.Settings
import kotlinx.coroutines.delay

/**
 * The screen for when nothing happens.
 *
 * Infrared gives no feedback, so a dead remote and a perfectly working one look
 * identical from inside the app. This screen exists to break that tie: it reports what
 * the hardware says about itself, then lets the user change the three things that
 * actually matter — repeat count, address, carrier — while blasting a known code on a
 * loop so they can test with both hands free.
 */
@Composable
fun DiagnosticsScreen(
    modifier: Modifier = Modifier,
    settings: Settings,
    deviceLabel: String,
    hasEmitter: Boolean,
    managerPresent: Boolean,
    carrierSummary: String,
    carrierSupported: Boolean,
    lastError: String?,
    framesSent: Int,
    onBlast: () -> Unit,
    onSettingsChanged: () -> Unit,
    onHuntFire: (HuntCandidate) -> Unit,
    onHuntAdopt: (HuntCandidate) -> Unit,
    adopted: HuntCandidate?,
) {
    var blasting by remember { mutableStateOf(false) }
    var blastCount by remember { mutableIntStateOf(0) }
    var frames by remember { mutableIntStateOf(settings.framesPerPress) }
    var addr by remember { mutableIntStateOf(settings.addressOverride) }
    var sub by remember { mutableIntStateOf(settings.subAddressOverride) }
    var carrier by remember { mutableIntStateOf(settings.carrierHz) }

    // Fire Power on a loop so the user can aim, walk closer, and change angle without
    // needing a third hand to keep tapping.
    LaunchedEffect(blasting) {
        if (!blasting) return@LaunchedEffect
        while (blasting) {
            onBlast()
            blastCount++
            delay(700)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Auto-Hunt goes first: it is the answer for "nothing works" and requires no
        // understanding of anything below it.
        AutoHuntSection(
            enabled = hasEmitter,
            onFire = onHuntFire,
            onAdopt = onHuntAdopt,
            adopted = adopted,
        )

        Spacer(Modifier.height(28.dp))
        Text(
            "Manual diagnostics",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Only needed if Find my TV came up empty.",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        // --- 1. hardware ------------------------------------------------------
        SectionLabel("1 · Phone hardware")
        Spacer(Modifier.height(8.dp))
        InfoRow("Device", deviceLabel)
        InfoRow("IR service", if (managerPresent) "present" else "MISSING", !managerPresent)
        InfoRow("IR emitter", if (hasEmitter) "yes" else "NOT DETECTED", !hasEmitter)
        InfoRow("Carrier ranges", carrierSummary)
        InfoRow(
            "Using ${carrier} Hz",
            if (carrierSupported) "supported" else "NOT in a reported range",
            !carrierSupported,
        )
        InfoRow("Frames transmitted", framesSent.toString())
        InfoRow("Last error", lastError ?: "none", lastError != null)

        if (hasEmitter && lastError == null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "The phone is emitting. If the TV still does nothing, the problem is the " +
                    "code or the aim, not the hardware — keep going.",
                color = TextMuted,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(20.dp))

        // --- 2. blast test ------------------------------------------------------
        SectionLabel("2 · Aim test")
        Text(
            "Sends Power every 0.7 s. Stand 1–2 m away, point the TOP EDGE of the phone " +
                "at the TV, and sweep slowly. A phone blaster is far weaker and narrower " +
                "than the real remote, so aim matters more than you would expect.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Button(
            onClick = { blasting = !blasting },
            enabled = hasEmitter,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (blasting) Color(0xFF7A2E18) else Ember,
                contentColor = if (blasting) Color.White else Color.Black,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (blasting) "Stop  ·  $blastCount sent" else "Blast Power on a loop",
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Tip: point a phone camera at the blaster while this runs. Most cameras see " +
                "IR as a faint purple flicker — that confirms the emitter is firing.",
            color = TextMuted,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(20.dp))

        // --- 3. repeats ---------------------------------------------------------
        SectionLabel("3 · Frames per press — $frames")
        Text(
            "Many receivers ignore a single frame and only react once they see the code " +
                "repeat. If the TV responds only sometimes, raise this.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Slider(
            value = frames.toFloat(),
            onValueChange = {
                frames = it.toInt().coerceIn(1, 5)
                settings.framesPerPress = frames
                onSettingsChanged()
            },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        // --- 4. address ----------------------------------------------------------
        SectionLabel("4 · Device address")
        Text(
            "Every built-in code assumes NECext 02 7D, which is documented for Fire TV " +
                "Edition panels. If your set answers somewhere else, nothing works and " +
                "there is no error to see. Try each preset with the aim test running.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )

        Settings.ADDRESS_PRESETS.forEach { preset ->
            AddressCard(
                preset = preset,
                selected = addr == preset.address && sub == preset.subAddress,
                onPick = {
                    addr = preset.address
                    sub = preset.subAddress
                    settings.addressOverride = addr
                    settings.subAddressOverride = sub
                    onSettingsChanged()
                },
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- 5. carrier -------------------------------------------------------------
        SectionLabel("5 · Carrier frequency")
        Text(
            "Standard NEC is 38 kHz. Only change this if the hardware says 38 kHz is out " +
                "of range, or nothing else has worked.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(36_000, 38_000, 38_400, 40_000, 56_000).forEach { hz ->
                val on = carrier == hz
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (on) Ember else KeyFace, RoundedCornerShape(8.dp))
                        .clickable {
                            carrier = hz
                            settings.carrierHz = hz
                            onSettingsChanged()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${hz / 1000}k",
                        color = if (on) Color.Black else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                settings.resetToDefaults()
                frames = settings.framesPerPress
                addr = settings.addressOverride
                sub = settings.subAddressOverride
                carrier = settings.carrierHz
                onSettingsChanged()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset everything to defaults") }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String, bad: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Text(
            value,
            color = if (bad) Color(0xFFFF8A80) else TextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (bad) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun AddressCard(preset: AddressPreset, selected: Boolean, onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(if (selected) KeyFaceHot else KeyFace, RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (selected) Ember else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(10.dp),
            )
            .clickable { onPick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    preset.label,
                    color = if (selected) Ember else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "%02X %02X".format(preset.address, preset.subAddress),
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text(preset.note, color = TextMuted, fontSize = 12.sp)
        }
    }
}
