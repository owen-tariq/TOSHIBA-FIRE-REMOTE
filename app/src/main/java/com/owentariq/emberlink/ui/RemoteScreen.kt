package com.owentariq.emberlink.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.owentariq.emberlink.data.Commands
import com.owentariq.emberlink.data.IrCommand

/**
 * The main pad. Layout mirrors the physical Insignia / Toshiba Fire TV Edition remote
 * top to bottom so muscle memory carries over: power, search, the back/home/menu bar,
 * the ring, transport, the volume cluster, then the app row.
 */
@Composable
fun RemoteScreen(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    resolve: (String) -> IrCommand?,
    onSend: (IrCommand) -> Unit,
    onUnmappedTapped: (String) -> Unit,
) {
    // Fire a slot by id. Built-in codes go straight out; slots with no known code yet
    // bounce the user into Code Lab instead of silently doing nothing.
    val fire: (String) -> Unit = { id ->
        val cmd = resolve(id)
        if (cmd != null) onSend(cmd) else onUnmappedTapped(id)
    }

    fun known(id: String) = resolve(id) != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- power ---------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            RoundKey(
                icon = Icons.Rounded.PowerSettingsNew,
                contentDescription = "Power",
                enabled = enabled,
                face = Color(0xFF2A1C18),
                tint = Ember,
                onFire = { fire(Commands.POWER.id) },
            )
        }

        Spacer(Modifier.height(14.dp))

        // --- search --------------------------------------------------------
        RoundKey(
            icon = Icons.Rounded.Search,
            contentDescription = "Search",
            enabled = enabled,
            // "search" is an unmapped slot; magnifier is the closest confirmed code.
            onFire = { if (known("search")) fire("search") else fire(Commands.MAGNIFIER.id) },
        )

        Spacer(Modifier.height(18.dp))

        // --- back / home / menu --------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundKey(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                enabled = enabled,
                onFire = { fire(Commands.BACK.id) },
            )
            RoundKey(
                icon = Icons.Rounded.Home,
                contentDescription = "Home",
                enabled = enabled,
                onFire = { fire(Commands.HOME.id) },
            )
            RoundKey(
                icon = Icons.Rounded.Menu,
                contentDescription = "Menu",
                enabled = enabled,
                onFire = { fire(Commands.MENU.id) },
            )
        }

        Spacer(Modifier.height(22.dp))

        // --- navigation ring -------------------------------------------------
        DPad(
            enabled = enabled,
            onUp = { fire(Commands.UP.id) },
            onDown = { fire(Commands.DOWN.id) },
            onLeft = { fire(Commands.LEFT.id) },
            onRight = { fire(Commands.RIGHT.id) },
            onSelect = { fire(Commands.SELECT.id) },
        )

        Spacer(Modifier.height(22.dp))

        // --- transport -------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundKey(
                icon = Icons.Rounded.FastRewind,
                contentDescription = "Rewind",
                enabled = enabled,
                repeat = true,
                onFire = { fire(Commands.REWIND.id) },
            )
            RoundKey(
                icon = Icons.Rounded.PlayArrow,
                contentDescription = "Play or pause",
                enabled = enabled,
                onFire = { fire(Commands.PLAY_PAUSE.id) },
            )
            RoundKey(
                icon = Icons.Rounded.FastForward,
                contentDescription = "Fast forward",
                enabled = enabled,
                repeat = true,
                onFire = { fire(Commands.FAST_FORWARD.id) },
            )
        }

        Spacer(Modifier.height(22.dp))

        // --- live tv / volume / mute ------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundKey(
                icon = Icons.Rounded.LiveTv,
                contentDescription = "Live TV",
                enabled = enabled,
                onFire = { if (known("live_tv")) fire("live_tv") else fire(Commands.GUIDE.id) },
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RoundKey(
                    label = "+",
                    contentDescription = "Volume up",
                    enabled = enabled,
                    repeat = true,
                    onFire = { fire(Commands.VOL_UP.id) },
                )
                Spacer(Modifier.height(10.dp))
                RoundKey(
                    label = "−",
                    contentDescription = "Volume down",
                    enabled = enabled,
                    repeat = true,
                    onFire = { fire(Commands.VOL_DOWN.id) },
                )
            }

            RoundKey(
                icon = Icons.Rounded.VolumeOff,
                contentDescription = "Mute",
                enabled = enabled,
                onFire = { fire(Commands.MUTE.id) },
            )
        }

        Spacer(Modifier.height(26.dp))

        // --- app launch row ----------------------------------------------------
        SectionLabel("Apps", Modifier.align(Alignment.Start))
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillKey(
                modifier = Modifier.weight(1f),
                label = "prime video",
                enabled = enabled,
                face = PrimeBlue,
                tint = Color.White,
                onFire = { fire(Commands.PRIME_VIDEO.id) },
            )
            PillKey(
                modifier = Modifier.weight(1f),
                label = "NETFLIX",
                enabled = enabled,
                face = NetflixRed,
                tint = Color.White,
                onFire = { fire(Commands.NETFLIX.id) },
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillKey(
                modifier = Modifier.weight(1f),
                label = if (known("hbo")) "HBO" else "HBO ·  find",
                enabled = enabled,
                face = if (known("hbo")) HboPurple else KeyFace,
                tint = if (known("hbo")) Color.White else TextMuted,
                onFire = { fire("hbo") },
            )
            PillKey(
                modifier = Modifier.weight(1f),
                label = if (known("ps_vue")) "PS Vue" else "PS Vue ·  find",
                enabled = enabled,
                face = if (known("ps_vue")) VueBlue else KeyFace,
                tint = if (known("ps_vue")) Color.White else TextMuted,
                onFire = { fire("ps_vue") },
            )
        }

        Spacer(Modifier.height(10.dp))

        // Bonus apps that are confirmed on this panel family but absent from the
        // physical remote. Free real estate on a touchscreen.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillKey(
                modifier = Modifier.weight(1f),
                label = "Disney+",
                enabled = enabled,
                face = DisneyBlue,
                tint = Color.White,
                onFire = { fire(Commands.DISNEY_PLUS.id) },
            )
            PillKey(
                modifier = Modifier.weight(1f),
                label = "hulu",
                enabled = enabled,
                face = HuluGreen,
                tint = Color.Black,
                onFire = { fire(Commands.HULU.id) },
            )
            PillKey(
                modifier = Modifier.weight(1f),
                label = "Freevee",
                enabled = enabled,
                face = KeyFace,
                onFire = { fire(Commands.FREEVEE.id) },
            )
        }

        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.width(1.dp))
    }
}
