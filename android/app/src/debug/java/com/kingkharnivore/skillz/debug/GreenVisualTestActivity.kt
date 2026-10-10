package com.kingkharnivore.skillz.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import com.kingkharnivore.skillz.data.model.entity.green.GreenActionEntity
import com.kingkharnivore.skillz.domain.green.*
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.*
import com.kingkharnivore.skillz.ui.theme.SkillzTheme
import com.kingkharnivore.skillz.viewmodel.green.GreenUiState

/** Deterministic screenshot harness, isolated from user persistence. */
class GreenVisualTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = intent.getBooleanExtra("dark", false)
        val barStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        val environment=GreenEnvironment.entries.firstOrNull { it.id==intent.getStringExtra("environment") } ?: GreenEnvironment.GARDEN
        val scenario=intent.getStringExtra("scenario") ?: "world"
        val failActions = intent.getBooleanExtra("fail_actions", false)
        val level = scenario.removePrefix("level").toIntOrNull()
        val milestonePlant = level?.let { GreenPreviewFixtures.specimen(GreenCatalogue.inEnvironment(environment).last(), it, 1) }

        setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, if (scenario == "large-text") 1.3f else density.fontScale)) {
            SkillzTheme(darkTheme=dark,dynamicColor=false) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    if(scenario=="portraits") GreenBotanicalPortraits(environment)
                    else if(scenario=="growth-collections") GrowthBadgePreviewContent(true)
                    else if(scenario=="growth-badges") GrowthBadgePreviewContent()
                    else if(scenario=="stages") GreenStagesPreview()
                    else if (scenario=="collection") GreenChestPreviewContent(GreenPreviewFixtures.mixed(environment))
                    else if (scenario=="badges") GreenBadgePreviewContent(GreenPreviewFixtures.mixed(environment))
                    else {
                        var state by remember { mutableStateOf(if(scenario=="empty") GreenUiState() else if(milestonePlant != null) GreenUiState(250_000, listOf(milestonePlant)) else GreenPreviewFixtures.mixed(environment).copy(drops=intent.getLongExtra("drops",250_000))) }
                        GreenScreen(state,onPlant=plant@{ id,token ->
                            if (failActions) { state=state.copy(messageRes=com.kingkharnivore.skillz.R.string.green_error); return@plant }
                            val species=GreenCatalogue.byId.getValue(id);val cost=GreenEconomy.seedCost(species.tier)
                            val plant=GreenPreviewFixtures.specimen(species,1,(state.specimens.size+1).toLong())
                            state=state.copy(drops=state.drops-cost,specimens=state.specimens+plant,action=GreenActionEntity(token,"plant_seed_planted",id,plant.id,0,1,cost,state.drops-cost,GreenPreviewFixtures.DATE))
                        }, onWater=water@{ id,level,token ->
                            if (failActions) { state=state.copy(messageRes=com.kingkharnivore.skillz.R.string.green_error); return@water }
                            val plant=state.specimens.first { it.id==id };val species=GreenCatalogue.byId.getValue(plant.speciesId);val cost=GreenEconomy.waterCost(species.tier,level)
                            val grown=plant.copy(level=level+1,fullyGrownAt=plant.fullyGrownAt ?: (GreenPreviewFixtures.DATE+172_800_000).takeIf { level+1>=90 },masteredAt=plant.masteredAt ?: (GreenPreviewFixtures.DATE+172_800_000).takeIf { level+1==99 },lastWateredAt=GreenPreviewFixtures.DATE+172_800_000,investedDrops=plant.investedDrops+cost)
                            state=state.copy(drops=state.drops-cost,specimens=state.specimens.map { if(it.id==id) grown else it },action=GreenActionEntity(token,"plant_watered",id,id,level,level+1,cost,state.drops-cost,GreenPreviewFixtures.DATE))
                        },onDismissFeedback={state=state.copy(action=null,messageRes=null)},initialEnvironment=environment,
                            initialSection=GreenSection.entries.firstOrNull { it.name.equals(scenario,true) } ?: GreenSection.WORLD,
                            initialSpecimenId = milestonePlant?.id,
                            initialSeedId = GreenCatalogue.inEnvironment(environment).first().id.takeIf { scenario == "insufficient" })
                    }
                    }
                }
            }
            }
        }
    }
}
