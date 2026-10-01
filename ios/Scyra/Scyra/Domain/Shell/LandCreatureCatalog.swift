import Foundation

enum LandStillwaterHabitat: String, CaseIterable, Sendable {
    case pasture, glade, oasis, ravine, sanctuary
    var zone: ShellDepthTier {
        switch self {
        case .pasture: .goldenFields
        case .glade: .ancientWoods
        case .oasis: .openSands
        case .ravine: .highPeaks
        case .sanctuary: .greatWild
        }
    }
    var dropCost: Int64 {
        switch self {
        case .pasture: 15_000
        case .glade: 25_000
        case .oasis: 45_000
        case .ravine: 60_000
        case .sanctuary: 75_000
        }
    }
}

/// Canonical September 2026 roster from Android LandCreatureCatalog.
/// Acquisition requirements and Pearl economic value deliberately remain separate.
enum LandCreatureCatalog {
    static let all: [CreatureDefinition] = [
        creature("creature_chicken", "Chicken", .goldenFields, .arcEarned, flows: 3, growthBase: 720),
        creature("creature_duck", "Duck", .goldenFields, .beyondBlue, minutes: 120),
        creature("creature_turkey", "Turkey", .goldenFields, .beyondBlue, minutes: 150),
        creature("creature_goat", "Goat", .goldenFields, .beyondBlue, minutes: 180),
        creature("creature_sheep", "Sheep", .goldenFields, .beyondBlue, minutes: 240),
        creature("creature_pig", "Pig", .goldenFields, .beyondBlue, minutes: 300),
        creature("creature_alpaca", "Alpaca", .goldenFields, .beyondBlue, minutes: 420),
        creature("creature_llama", "Llama", .goldenFields, .beyondBlue, minutes: 480),
        creature("creature_donkey", "Donkey", .goldenFields, .beyondBlue, minutes: 540),
        creature("creature_cow", "Cow", .goldenFields, .beyondBlue, minutes: 600),
        creature("creature_horse", "Horse", .goldenFields, .beyondBlue, minutes: 720),
        creature("creature_deer", "Deer", .ancientWoods, .arcEarned, flows: 6, growthBase: 1_200),
        creature("creature_squirrel", "Squirrel", .ancientWoods, .beyondBlue, minutes: 240),
        creature("creature_rabbit", "Rabbit", .ancientWoods, .beyondBlue, minutes: 300),
        creature("creature_raccoon", "Raccoon", .ancientWoods, .beyondBlue, minutes: 420),
        creature("creature_fox", "Fox", .ancientWoods, .beyondBlue, minutes: 540),
        creature("creature_porcupine", "Porcupine", .ancientWoods, .beyondBlue, minutes: 600),
        creature("creature_wild_boar", "Wild Boar", .ancientWoods, .beyondBlue, minutes: 660),
        creature("creature_badger", "Badger", .ancientWoods, .beyondBlue, minutes: 780),
        creature("creature_red_panda", "Red Panda", .ancientWoods, .beyondBlue, minutes: 900),
        creature("creature_chimpanzee", "Chimpanzee", .ancientWoods, .beyondBlue, minutes: 960),
        creature("creature_orangutan", "Orangutan", .ancientWoods, .beyondBlue, minutes: 1080),
        creature("creature_bobcat", "Bobcat", .ancientWoods, .beyondBlue, minutes: 1140),
        creature("creature_black_bear", "Black Bear", .ancientWoods, .beyondBlue, minutes: 1200),
        creature("creature_camel", "Camel", .openSands, .arcEarned, flows: 9, growthBase: 1_500),
        creature("creature_meerkat", "Meerkat", .openSands, .beyondBlue, minutes: 420),
        creature("creature_desert_tortoise", "Desert Tortoise", .openSands, .beyondBlue, minutes: 540),
        creature("creature_roadrunner", "Roadrunner", .openSands, .beyondBlue, minutes: 600),
        creature("creature_fennec_fox", "Fennec Fox", .openSands, .beyondBlue, minutes: 720),
        creature("creature_armadillo", "Armadillo", .openSands, .beyondBlue, minutes: 840),
        creature("creature_scorpion", "Scorpion", .openSands, .beyondBlue, minutes: 960),
        creature("creature_warthog", "Warthog", .openSands, .beyondBlue, minutes: 1020),
        creature("creature_rattlesnake", "Rattlesnake", .openSands, .beyondBlue, minutes: 1080),
        creature("creature_jackal", "Jackal", .openSands, .beyondBlue, minutes: 1140),
        creature("creature_coyote", "Coyote", .openSands, .beyondBlue, minutes: 1200),
        creature("creature_ostrich", "Ostrich", .openSands, .beyondBlue, minutes: 1260),
        creature("creature_vulture", "Vulture", .openSands, .beyondBlue, minutes: 1380),
        creature("creature_monitor_lizard", "Monitor Lizard", .openSands, .beyondBlue, minutes: 1500),
        creature("creature_moose", "Moose", .highPeaks, .arcEarned, flows: 12, growthBase: 2_400),
        creature("creature_marmot", "Marmot", .highPeaks, .beyondBlue, minutes: 600),
        creature("creature_mountain_goat", "Mountain Goat", .highPeaks, .beyondBlue, minutes: 720),
        creature("creature_bighorn_sheep", "Bighorn Sheep", .highPeaks, .beyondBlue, minutes: 840),
        creature("creature_reindeer", "Reindeer", .highPeaks, .beyondBlue, minutes: 960),
        creature("creature_arctic_fox", "Arctic Fox", .highPeaks, .beyondBlue, minutes: 1080),
        creature("creature_yak", "Yak", .highPeaks, .beyondBlue, minutes: 1200),
        creature("creature_musk_ox", "Musk Ox", .highPeaks, .beyondBlue, minutes: 1500),
        creature("creature_snowy_owl", "Snowy Owl", .highPeaks, .beyondBlue, minutes: 1800),
        creature("creature_eagle", "Eagle", .highPeaks, .beyondBlue, minutes: 1950),
        creature("creature_snow_leopard", "Snow Leopard", .highPeaks, .beyondBlue, minutes: 2100),
        creature("creature_polar_bear", "Polar Bear", .highPeaks, .beyondBlue, minutes: 2400),
        creature("creature_tiger", "Tiger", .greatWild, .arcEarned, flows: 15, growthBase: 3_000),
        creature("creature_spotted_hyena", "Spotted Hyena", .greatWild, .beyondBlue, minutes: 900),
        creature("creature_gray_wolf", "Gray Wolf", .greatWild, .beyondBlue, minutes: 1080),
        creature("creature_cheetah", "Cheetah", .greatWild, .beyondBlue, minutes: 1200),
        creature("creature_leopard", "Leopard", .greatWild, .beyondBlue, minutes: 1500),
        creature("creature_cougar", "Cougar", .greatWild, .beyondBlue, minutes: 1650),
        creature("creature_jaguar", "Jaguar", .greatWild, .beyondBlue, minutes: 1800),
        creature("creature_crocodile", "Crocodile", .greatWild, .beyondBlue, minutes: 2100),
        creature("creature_komodo_dragon", "Komodo Dragon", .greatWild, .beyondBlue, minutes: 2250),
        creature("creature_baboon", "Baboon", .greatWild, .beyondBlue, minutes: 2325),
        creature("creature_gorilla", "Gorilla", .greatWild, .beyondBlue, minutes: 2400),
        creature("creature_grizzly_bear", "Grizzly Bear", .greatWild, .beyondBlue, minutes: 2700),
        creature("creature_lion", "Lion", .greatWild, .beyondBlue, minutes: 3000),
        creature("creature_peacock", "Peacock", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_goose", "Goose", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_pheasant", "Pheasant", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_emu", "Emu", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_capybara", "Capybara", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_quail", "Quail", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_partridge", "Partridge", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_wallaby", "Wallaby", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_kangaroo", "Kangaroo", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_wombat", "Wombat", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_echidna", "Echidna", .goldenFields, .restorativeLand, habitat: .pasture),
        creature("creature_hedgehog", "Hedgehog", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_sloth", "Sloth", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_pangolin", "Pangolin", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_kinkajou", "Kinkajou", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_lemur", "Lemur", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_tapir", "Tapir", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_okapi", "Okapi", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_panda", "Panda", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_koala", "Koala", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_mandrill", "Mandrill", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_gibbon", "Gibbon", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_platypus", "Platypus", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_chameleon", "Chameleon", .ancientWoods, .restorativeLand, habitat: .glade),
        creature("creature_jerboa", "Jerboa", .openSands, .restorativeLand, habitat: .oasis),
        creature("creature_oryx", "Oryx", .openSands, .restorativeLand, habitat: .oasis),
        creature("creature_gazelle", "Gazelle", .openSands, .restorativeLand, habitat: .oasis),
        creature("creature_caracal", "Caracal", .openSands, .restorativeLand, habitat: .oasis),
        creature("creature_gerenuk", "Gerenuk", .openSands, .restorativeLand, habitat: .oasis),
        creature("creature_addax", "Addax", .openSands, .restorativeLand, habitat: .oasis),
        creature("creature_serval", "Serval", .openSands, .restorativeLand, habitat: .oasis),
        creature("creature_mongoose", "Mongoose", .openSands, .restorativeLand, habitat: .oasis),
        creature("creature_iguana", "Iguana", .openSands, .restorativeLand, habitat: .oasis),
        creature("creature_pika", "Pika", .highPeaks, .restorativeLand, habitat: .ravine),
        creature("creature_ibex", "Ibex", .highPeaks, .restorativeLand, habitat: .ravine),
        creature("creature_chamois", "Chamois", .highPeaks, .restorativeLand, habitat: .ravine),
        creature("creature_vicuna", "Vicuna", .highPeaks, .restorativeLand, habitat: .ravine),
        creature("creature_takin", "Takin", .highPeaks, .restorativeLand, habitat: .ravine),
        creature("creature_tahr", "Tahr", .highPeaks, .restorativeLand, habitat: .ravine),
        creature("creature_macaque", "Macaque", .highPeaks, .restorativeLand, habitat: .ravine),
        creature("creature_wolverine", "Wolverine", .highPeaks, .restorativeLand, habitat: .ravine),
        creature("creature_elephant", "Elephant", .greatWild, .restorativeLand, habitat: .sanctuary),
        creature("creature_rhinoceros", "Rhinoceros", .greatWild, .restorativeLand, habitat: .sanctuary),
        creature("creature_giraffe", "Giraffe", .greatWild, .restorativeLand, habitat: .sanctuary),
        creature("creature_bison", "Bison", .greatWild, .restorativeLand, habitat: .sanctuary),
        creature("creature_hippopotamus", "Hippopotamus", .greatWild, .restorativeLand, habitat: .sanctuary),
        creature("creature_anteater", "Anteater", .greatWild, .restorativeLand, habitat: .sanctuary),
        creature("creature_zebra", "Zebra", .greatWild, .restorativeLand, habitat: .sanctuary),
        creature("creature_lynx", "Lynx", .greatWild, .restorativeLand, habitat: .sanctuary),
        creature("creature_wildebeest", "Wildebeest", .greatWild, .restorativeLand, habitat: .sanctuary),
        creature("creature_buffalo", "Buffalo", .greatWild, .restorativeLand, habitat: .sanctuary),
    ]

    private static func creature(
        _ id: String, _ name: String, _ zone: ShellDepthTier, _ source: CreatureSourceType,
        minutes: Int? = nil, flows: Int? = nil, growthBase: Int? = nil,
        habitat: LandStillwaterHabitat? = nil
    ) -> CreatureDefinition {
        let flagshipValues = ["creature_chicken": 30, "creature_deer": 90, "creature_camel": 180, "creature_moose": 360, "creature_tiger": 720]
        return .init(
            creatureID: id, displayName: name, zone: zone, sourceType: source,
            flowTimeValueMinutes: nil, requirementMinutes: minutes, systemImage: "pawprint.fill",
            participatesInCollector: true, participatesInCompletionist: true,
            secretUntilDiscovered: false, rosterVersion: 1,
            arcFlowRequirement: flows, restorativeHabitat: habitat,
            economicValuePearls: flagshipValues[id] ?? habitat.map { Int($0.dropCost / 60) * 2 },
            baseGrowthCostPearls: growthBase
        )
    }
}

