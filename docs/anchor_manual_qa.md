# Anchor Manual QA Checklist

## Guide Mode
1. Add Instagram from Common Distractions or Installed Apps.
2. Add Reddit from Common Distractions or Installed Apps.
3. Add WhatsApp from Common Distractions or Installed Apps.
4. Grant Usage Access.
5. Enable notifications.
6. Select Guide Mode.
7. Start a Flow.
8. Let the timer run for more than 1 second.
9. Enable Anchor for this Flow.
10. Open Instagram.
11. Verify the Anchor nudge appears within 1–2 seconds.
12. Verify the nudge is not overwritten by the persistent Flow notification.
13. Verify the Return to Flow action opens the active Flow.
14. Repeat the nudge test for Reddit.
15. Repeat the nudge test for WhatsApp.
16. Disable notifications and verify Scyra shows an in-app pending return/warning instead of silently failing.

## Guard Mode
1. Select Guard Mode.
2. If Accessibility is disabled, verify setup is shown.
3. Enable the Scyra Anchor Guard Accessibility service.
4. Start a Flow.
5. Let the timer run for more than 1 second.
6. Enable Anchor for this Flow.
7. Open Instagram.
8. Verify Guard Mode actively exits/returns away from Instagram.
9. Repeat Guard Mode for Reddit.
10. Repeat Guard Mode for WhatsApp.
11. Open Settings and verify it is not guarded.
12. Open Accessibility Settings and verify it is not guarded.
13. Open Phone and verify it is not guarded.
14. Open launcher/home and verify it is not guarded.
15. Pause Flow and verify Guard stops.
16. Reset Flow and verify Guard stops.
17. End Flow and verify Guard stops.
18. Select a non-curated installed launchable app using Installed Apps search and guard it.

## StoryScreen hero
1. No Flow → no hero.
2. Flow screen open but timer 0 → no hero.
3. Timer > 1 sec → hero visible.
4. Pause after elapsed → hero visible.
5. Reset to 0 → hero hidden.
6. Anchor enabled but timer 0 → hero hidden.
7. Anchor enabled and timer > 0 → hero visible.
8. Reset while Anchor enabled → hero hidden and Anchor stops.

## Regression checks
1. HelpScreen Anchor section always visible.
2. HelpScreen global Anchor toggle always visible.
3. Usage Access missing → FlowScreen Anchor tap opens setup sheet.
4. No apps selected → FlowScreen Anchor tap opens Manage Anchor Apps sheet.
5. Setup complete → FlowScreen Anchor toggles On.
6. Tap again → FlowScreen Anchor toggles Off.
7. Ending Flow stops Anchor detection.
8. Anchor does not change Scyra Points, Pearls, Surge, Arc, Soft Flow, or Shell rewards.
9. Existing Search and Surge controls still work.
10. Upgrade from previous build with existing data. Data remains.

## Permission and privacy notes for PR review
- Guide Mode uses Usage Access and notifications to detect selected apps and nudge the user back; it does not block apps.
- Guard Mode uses the Scyra Anchor Guard Accessibility service to observe package/window changes only and perform Back/Home when a selected guarded app opens during a meaningful active Flow.
- `QUERY_ALL_PACKAGES` is intentionally used because user-selected app guarding and reliable installed-app discovery are core Anchor functionality. Installed app lists are shown locally for setup and are not uploaded.
- Anchor does not read messages, screen text, keystrokes, photos, camera, microphone, browsing history, or private app content.
- Play release readiness: the production release must include the Play Console declaration and policy rationale for broad package visibility and Accessibility use.
