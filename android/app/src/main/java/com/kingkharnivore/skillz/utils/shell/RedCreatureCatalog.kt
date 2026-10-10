package com.kingkharnivore.skillz.utils.shell

import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.*

enum class RedEra(val titleRes: Int, val zone: CreatureZone) {
    TRIASSIC(R.string.red_triassic, CreatureZone.TRIASSIC),
    JURASSIC(R.string.red_jurassic, CreatureZone.JURASSIC),
    CRETACEOUS(R.string.red_cretaceous, CreatureZone.CRETACEOUS)
}

data class RedCatalogEntry(val id: String, val nameRes: Int, val era: RedEra, val pebbleCost: Int, val catalogOrder: Int, val assetId: String = id, val active: Boolean = true) {
    val definition get() = CreatureDefinition(
        creatureId = id, displayName = id, zone = era.zone, sourceType = CreatureSourceType.RED_PURCHASE,
        staticIconKey = assetId, animatedRendererKey = assetId, renderFamily = CreatureRenderFamily.DINOSAUR,
        sceneBehavior = if (id in RedCreatureCatalog.predators) CreatureSceneBehavior.PROWL else CreatureSceneBehavior.GRAZE,
        placementBand = CreaturePlacementBand.GROUND, scaleClass = if (pebbleCost >= 2000) CreatureScaleClass.GIANT else CreatureScaleClass.MEDIUM,
        economicValuePearls = pebbleCost, baseGrowthCostPearls = pebbleCost, pebbleCost = pebbleCost
    )
}

