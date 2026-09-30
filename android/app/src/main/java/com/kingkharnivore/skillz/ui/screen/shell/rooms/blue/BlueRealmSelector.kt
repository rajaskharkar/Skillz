package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.theme.GryffindorBlack
import com.kingkharnivore.skillz.utils.shell.CreatureRealm
import com.kingkharnivore.skillz.utils.shell.CreatureZone

@Composable
fun BlueRealmSelector(onSelect: (CreatureRealm) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.matchParentSize()) {
            drawLandEnvironment(CreatureZone.GOLDEN_FIELDS)
            drawRect(Brush.verticalGradient(listOf(Color(0xFF86AAA6),Color(0xFF456D7C))),
                topLeft=Offset(0f,size.height*.49f),size=androidx.compose.ui.geometry.Size(size.width,size.height*.51f))
            repeat(6) { i -> drawLine(Color.White.copy(alpha=.12f),Offset(0f,size.height*(.55f+i*.07f)),Offset(size.width,size.height*(.52f+i*.07f)),2f) }
        }
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.shell_room_the_blue_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = GryffindorBlack,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.land_realm_subtitle),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = GryffindorBlack,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.weight(.5f))
            RealmEntry(stringResource(R.string.land_realm_land), stringResource(R.string.land_realm_land_description)) { onSelect(CreatureRealm.LAND) }
            Spacer(Modifier.weight(1f))
            RealmEntry(stringResource(R.string.land_realm_sea), stringResource(R.string.land_realm_sea_description)) { onSelect(CreatureRealm.SEA) }
            Spacer(Modifier.weight(.6f))
        }
    }
}

@Composable
private fun RealmEntry(title: String, subtitle: String, onClick: () -> Unit) {
    val enterLabel = stringResource(R.string.land_enter_realm, title)
    TheBlueOverlaySurface(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = enterLabel, onClick = onClick)) {
        Column(Modifier.padding(vertical=12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold)
            Text(subtitle, style=MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.land_enter_realm_action, title), color=MaterialTheme.colorScheme.primary, style=MaterialTheme.typography.labelLarge)
        }
    }
}
