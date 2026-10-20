package com.kingkharnivore.skillz.ui.screen.shell

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellDropIcon
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellPearlMiniIcon
import com.kingkharnivore.skillz.ui.screen.shell.rooms.red.PebbleIcon
import java.text.NumberFormat

/** Equal-width balances shared by the persistent header and the heart of The Shell. */
@Composable
internal fun ShellBalances(
    pearls: Int,
    pebbles: Int,
    drops: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ShellBalance(pearls.toLong(), stringResource(R.string.shell_currency_pearls), Modifier.weight(1f)) {
            ShellPearlMiniIcon(it)
        }
        ShellBalance(pebbles.toLong(), stringResource(R.string.shell_currency_pebbles), Modifier.weight(1f)) {
            PebbleIcon(it)
        }
        ShellBalance(drops, stringResource(R.string.shell_currency_drops), Modifier.weight(1f)) {
            ShellDropIcon(it)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShellBalance(
    balance: Long,
    label: String,
    modifier: Modifier,
    icon: @Composable (Modifier) -> Unit
) {
    var showExactBalance by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val exact = remember(balance, locale) { NumberFormat.getIntegerInstance(locale).format(balance) }
    val display = remember(balance, locale) {
        if (balance < 10_000) exact else CompactDecimalFormat.getInstance(
            locale, CompactDecimalFormat.CompactStyle.SHORT
        ).apply { maximumSignificantDigits = 3 }.format(balance)
    }
    Column(
        modifier = modifier.heightIn(min = 48.dp)
            .clickable(role = Role.Button) { showExactBalance = true }
            .clearAndSetSemantics {
                contentDescription = "$label: $exact"
                role = Role.Button
                onClick { showExactBalance = true; true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            icon(Modifier.size(20.dp).align(Alignment.CenterVertically))
            Text(
                display,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
    if (showExactBalance) {
        AlertDialog(
            onDismissRequest = { showExactBalance = false },
            title = { Text(label) },
            text = { Text(exact, style = MaterialTheme.typography.headlineSmall) },
            confirmButton = {
                TextButton(onClick = { showExactBalance = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }
}
