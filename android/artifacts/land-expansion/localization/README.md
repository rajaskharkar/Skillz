# Land expansion localization

Scope: Android display text for the Land expansion, its collection/mastery badges, and the shared Badge Book controls used by the expansion. Creature IDs, zone IDs, source IDs, badge IDs, prices, requirements, repositories, DAOs and database schema are unchanged in this localization pass.

## Terminology

| Concept | Hindi | Marathi | Spanish |
|---|---|---|---|
| The Blue | द ब्लू | द ब्लू | The Blue |
| Sea | समुद्र | समुद्र | Mar |
| Land | धरती | भूमी | Tierra |
| Flow | फ़्लो | फ्लो | Flow |
| Arc | आर्क | आर्क | Arc |
| Stillwater | स्टिलवॉटर | स्टिलवॉटर | Stillwater |
| Mastery | महारत | प्रभुत्व | Dominio |
| Golden Fields | सुनहरे मैदान | सोनेरी कुरणे | Praderas doradas |
| Ancient Woods | प्राचीन वन | प्राचीन वने | Bosques ancestrales |
| Open Sands | विस्तृत रेत | खुले वाळवंट | Arenas abiertas |
| High Peaks | ऊँची चोटियाँ | उंच शिखरे | Cumbres altas |
| Great Wild | विशाल वन्य प्रदेश | विस्तीर्ण रान | Naturaleza indómita |
| Pasture | चरागाह | कुरण | Pastizal |
| Glade | वन प्रांगण | वनातील माळ | Claro |
| Oasis | नखलिस्तान | मरूद्यान | Oasis |
| Ravine | तंग घाटी | घळ | Barranco |
| Sanctuary | अभयारण्य | अभयारण्य | Santuario |

The five Stillwater habitats retain their distinct identities and corresponding Land destinations. The Encounter title remains the same named experience in both realms, translated as “द ब्लू के पार मुलाक़ात”, “द ब्लूच्या पलीकडील भेट”, and “Encuentro más allá de The Blue”.

Species use familiar local animal names where appropriate and transliterations for species without a clear everyday equivalent. Distinct species must not collapse into the same translated name (for example, bobcat/lynx, leopard/cheetah, porcupine/hedgehog, bison/buffalo, and reindeer/deer). Latin-script proper product names in Spanish are intentional. This is neutral Spanish, not a country-specific locale.

## Implementation conventions

- UTF-8 Android `values-hi`, `values-mr`, `values-es`, with complete `land_strings.xml` and `land_badge_strings.xml` files. English remains the default fallback.
- Complete messages with numbered, typed placeholders. Never concatenate translated fragments into a sentence or persist translated display labels.
- Count-neutral labels for variable creature and badge counts; Android plurals for the shared badge summary and mastery plural messages.
- Locale-aware names in the Chest and Arc summaries; alphabetical comparison, number formatting and badge dates follow the app resource configuration rather than assuming the device locale.
- Short realm rail labels and separate full accessibility descriptions. The creature tray heading can wrap beside its action.
- Visual QA uses Android per-app locales on the isolated emulator, matching the production locale mechanism for pages and modal sheets. The previous emulator locale is restored afterward. Mock preview data never touches user records.

## Validation

Resource unit tests cover missing translations, formatting arguments, duplicate resources, all 114 unique species names, and accidental use of English catalog display names in the feature UI. Android instrumentation checks packaged resource selection and formatting in all four supported languages.

The shared translated key inventory is in `shared-resource-keys.txt`; its test fixture is `app/src/test/resources/land/localization-shared-keys.txt`. New keys added to either Land resource file are covered automatically by the resource tests.

See the [implementation report](implementation-report.md) for final execution results and the [screenshot index](../screenshots/localization/README.md) for visual QA captures.
