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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.owentariq.emberlink.data.HuntCandidate
import com.owentariq.emberlink.data.HuntCandidates
import kotlinx.coroutines.delay

/**
 * One button that finds the TV.
 *
 * The previous approach asked the user to reason about NEC addresses and carrier
 * frequencies. That was the wrong shape of solution: they own a remote that does not
 * work and have no way to evaluate any of those choices. This screen replaces all of
 * it — press start, point at the TV, and press the big button the instant anything
 * happens on screen.
 *
 * The TV powering off is the signal. It is unmistakable from across a room, and it
 * works whether the set is currently on or off.
 */
@Composable
fun AutoHuntSection(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onFire: (HuntCandidate) -> Unit,
    onAdopt: (HuntCandidate) -> Unit,
    adopted: HuntCandidate?,
) {
    val plan = remember { HuntCandidates.defaultPlan() }

    var running by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf(false) }
    var speedMs by remember { mutableIntStateOf(750) }

    // Recent history, newest first. The user cannot react instantly, so the code that
    // actually worked is usually two or three back — offering only "the current one"
    // would be a trap.
    val history = remember { mutableStateListOf<HuntCandidate>() }

    LaunchedEffect(running, speedMs) {
        if (!running) return@LaunchedEffect
        while (running && index < plan.size) {
            val candidate = plan[index]
            onFire(candidate)
            history.add(0, candidate)
            if (history.size > 10) history.removeAt(history.lastIndex)
            index++
            delay(speedMs.toLong())
        }
        if (index >= plan.size) running = false
    }

    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            "Find my TV",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "This tries every plausible TV code until one works — different protocols, " +
                "different addresses, ${plan.size} combinations in all. Point the top edge " +
                "of the phone at the TV and press start. The moment the TV reacts, hit the " +
                "big orange button.",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )

        adopted?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(Color(0xFF14301B), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF3DDC84), RoundedCornerShape(10.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Text(
                        "Working code saved",
                        color = Color(0xFF3DDC84),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${it.label} · ${it.protocol.label} · ${it.addressHex} · cmd ${it.commandHex}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        "The whole remote now uses this. Go try the Remote tab.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        // --- progress -----------------------------------------------------------
        LinearProgressIndicator(
            progress = { if (plan.isEmpty()) 0f else index.toFloat() / plan.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = Ember,
        )
        Spacer(Modifier.height(8.dp))

        val current = plan.getOrNull(index.coerceAtMost(plan.size - 1))
        Text(
            if (running) {
                "Trying ${current?.label} · ${current?.protocol?.label} · " +
                    "${current?.addressHex} cmd ${current?.commandHex}"
            } else if (index == 0) {
                "Not started"
            } else {
                "Paused at $index of ${plan.size}"
            },
            color = if (running) Ember else TextMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            "$index of ${plan.size}",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(14.dp))

        // --- the big button --------------------------------------------------------
        Button(
            onClick = {
                running = false
                picking = true
            },
            enabled = history.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Ember,
                contentColor = Color.Black,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
        ) {
            Text("THE TV JUST REACTED", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { running = !running },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running) Color(0xFF7A2E18) else KeyFaceHot,
                    contentColor = Color.White,
                ),
                modifier = Modifier.weight(1f),
            ) { Text(if (running) "Pause" else if (index == 0) "Start hunting" else "Resume") }

            OutlinedButton(
                onClick = {
                    running = false
                    index = 0
                    history.clear()
                },
                modifier = Modifier.weight(1f),
            ) { Text("Restart") }
        }

        Spacer(Modifier.height(10.dp))

        // --- pace ------------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(400 to "Fast", 750 to "Normal", 1200 to "Slow").forEach { (ms, label) ->
                val on = speedMs == ms
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (on) Ember else KeyFace, RoundedCornerShape(8.dp))
                        .clickable { speedMs = ms }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (on) Color.Black else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "The first ${HuntCandidates.KNOWN_POWER.size} tries are documented power codes " +
                "for panels like yours — if one of those lands, you are done in under a " +
                "minute. After that it brute-forces every command on each address.",
            color = TextMuted,
            fontSize = 12.sp,
        )
    }

    // --- pick which one worked -------------------------------------------------------
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Which one was it?") },
            text = {
                Column {
                    Text(
                        "You will have reacted a moment after the code actually fired, so " +
                            "it is usually the second or third in this list. Pick one and " +
                            "the whole remote switches to it.",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    history.take(6).forEachIndexed { i, c ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .background(KeyFace, RoundedCornerShape(8.dp))
                                .clickable {
                                    onAdopt(c)
                                    picking = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Column {
                                Text(
                                    if (i == 0) "just now" else "$i back",
                                    color = if (i == 0) Ember else TextMuted,
                                    fontSize = 11.sp,
                                )
                                Text(
                                    "${c.label} · ${c.protocol.label}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${c.addressHex}  cmd ${c.commandHex}",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picking = false }) { Text("Cancel", color = TextMuted) }
            },
        )
    }
}
