package com.owentariq.emberlink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.owentariq.emberlink.data.Commands
import com.owentariq.emberlink.data.IrCommand
import com.owentariq.emberlink.data.KeyPosition
import com.owentariq.emberlink.data.Move
import com.owentariq.emberlink.data.OnScreenKeyboard
import kotlinx.coroutines.delay

/**
 * Type on the TV from the phone.
 *
 * Infrared cannot carry text, so this drives the TV's own on-screen keyboard: it works
 * out the D-pad route to each letter and walks it. The catch is that the app is flying
 * blind — there is no feedback channel from the TV — so it has to track where the
 * highlight is by dead reckoning. Hence the grid below: it shows where Emberlink
 * *believes* the cursor sits, and you can tap any cell to correct it if the two drift
 * apart.
 */
@Composable
fun KeyboardScreen(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onSendCommand: (IrCommand) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var layoutText by remember { mutableStateOf(OnScreenKeyboard.FIRE_TV_SEARCH.joinToString("\n")) }
    var wrapEdges by remember { mutableStateOf(true) }
    var stepDelay by remember { mutableStateOf(260f) }
    var editingLayout by remember { mutableStateOf(false) }

    var cursorRow by remember { mutableIntStateOf(0) }
    var cursorCol by remember { mutableIntStateOf(0) }

    var running by remember { mutableStateOf(false) }
    var queue by remember { mutableStateOf<List<Move>>(emptyList()) }
    var sent by remember { mutableIntStateOf(0) }

    val keyboard = remember(layoutText, wrapEdges) {
        OnScreenKeyboard.parse(layoutText, wrapEdges)
    }

    fun moveToCommand(m: Move): IrCommand = when (m) {
        Move.UP -> Commands.UP
        Move.DOWN -> Commands.DOWN
        Move.LEFT -> Commands.LEFT
        Move.RIGHT -> Commands.RIGHT
        Move.SELECT -> Commands.SELECT
    }

    // Walk the queued route one move at a time so the TV's UI can keep up and so the
    // run stays cancellable mid-word.
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (running && sent < queue.size) {
            onSendCommand(moveToCommand(queue[sent]))
            sent++
            delay(stepDelay.toLong())
        }
        running = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Type on TV", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Infrared can't send text, so Emberlink drives the TV's own on-screen keyboard " +
                "with the D-pad. Open a search box on the TV first, then type here.",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text to send") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        val missing = keyboard.unsupported(text)
        if (missing.isNotEmpty()) {
            Text(
                "Not on this layout, will be skipped: ${missing.joinToString(" ")}",
                color = Color(0xFFFFAB91),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- cursor grid ---------------------------------------------------------
        SectionLabel("Where the highlight is")
        Text(
            "Emberlink is guessing this from what it has sent. If the TV disagrees, tap " +
                "the cell that is actually highlighted to resync.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )

        keyboard.rows.forEachIndexed { r, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEachIndexed { c, ch ->
                    val isCursor = r == cursorRow && c == cursorCol
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                if (isCursor) Ember else KeyFace,
                                RoundedCornerShape(8.dp),
                            )
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(enabled = !running) {
                                cursorRow = r
                                cursorCol = c
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            when (ch) {
                                OnScreenKeyboard.SPACE_GLYPH -> "␣"
                                OnScreenKeyboard.BACKSPACE_GLYPH -> "⌫"
                                else -> ch.uppercase()
                            },
                            color = if (isCursor) Color.Black else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (isCursor) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
                // Pad short rows so cells stay square and aligned.
                repeat(keyboard.colCount - row.length) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(14.dp))

        // --- pacing + wrap ---------------------------------------------------------
        SectionLabel("Gap between presses — ${stepDelay.toInt()} ms")
        Slider(
            value = stepDelay,
            onValueChange = { stepDelay = it },
            valueRange = 120f..600f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Too fast and the TV drops presses, which corrupts everything after it. " +
                "260 ms is a safe starting point.",
            color = TextMuted,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Wrap at the edges", color = TextPrimary, fontSize = 14.sp)
                Text(
                    "Cuts long jumps short if your keyboard wraps around. Turn off if it doesn't.",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
            Switch(checked = wrapEdges, onCheckedChange = { wrapEdges = it })
        }

        Spacer(Modifier.height(16.dp))

        // --- run -------------------------------------------------------------------
        val preview = remember(text, layoutText, wrapEdges, cursorRow, cursorCol) {
            keyboard.routeText(text, KeyPosition(cursorRow, cursorCol)).first
        }

        if (queue.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { sent.toFloat() / queue.size.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Ember,
            )
            Text(
                "$sent of ${queue.size} presses",
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(10.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (running) {
                        running = false
                    } else {
                        val (moves, endCursor) =
                            keyboard.routeText(text, KeyPosition(cursorRow, cursorCol))
                        queue = moves
                        sent = 0
                        cursorRow = endCursor.row
                        cursorCol = endCursor.col
                        running = true
                    }
                },
                enabled = enabled && (running || text.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running) Color(0xFF7A2E18) else Ember,
                    contentColor = if (running) Color.White else Color.Black,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (running) "Stop" else "Type it  ·  ${preview.size} presses",
                    fontWeight = FontWeight.Bold,
                )
            }

            OutlinedButton(
                onClick = {
                    running = false
                    queue = emptyList()
                    sent = 0
                },
                modifier = Modifier.weight(0.5f),
            ) { Text("Clear") }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onSendCommand(Commands.SELECT) },
                enabled = enabled && !running,
                modifier = Modifier.weight(1f),
            ) { Text("OK") }
            OutlinedButton(
                onClick = { onSendCommand(Commands.BACK) },
                enabled = enabled && !running,
                modifier = Modifier.weight(1f),
            ) { Text("Back") }
        }

        Spacer(Modifier.height(20.dp))

        // --- layout editor -----------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Keyboard layout")
            Text(
                if (editingLayout) "Done" else "Edit",
                color = Ember,
                fontSize = 13.sp,
                modifier = Modifier.clickable { editingLayout = !editingLayout },
            )
        }

        if (editingLayout) {
            Spacer(Modifier.height(8.dp))
            Text(
                "One line per row, one character per cell. Use _ for space and < for " +
                    "backspace. Amazon has shipped more than one arrangement, so if the " +
                    "letters come out wrong, match this to what's actually on your screen.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = layoutText,
                onValueChange = { layoutText = it },
                label = { Text("Grid") },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    color = TextPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    layoutText = OnScreenKeyboard.FIRE_TV_SEARCH.joinToString("\n")
                },
            ) { Text("Reset to Fire TV default") }
        }

        Spacer(Modifier.height(40.dp))
    }
}
