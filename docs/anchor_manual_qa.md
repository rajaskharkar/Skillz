# Anchor Manual QA Checklist

1. Install over an existing app database; verify existing data remains.
2. Start an active Flow before upgrade; after upgrade the StoryScreen active Flow hero remains visible.
3. Start a new Flow, return to StoryScreen, and verify the active Flow hero is visible.
4. Verify the HelpScreen Anchor control center and global Anchor toggle are visible in every setup state.
5. Add Instagram from Common Distractions even if it is not in Recently Detected Apps.
6. Add WhatsApp from Common Distractions.
7. Add Reddit from Common Distractions.
8. In Guide Mode, open Reddit during an active Flow and verify Scyra posts an Anchor nudge and counts one drift episode.
9. Verify Guide Mode copy clearly says it nudges only and does not block apps.
10. Verify Guard Mode is labeled coming soon/unavailable and does not imply app blocking in this PR.
11. Verify phone, SMS, alarms, Settings, launcher, and other system-critical apps never trigger Anchor.
12. End the Flow and verify Anchor detection stops.
13. Verify Anchor does not change Scyra Points, Pearls, Surge, Arc, or Soft Flow scoring.
