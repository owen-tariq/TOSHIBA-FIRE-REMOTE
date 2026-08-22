package com.owentariq.emberlink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owentariq.emberlink.data.Commands
import com.owentariq.emberlink.data.HuntCandidate
import com.owentariq.emberlink.data.IrCommand
import com.owentariq.emberlink.data.ProfileStore
import com.owentariq.emberlink.data.Settings
import com.owentariq.emberlink.ir.IrService
import com.owentariq.emberlink.ui.Charcoal
import com.owentariq.emberlink.ui.CodeLabScreen
import com.owentariq.emberlink.ui.DiagnosticsScreen
import com.owentariq.emberlink.ui.Ember
import com.owentariq.emberlink.ui.EmberlinkTheme
import com.owentariq.emberlink.ui.KeyboardScreen
import com.owentariq.emberlink.ui.KeypadScreen
import com.owentariq.emberlink.ui.RemoteScreen
import com.owentariq.emberlink.ui.TextMuted
import com.owentariq.emberlink.ui.TextPrimary

class MainActivity : ComponentActivity() {

    private lateinit var ir: IrService
    private lateinit var profile: ProfileStore
    private lateinit var settings: Settings

    /** Bumped whenever the profile changes, to nudge Compose into recomposing. */
    private var profileRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = Settings(this)
        ir = IrService(this, settings)
        profile = ProfileStore(this)

        // A remote that dims out mid-scroll is useless.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            EmberlinkTheme {
                // Deliberately not gating the whole app on hasIrEmitter any more:
                // if the emitter is missing, Diagnostics is the one screen that can
                // explain why, so the user must still be able to reach it.
                AppScaffold()
            }
        }
    }

    /**
     * Phone volume rocker drives the TV instead of the phone. This is the single most
     * asked-for feature in every IR remote thread, and it costs one override.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!::ir.isInitialized || !ir.hasEmitter) return super.dispatchKeyEvent(event)

        val cmd = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> Commands.VOL_UP
            KeyEvent.KEYCODE_VOLUME_DOWN -> Commands.VOL_DOWN
            else -> null
        } ?: return super.dispatchKeyEvent(event)

        // Fire on the down edge and on auto-repeat; swallow the up edge entirely so the
        // system volume UI never appears.
        if (event.action == KeyEvent.ACTION_DOWN) ir.send(cmd)
        return true
    }

    /** The hunt result currently in force, shown back to the user as confirmation. */
    private var adoptedCandidate by mutableStateOf<HuntCandidate?>(null)

    /**
     * Adopt a code that demonstrably worked.
     *
     * The command byte itself is discarded — it was only ever a probe. What matters is
     * the protocol and address it proved, which every other button then inherits.
     */
    private fun adoptCandidate(c: HuntCandidate) {
        settings.protocol = c.protocol
        settings.addressOverride = c.address
        settings.subAddressOverride = c.subAddress
        settings.hasConfirmedWorkingCode = true
        adoptedCandidate = c
        profileRevision++
        toast("Saved: ${c.protocol.label} ${c.addressHex} — try the Remote tab")
    }

    private fun send(cmd: IrCommand) {
        if (!ir.send(cmd)) {
            toast("IR transmit failed")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun exportProfile() {
        if (profile.isEmpty()) {
            toast("No discovered codes to export yet")
            return
        }
        val payload = buildString {
            appendLine(profile.toFlipperIr())
            appendLine()
            appendLine(profile.toJson())
        }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Emberlink profile", payload))
        toast("Profile copied to clipboard")
    }

    @Composable
    private fun AppScaffold() {
        var tab by remember { mutableIntStateOf(0) }
        var codeLabTarget by remember { mutableStateOf<String?>(null) }

        // Keyed on the revision so every screen recomposes after a discovery is saved.
        val revision = profileRevision
        val resolve: (String) -> IrCommand? = remember(revision) {
            { id -> profile.resolve(id) }
        }

        val jumpToCodeLab: (String) -> Unit = { slot ->
            codeLabTarget = slot
            tab = 3
            toast("No code known for that key yet — sweep for it here")
        }

        Scaffold(
            containerColor = Charcoal,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF17181C)) {
                    val items = listOf(
                        Triple("Remote", Icons.Rounded.SettingsRemote, 0),
                        Triple("Keypad", Icons.Rounded.Dialpad, 1),
                        Triple("Type", Icons.Rounded.Keyboard, 2),
                        Triple("Code Lab", Icons.Rounded.Science, 3),
                        Triple("Fix", Icons.Rounded.MonitorHeart, 4),
                    )
                    items.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = Ember,
                                indicatorColor = Ember,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when (tab) {
                    0 -> RemoteScreen(
                        enabled = true,
                        resolve = resolve,
                        onSend = ::send,
                        onUnmappedTapped = jumpToCodeLab,
                    )

                    1 -> KeypadScreen(
                        enabled = true,
                        resolve = resolve,
                        onSend = ::send,
                        onUnmappedTapped = jumpToCodeLab,
                    )

                    2 -> KeyboardScreen(
                        enabled = true,
                        onSendCommand = ::send,
                    )

                    3 -> CodeLabScreen(
                        enabled = true,
                        targetSlot = codeLabTarget,
                        onRawSend = { a, s, c -> ir.sendRaw(a, s, c) },
                        onSaveDiscovery = { cmd ->
                            profile.save(cmd)
                            profileRevision++
                            toast("Saved ${cmd.label} = 0x%02X".format(cmd.command))
                        },
                        savedCount = profile.all().size,
                        onExport = ::exportProfile,
                    )

                    4 -> DiagnosticsScreen(
                        settings = settings,
                        deviceLabel = ir.deviceLabel,
                        hasEmitter = ir.hasEmitter,
                        managerPresent = ir.managerPresent,
                        carrierSummary = ir.carrierSummary,
                        carrierSupported = ir.carrierSupported,
                        lastError = ir.lastError,
                        framesSent = ir.framesSent.get(),
                        onBlast = { ir.send(Commands.POWER) },
                        onSettingsChanged = { profileRevision++ },
                        onHuntFire = { c ->
                            ir.sendCandidate(c.protocol, c.address, c.subAddress, c.command)
                        },
                        onHuntAdopt = ::adoptCandidate,
                        adopted = adoptedCandidate,
                    )
                }
            }
        }
    }

    @Composable
    private fun NoEmitterScreen(device: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Charcoal)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "No IR blaster",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "$device does not report an infrared emitter, so there is no way for this " +
                    "app to reach your TV.\n\nEmberlink talks to the TV purely over IR — " +
                    "there is no network fallback by design.",
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