/** V1 roster and prices are specified in 08_RED_CREATURE_ECONOMY.md. */
object RedCreatureCatalog {
    val entries = listOf(
        RedCatalogEntry("triassic_saturnalia", R.string.red_triassic_saturnalia, RedEra.TRIASSIC, 180, 0),
        RedCatalogEntry("triassic_thecodontosaurus", R.string.red_triassic_thecodontosaurus, RedEra.TRIASSIC, 220, 1),
        RedCatalogEntry("triassic_staurikosaurus", R.string.red_triassic_staurikosaurus, RedEra.TRIASSIC, 280, 2),
        RedCatalogEntry("triassic_chindesaurus", R.string.red_triassic_chindesaurus, RedEra.TRIASSIC, 320, 3),
        RedCatalogEntry("triassic_eoraptor", R.string.red_triassic_eoraptor, RedEra.TRIASSIC, 380, 4),
        RedCatalogEntry("triassic_guaibasaurus", R.string.red_triassic_guaibasaurus, RedEra.TRIASSIC, 420, 5),
        RedCatalogEntry("triassic_coelophysis", R.string.red_triassic_coelophysis, RedEra.TRIASSIC, 500, 6),
        RedCatalogEntry("triassic_coloradisaurus", R.string.red_triassic_coloradisaurus, RedEra.TRIASSIC, 580, 7),
        RedCatalogEntry("triassic_mussaurus", R.string.red_triassic_mussaurus, RedEra.TRIASSIC, 700, 8),
        RedCatalogEntry("triassic_liliensternus", R.string.red_triassic_liliensternus, RedEra.TRIASSIC, 780, 9),
        RedCatalogEntry("triassic_melanorosaurus", R.string.red_triassic_melanorosaurus, RedEra.TRIASSIC, 900, 10),
        RedCatalogEntry("triassic_riojasaurus", R.string.red_triassic_riojasaurus, RedEra.TRIASSIC, 1050, 11),
        RedCatalogEntry("triassic_plateosaurus", R.string.red_triassic_plateosaurus, RedEra.TRIASSIC, 1250, 12),
        RedCatalogEntry("triassic_herrerasaurus", R.string.red_triassic_herrerasaurus, RedEra.TRIASSIC, 1500, 13),
        RedCatalogEntry("jurassic_heterodontosaurus", R.string.red_jurassic_heterodontosaurus, RedEra.JURASSIC, 420, 14),
        RedCatalogEntry("jurassic_compsognathus", R.string.red_jurassic_compsognathus, RedEra.JURASSIC, 500, 15),
        RedCatalogEntry("jurassic_massospondylus", R.string.red_jurassic_massospondylus, RedEra.JURASSIC, 600, 16),
        RedCatalogEntry("jurassic_dryosaurus", R.string.red_jurassic_dryosaurus, RedEra.JURASSIC, 650, 17),
        RedCatalogEntry("jurassic_camptosaurus", R.string.red_jurassic_camptosaurus, RedEra.JURASSIC, 750, 18),
        RedCatalogEntry("jurassic_archaeopteryx", R.string.red_jurassic_archaeopteryx, RedEra.JURASSIC, 900, 19),
        RedCatalogEntry("jurassic_dilophosaurus", R.string.red_jurassic_dilophosaurus, RedEra.JURASSIC, 1050, 20),
        RedCatalogEntry("jurassic_kentrosaurus", R.string.red_jurassic_kentrosaurus, RedEra.JURASSIC, 1150, 21),
        RedCatalogEntry("jurassic_megalosaurus", R.string.red_jurassic_megalosaurus, RedEra.JURASSIC, 1300, 22),
        RedCatalogEntry("jurassic_cryolophosaurus", R.string.red_jurassic_cryolophosaurus, RedEra.JURASSIC, 1400, 23),
        RedCatalogEntry("jurassic_ceratosaurus", R.string.red_jurassic_ceratosaurus, RedEra.JURASSIC, 1500, 24),
        RedCatalogEntry("jurassic_yangchuanosaurus", R.string.red_jurassic_yangchuanosaurus, RedEra.JURASSIC, 1650, 25),
        RedCatalogEntry("jurassic_camarasaurus", R.string.red_jurassic_camarasaurus, RedEra.JURASSIC, 1750, 26),
        RedCatalogEntry("jurassic_torvosaurus", R.string.red_jurassic_torvosaurus, RedEra.JURASSIC, 1950, 27),
        RedCatalogEntry("jurassic_mamenchisaurus", R.string.red_jurassic_mamenchisaurus, RedEra.JURASSIC, 2150, 28),
        RedCatalogEntry("jurassic_apatosaurus", R.string.red_jurassic_apatosaurus, RedEra.JURASSIC, 2300, 29),
        RedCatalogEntry("jurassic_diplodocus", R.string.red_jurassic_diplodocus, RedEra.JURASSIC, 2500, 30),
        RedCatalogEntry("jurassic_giraffatitan", R.string.red_jurassic_giraffatitan, RedEra.JURASSIC, 2700, 31),
        RedCatalogEntry("jurassic_stegosaurus", R.string.red_jurassic_stegosaurus, RedEra.JURASSIC, 3000, 32),
        RedCatalogEntry("jurassic_allosaurus", R.string.red_jurassic_allosaurus, RedEra.JURASSIC, 3300, 33),
        RedCatalogEntry("jurassic_brachiosaurus", R.string.red_jurassic_brachiosaurus, RedEra.JURASSIC, 3600, 34),
        RedCatalogEntry("cretaceous_psittacosaurus", R.string.red_cretaceous_psittacosaurus, RedEra.CRETACEOUS, 700, 35),
        RedCatalogEntry("cretaceous_microraptor", R.string.red_cretaceous_microraptor, RedEra.CRETACEOUS, 850, 36),
        RedCatalogEntry("cretaceous_protoceratops", R.string.red_cretaceous_protoceratops, RedEra.CRETACEOUS, 950, 37),
        RedCatalogEntry("cretaceous_oviraptor", R.string.red_cretaceous_oviraptor, RedEra.CRETACEOUS, 1050, 38),
        RedCatalogEntry("cretaceous_nigersaurus", R.string.red_cretaceous_nigersaurus, RedEra.CRETACEOUS, 1150, 39),
        RedCatalogEntry("cretaceous_gallimimus", R.string.red_cretaceous_gallimimus, RedEra.CRETACEOUS, 1250, 40),
        RedCatalogEntry("cretaceous_iguanodon", R.string.red_cretaceous_iguanodon, RedEra.CRETACEOUS, 1400, 41),
        RedCatalogEntry("cretaceous_deinonychus", R.string.red_cretaceous_deinonychus, RedEra.CRETACEOUS, 1600, 42),
        RedCatalogEntry("cretaceous_pachycephalosaurus", R.string.red_cretaceous_pachycephalosaurus, RedEra.CRETACEOUS, 1700, 43),
        RedCatalogEntry("cretaceous_corythosaurus", R.string.red_cretaceous_corythosaurus, RedEra.CRETACEOUS, 1750, 44),
        RedCatalogEntry("cretaceous_utahraptor", R.string.red_cretaceous_utahraptor, RedEra.CRETACEOUS, 1900, 45),
        RedCatalogEntry("cretaceous_parasaurolophus", R.string.red_cretaceous_parasaurolophus, RedEra.CRETACEOUS, 2050, 46),
        RedCatalogEntry("cretaceous_baryonyx", R.string.red_cretaceous_baryonyx, RedEra.CRETACEOUS, 2200, 47),
        RedCatalogEntry("cretaceous_yutyrannus", R.string.red_cretaceous_yutyrannus, RedEra.CRETACEOUS, 2300, 48),
        RedCatalogEntry("cretaceous_styracosaurus", R.string.red_cretaceous_styracosaurus, RedEra.CRETACEOUS, 2400, 49),
        RedCatalogEntry("cretaceous_edmontosaurus", R.string.red_cretaceous_edmontosaurus, RedEra.CRETACEOUS, 2500, 50),
        RedCatalogEntry("cretaceous_suchomimus", R.string.red_cretaceous_suchomimus, RedEra.CRETACEOUS, 2600, 51),
        RedCatalogEntry("cretaceous_carnotaurus", R.string.red_cretaceous_carnotaurus, RedEra.CRETACEOUS, 2800, 52),
        RedCatalogEntry("cretaceous_therizinosaurus", R.string.red_cretaceous_therizinosaurus, RedEra.CRETACEOUS, 3000, 53),
        RedCatalogEntry("cretaceous_acrocanthosaurus", R.string.red_cretaceous_acrocanthosaurus, RedEra.CRETACEOUS, 3200, 54),
        RedCatalogEntry("cretaceous_velociraptor", R.string.red_cretaceous_velociraptor, RedEra.CRETACEOUS, 3400, 55),
        RedCatalogEntry("cretaceous_rajasaurus", R.string.red_cretaceous_rajasaurus, RedEra.CRETACEOUS, 3500, 56),
        RedCatalogEntry("cretaceous_deinocheirus", R.string.red_cretaceous_deinocheirus, RedEra.CRETACEOUS, 3600, 57),
        RedCatalogEntry("cretaceous_pachyrhinosaurus", R.string.red_cretaceous_pachyrhinosaurus, RedEra.CRETACEOUS, 3700, 58),
        RedCatalogEntry("cretaceous_ankylosaurus", R.string.red_cretaceous_ankylosaurus, RedEra.CRETACEOUS, 4000, 59),
        RedCatalogEntry("cretaceous_majungasaurus", R.string.red_cretaceous_majungasaurus, RedEra.CRETACEOUS, 4100, 60),
        RedCatalogEntry("cretaceous_tarbosaurus", R.string.red_cretaceous_tarbosaurus, RedEra.CRETACEOUS, 4300, 61),
        RedCatalogEntry("cretaceous_carcharodontosaurus", R.string.red_cretaceous_carcharodontosaurus, RedEra.CRETACEOUS, 4600, 62),
        RedCatalogEntry("cretaceous_triceratops", R.string.red_cretaceous_triceratops, RedEra.CRETACEOUS, 5000, 63),
        RedCatalogEntry("cretaceous_patagotitan", R.string.red_cretaceous_patagotitan, RedEra.CRETACEOUS, 5400, 64),
        RedCatalogEntry("cretaceous_argentinosaurus", R.string.red_cretaceous_argentinosaurus, RedEra.CRETACEOUS, 5700, 65),
        RedCatalogEntry("cretaceous_giganotosaurus", R.string.red_cretaceous_giganotosaurus, RedEra.CRETACEOUS, 6000, 66),
        RedCatalogEntry("cretaceous_spinosaurus", R.string.red_cretaceous_spinosaurus, RedEra.CRETACEOUS, 6400, 67),
        RedCatalogEntry("cretaceous_tyrannosaurus", R.string.red_cretaceous_tyrannosaurus, RedEra.CRETACEOUS, 7000, 68),
    )
    val byId = entries.associateBy { it.id }
    val predators = setOf(
        "triassic_staurikosaurus", "triassic_chindesaurus", "triassic_eoraptor", "triassic_guaibasaurus",
        "triassic_coelophysis", "triassic_liliensternus", "triassic_herrerasaurus",
        "jurassic_compsognathus", "jurassic_dilophosaurus", "jurassic_megalosaurus", "jurassic_cryolophosaurus",
        "jurassic_ceratosaurus", "jurassic_yangchuanosaurus", "jurassic_torvosaurus", "jurassic_allosaurus",
        "cretaceous_microraptor", "cretaceous_deinonychus", "cretaceous_utahraptor", "cretaceous_baryonyx",
        "cretaceous_yutyrannus", "cretaceous_suchomimus", "cretaceous_carnotaurus", "cretaceous_acrocanthosaurus",
        "cretaceous_velociraptor", "cretaceous_rajasaurus", "cretaceous_majungasaurus", "cretaceous_tarbosaurus",
        "cretaceous_carcharodontosaurus", "cretaceous_giganotosaurus", "cretaceous_spinosaurus", "cretaceous_tyrannosaurus"
    )
    val definitions = entries.map { it.definition }
    val shellFinds = entries.map { entry ->
        ShellFindDefinition(entry.id, entry.nameRes, R.string.red_creature_description, ShellFindCategory.CREATURES,
            ShellRoomId.THE_RED, entry.assetId, entry.assetId, false, true, false, emptySet(), kind = ShellRewardKind.ANIMAL)
    }
}
