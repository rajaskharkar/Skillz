import fs from "node:fs/promises";
import path from "node:path";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const ROOT = "/Users/rajaskharkar/Documents/software/android/Scyra/Skillz/android";
const BUILD = path.join(ROOT, ".codex-build/scyra-investor");
const ASSETS = path.join(BUILD, "assets");
const OUTPUT = path.join(BUILD, "output");
const RENDERED = path.join(BUILD, "rendered");
const FINAL_PPTX = path.join(ROOT, "Scyra_Investor_Product_Demo.pptx");

const W = 1280;
const H = 720;
const FONT = "Helvetica Neue";
const C = {
  canvas: "#FAF7F0",
  paper: "#FFFFFF",
  ink: "#17383B",
  black: "#111111",
  muted: "#647274",
  panel: "#ECE8DE",
  rule: "#C8C6BD",
  teal: "#3F9694",
  tealSoft: "#D9EEEB",
  ocean: "#185B72",
  oceanSoft: "#DCECF2",
  gold: "#B69B50",
  goldSoft: "#EFE6C8",
  shell: "#E3C8AD",
  plum: "#786C7D",
};

const imageCache = new Map();

async function imageBytes(name) {
  if (!imageCache.has(name)) {
    const bytes = await fs.readFile(path.join(ASSETS, name));
    imageCache.set(
      name,
      bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength),
    );
  }
  return imageCache.get(name);
}

async function writeBlob(filePath, blob) {
  await fs.writeFile(filePath, new Uint8Array(await blob.arrayBuffer()));
}

function addShape(slide, geometry, position, fill, line = { style: "solid", fill: "none", width: 0 }, extra = {}) {
  return slide.shapes.add({ geometry, position, fill, line, ...extra });
}

function addText(slide, text, position, style = {}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    position,
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  shape.text = text;
  shape.text.style = {
    fontSize: style.fontSize ?? 20,
    bold: style.bold ?? false,
    color: style.color ?? C.ink,
    alignment: style.alignment ?? "left",
    verticalAlignment: style.verticalAlignment ?? "top",
    typeface: style.typeface ?? FONT,
  };
  return shape;
}

function addRule(slide, x, y, width, fill = C.rule, weight = 1) {
  return addShape(
    slide,
    "line",
    { left: x, top: y, width, height: 0 },
    "none",
    { style: "solid", fill, width: weight },
  );
}

function slideBase(presentation, section, number, title, subtitle = "") {
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addText(slide, `${String(number).padStart(2, "0")}  /  ${section.toUpperCase()}`, { left: 52, top: 34, width: 430, height: 24 }, { fontSize: 13, bold: true, color: C.teal });
  addText(slide, title, { left: 52, top: 69, width: 1176, height: 62 }, { fontSize: 40, bold: true, color: C.black });
  if (subtitle) {
    addText(slide, subtitle, { left: 54, top: 128, width: 1120, height: 42 }, { fontSize: 18, color: C.muted });
  }
  addRule(slide, 52, 178, 1176, C.rule, 1);
  return slide;
}

function addNotes(slide, talkTrack, source = "Scyra Android app, locally captured on an Android emulator from the Scyra Debug build, 2026-08-19.") {
  slide.speakerNotes.textFrame.setText(`${talkTrack}\n\n[Sources]\n- ${source}\n[/Sources]`);
  slide.speakerNotes.setVisible(true);
}

async function addImage(slide, name, position, { fit = "cover", alt = name, radius = 22 } = {}) {
  return slide.images.add({
    blob: await imageBytes(name),
    contentType: "image/png",
    alt,
    fit,
    geometry: "roundRect",
    borderRadius: radius,
    position,
  });
}

async function addPhone(slide, name, x, y, width, label = "", options = {}) {
  const height = width * (2340 / 1080);
  addShape(
    slide,
    "roundRect",
    { left: x - 7, top: y - 7, width: width + 14, height: height + 14 },
    options.frameFill ?? C.ink,
    { style: "solid", fill: options.frameLine ?? C.ink, width: 1 },
    { borderRadius: 32, shadow: options.shadow === false ? "shadow-none" : "shadow-md" },
  );
  await addImage(slide, name, { left: x, top: y, width, height }, { fit: "cover", alt: label || name, radius: 27 });
  if (label) {
    addText(slide, label, { left: x - 12, top: y + height + 17, width: width + 24, height: 30 }, { fontSize: 15, bold: true, alignment: "center", color: options.labelColor ?? C.ink });
  }
  return height;
}

function addFeatureBlock(slide, kicker, title, body, x, y, width, accent = C.teal) {
  addText(slide, kicker.toUpperCase(), { left: x, top: y, width, height: 22 }, { fontSize: 12, bold: true, color: accent });
  addText(slide, title, { left: x, top: y + 30, width, height: 62 }, { fontSize: 27, bold: true, color: C.black });
  addText(slide, body, { left: x, top: y + 96, width, height: 150 }, { fontSize: 18, color: C.muted });
}

function addCallout(slide, number, title, body, x, y, width, accent = C.teal) {
  addText(slide, number, { left: x, top: y, width: 78, height: 44 }, { fontSize: 29, bold: true, color: accent });
  addText(slide, title, { left: x + 86, top: y + 2, width: width - 86, height: 34 }, { fontSize: 21, bold: true, color: C.black });
  addText(slide, body, { left: x + 86, top: y + 43, width: width - 86, height: 80 }, { fontSize: 16, color: C.muted });
}

async function buildDeck() {
  await fs.mkdir(OUTPUT, { recursive: true });
  await fs.mkdir(RENDERED, { recursive: true });
  const presentation = Presentation.create({ slideSize: { width: W, height: H } });

  // 1 — cover (Codex Grid slide 08 adaptation)
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.canvas;
    addText(slide, "SCYRA", { left: 58, top: 50, width: 280, height: 30 }, { fontSize: 15, bold: true, color: C.teal });
    await addImage(slide, "app-icon.png", { left: 58, top: 106, width: 92, height: 92 }, { fit: "cover", alt: "Scyra app icon", radius: 25 });
    addText(slide, "Turn intentional time\ninto a living story", { left: 58, top: 232, width: 545, height: 180 }, { fontSize: 54, bold: true, color: C.black });
    addText(slide, "A complete product walkthrough for investors and stakeholders", { left: 60, top: 438, width: 500, height: 68 }, { fontSize: 22, color: C.muted });
    addText(slide, "Android product demo  •  August 2026", { left: 60, top: 620, width: 450, height: 25 }, { fontSize: 14, bold: true, color: C.teal });
    addShape(slide, "roundRect", { left: 654, top: 36, width: 576, height: 648 }, C.oceanSoft, { style: "solid", fill: C.rule, width: 1 }, { borderRadius: 28 });
    await addPhone(slide, "20-blue-populated.png", 815, 69, 256, "", { shadow: true, frameFill: C.ocean });
    addText(slide, "THE BLUE", { left: 694, top: 615, width: 170, height: 22 }, { fontSize: 13, bold: true, color: C.ocean });
    addText(slide, "Progress becomes a world users can return to.", { left: 694, top: 642, width: 460, height: 30 }, { fontSize: 18, color: C.ink });
    addNotes(slide, "Open with the product promise: Scyra makes focus visible, memorable, and worth returning to.");
  }

  // 2 — product loop (Codex Grid slide 17 adaptation)
  {
    const slide = slideBase(presentation, "Product thesis", 2, "The app connects a complete behavior loop", "Every action creates context for the next session—and long-term progress beyond it.");
    const steps = [
      ["01", "Focus", "Start a scored Flow or a gentle Soft Flow."],
      ["02", "Capture", "Record a Pulse or multimodal Chronicle."],
      ["03", "Review", "See time, score, journeys, Sagas, and Chronicles."],
      ["04", "Plan", "Schedule Flows, build Arcs, or launch a suggested Scene."],
      ["05", "Return", "Grow The Shell, collections, creatures, and badges."],
    ];
    addRule(slide, 92, 325, 1095, C.ink, 2);
    steps.forEach((s, i) => {
      const x = 78 + i * 225;
      addShape(slide, "ellipse", { left: x + 55, top: 311, width: 29, height: 29 }, i === 4 ? C.gold : C.teal, { style: "solid", fill: C.canvas, width: 4 });
      addText(slide, s[0], { left: x, top: 238, width: 140, height: 30 }, { fontSize: 17, bold: true, alignment: "center", color: i === 4 ? C.gold : C.teal });
      addText(slide, s[1], { left: x, top: 372, width: 140, height: 34 }, { fontSize: 23, bold: true, alignment: "center", color: C.black });
      addText(slide, s[2], { left: x - 18, top: 420, width: 176, height: 104 }, { fontSize: 16, alignment: "center", color: C.muted });
    });
    addText(slide, "The differentiated value is the continuity between the tools—not any single timer, journal, or reward mechanic.", { left: 144, top: 603, width: 992, height: 42 }, { fontSize: 20, bold: true, alignment: "center", color: C.ink });
    addNotes(slide, "Frame Scyra as a system. The user moves from intention to evidence, then from evidence to a reason to come back.");
  }

  // 3 — Story
  {
    const slide = slideBase(presentation, "Review", 3, "Story turns effort into a navigable record", "Users can zoom from one day to a month, filter by journey, and move between narrative and trend views.");
    await addPhone(slide, "32-story-current.png", 92, 208, 214, "Story dashboard");
    addFeatureBlock(slide, "One home", "Time and score in context", "The Story view combines session time, Scyra Score, journey tags, and session cards—without forcing users into a separate analytics product.", 370, 224, 360, C.teal);
    addCallout(slide, "A", "Flexible lenses", "Day, Week, Month, previous/next period, and journey filters.", 774, 225, 405, C.ocean);
    addCallout(slide, "B", "Two storytelling modes", "Sagas organize progress; Chronicles preserve the qualitative record.", 774, 372, 405, C.teal);
    addCallout(slide, "C", "Action stays close", "Start a Flow or capture a Pulse directly from the dashboard.", 774, 519, 405, C.gold);
    addNotes(slide, "Use the current-week screenshot to show how quickly a completed session becomes visible and actionable in Story.");
  }

  // 4 — Flow
  {
    const slide = slideBase(presentation, "Focus", 4, "Flow supports intensity without enforcing one mode", "A single session surface covers scored focus, gentle work, targets, live runtime, and continuation.");
    await addPhone(slide, "05-flow-setup.png", 86, 215, 190, "Session setup");
    await addPhone(slide, "27-flow-active.png", 327, 215, 190, "Live Flow");
    addFeatureBlock(slide, "Flexible session", "Flow or Soft Flow", "Choose scored focus or a calmer, unscored mode. Add a title and journey, then run the timer in the foreground or background.", 586, 222, 540, C.teal);
    addCallout(slide, "01", "Surge targets", "Arm a timed challenge with milestone feedback and bonus scoring.", 586, 408, 540, C.gold);
    addCallout(slide, "02", "Stay in the loop", "Record a Pulse during the session, reset safely, or continue an Arc step.", 586, 540, 540, C.ocean);
    addNotes(slide, "Call out that the timer is only the entry point: Scyra adds mode choice, journey context, Surge targets, reflection, and Arc continuation.");
  }

  // 5 — capture
  {
    const slide = slideBase(presentation, "Capture", 5, "Reflection meets users in the medium they already have", "Quick Pulse capture and rich Chronicle capture share the same journey context.");
    await addPhone(slide, "06-chronicle-capture.png", 98, 211, 205, "Chronicle");
    await addPhone(slide, "07-pulse.png", 354, 211, 205, "Pulse");
    addFeatureBlock(slide, "Chronicle", "A multimodal memory layer", "Capture text, dictated or recorded audio, gallery media, a new photo, or video. Media and transcription stay attached to the user’s story.", 632, 225, 525, C.ocean);
    addFeatureBlock(slide, "Pulse", "Low-friction reflection", "Tag a thought to a journey, attach it to the active Flow, and later let Idea Grove turn that reflection into something actionable.", 632, 435, 525, C.teal);
    addNotes(slide, "Contrast Chronicle depth with Pulse speed. Both preserve context, while Pulse also feeds the idea workflow.");
  }

  // 6 — rewards
  {
    const slide = slideBase(presentation, "Reward", 6, "Completion closes the loop immediately", "The user sees what was logged, what was earned, and how that progress changes The Shell.");
    await addPhone(slide, "30-reward-reveal.png", 92, 210, 205, "Points & Pearls");
    await addPhone(slide, "31-shell-reward.png", 348, 210, 205, "Shell impact");
    addFeatureBlock(slide, "Reward reveal", "Effort becomes a portable resource", "Scyra Points are explained—not just displayed—and carried into The Shell as Pearls. The carousel can also reveal creatures and Arc outcomes.", 622, 231, 535, C.gold);
    addCallout(slide, "→", "No dead end", "Done returns the user to their story; Enter The Shell continues into the retention layer.", 622, 474, 535, C.teal);
    addNotes(slide, "Swipe the live reward carousel during the demo. The investor point is that completion creates both evidence and a next action.");
  }

  // 7 — Paths
  {
    const slide = slideBase(presentation, "Plan", 7, "Paths turns intention into a visible horizon", "Plan a single Flow, organize a multi-step Arc, or begin from a suggested Scene.");
    await addPhone(slide, "01-paths.png", 94, 214, 198, "Planned Flows");
    await addPhone(slide, "02-paths-arcs.png", 346, 214, 198, "Arcs");
    addFeatureBlock(slide, "Horizon", "Two planning scales", "Flows support the next intentional block. Arcs link multiple steps into a reusable route with a clear beginning and continuation state.", 620, 226, 530, C.teal);
    addCallout(slide, "01", "Create from scratch", "Plan a Flow or build an Arc around a goal.", 620, 448, 530, C.ocean);
    addCallout(slide, "02", "Borrow a pattern", "Suggested Scenes provide launch-ready structures for common focus modes.", 620, 560, 530, C.gold);
    addNotes(slide, "Show the Flows and Arcs tabs as the bridge between today’s focus and a repeatable longer-term plan.");
  }

  // 8 — Arc studio
  {
    const slide = slideBase(presentation, "Plan", 8, "The Studio makes multi-step work feel authored", "A five-step builder creates an Arc; suggested routes can be saved or launched immediately.");
    await addPhone(slide, "03-create-arc.png", 88, 211, 205, "Five-step Studio");
    await addPhone(slide, "04-suggested-scene-detail.png", 368, 211, 205, "Suggested Scene");
    addFeatureBlock(slide, "Your Studio", "Name, assemble, and tune the route", "The wizard lowers the cost of structuring a sequence while preserving per-step modes, timing, and Surge choices.", 620, 230, 530, C.teal);
    addFeatureBlock(slide, "Scenes", "Templates remain editable", "Preview a route such as Deep Work Launch, inspect the steps and estimated duration, then Save as Arc or Begin Arc.", 620, 438, 530, C.ocean);
    addNotes(slide, "Use this slide to position templates as scaffolding, not locked programs: users can save and adapt the route.");
  }

  // 9 — tools/settings
  {
    const slide = slideBase(presentation, "Personalize", 9, "Power tools stay close, but out of the way", "ScratchPad supports deep capture; settings tune motivation, calm, language, and movement.");
    await addPhone(slide, "08-notepad.png", 91, 211, 205, "ScratchPad");
    await addPhone(slide, "09-help.png", 349, 211, 205, "Help & settings");
    addFeatureBlock(slide, "ScratchPad", "A real writing surface", "Headings, type styles, bold, italic, underline, strike, sub/superscript, search, undo/redo, and top/bottom navigation.", 624, 222, 520, C.plum);
    addFeatureBlock(slide, "Settings", "Motivation can be calibrated", "Show or hide score, enable Calm Mode, change language, connect Health Connect, and earn a movement bonus during eligible Flows.", 624, 438, 520, C.teal);
    addNotes(slide, "Call out that Scyra can be tuned for users who want numbers and for users who want a calmer experience.");
  }

  // 10 — Shell and Lookout
  {
    const slide = slideBase(presentation, "Return", 10, "The Shell gives every session somewhere to land", "A persistent home, Pearl economy, objectives, and notifications turn isolated sessions into continuity.");
    await addPhone(slide, "19-shell-populated.png", 91, 211, 205, "The Shell");
    await addPhone(slide, "11-lookout.png", 349, 211, 205, "The Lookout");
    addFeatureBlock(slide, "The Shell", "The retention hub", "Pearls, collections, Heart, The Blue, Chest, badges, and notification inlays live in one navigable place.", 624, 224, 520, C.gold);
    addFeatureBlock(slide, "The Lookout", "Objectives without streak anxiety", "Daily, weekly, and monthly goals create multiple horizons, each with clear Pearl rewards.", 624, 438, 520, C.teal);
    addNotes(slide, "Explain the transition from reward reveal into The Shell, then open The Lookout to show concrete return prompts.");
  }

  // 11 — Voyage and ideas
  {
    const slide = slideBase(presentation, "Return", 11, "Progress can be measured—or turned into the next idea", "Voyage Hall serves the analytical user; Idea Grove serves the exploratory user.");
    await addPhone(slide, "12-voyage.png", 91, 211, 205, "Voyage Hall");
    await addPhone(slide, "13-idea-grove.png", 349, 211, 205, "Idea Grove");
    addFeatureBlock(slide, "Voyage Hall", "Stats, records, and streaks", "Review totals, bests, core progress, bonus contribution, and repeat behavior in a dedicated long-view space.", 624, 224, 520, C.ocean);
    addFeatureBlock(slide, "Idea Grove", "Thoughts become launch points", "Pulse-derived ideas can be alive or completed, then launch a Flow when the user is ready to act.", 624, 438, 520, C.teal);
    addNotes(slide, "This is a useful investor contrast: the same underlying activity supports analytical and generative motivations.");
  }

  // 12 — Focus room and Stillwater
  {
    const slide = slideBase(presentation, "Return", 12, "The app also rewards recovery and gentler momentum", "Guided focus exercises and Soft Flow collection progress make the system broader than high-intensity productivity.");
    await addPhone(slide, "14-focus-room.png", 91, 211, 205, "Focus Room");
    await addPhone(slide, "25-stillwater-populated.png", 349, 211, 205, "Stillwater");
    addFeatureBlock(slide, "Focus Room", "Guided regulation", "Voice-supported exercises include grounding and Box Breathing, giving users a deliberate on-ramp before or between demanding sessions.", 624, 224, 520, C.ocean);
    addFeatureBlock(slide, "Stillwater", "Soft work earns its own ecology", "Soft Flow Drops fill vessels and unlock exclusive creatures—recognizing restorative effort without turning it into score competition.", 624, 438, 520, C.teal);
    addNotes(slide, "Show that Scyra’s motivation system includes recovery and calm, not only more output.");
  }

  // 13 — The Blue
  {
    const slide = slideBase(presentation, "World", 13, "The Blue makes accumulated effort feel alive", "Creatures inhabit depth zones, collections expand, and the environment changes with continued use.");
    await addPhone(slide, "20-blue-populated.png", 80, 209, 216, "Sunlit Reef");
    await addPhone(slide, "22-blue-great.png", 344, 209, 216, "Great Blue");
    addFeatureBlock(slide, "Depth", "A world with visible progression", "Move from Sunlit Reef into deeper zones, encounter new species, and see collection counts and depth progress in place.", 624, 226, 520, C.ocean);
    addCallout(slide, "∞", "Beyond Blue", "Regular Flows can surface encounters; Pearls can grow creatures, and release creates another meaningful outcome.", 624, 464, 520, C.teal);
    addNotes(slide, "The populated environment is the clearest visual explanation of the long-term return loop. Move between depth zones during the live demo.");
  }

  // 14 — creatures/chest
  {
    const slide = slideBase(presentation, "Collection", 14, "Every creature retains the story of how it arrived", "The Chest makes ownership operational; detail views preserve provenance and growth state.");
    await addPhone(slide, "21-creature-detail.png", 91, 211, 205, "Creature detail");
    await addPhone(slide, "23-chest-populated.png", 349, 211, 205, "The Chest");
    addFeatureBlock(slide, "Provenance", "Created by a real Flow", "Creature detail connects species, zone, level, age, and Pearl value back to the session that generated it.", 624, 224, 520, C.ocean);
    addFeatureBlock(slide, "Inventory", "Collect, sort, grow, place, release", "The Chest supports filtering and progression actions, giving collection depth beyond a static gallery.", 624, 438, 520, C.teal);
    addNotes(slide, "Open a creature from The Blue or the Chest and emphasize the provenance line linking the reward to a completed Flow.");
  }

  // 15 — badges
  {
    const slide = slideBase(presentation, "Mastery", 15, "Badges make progress legible before and after mastery", "Users can pin, track, showcase, and browse what is within reach.");
    await addPhone(slide, "24-badges-populated.png", 92, 210, 224, "Badge showcase");
    addCallout(slide, "01", "Showcase", "Pin earned badges so mastery becomes identity, not just a hidden counter.", 395, 224, 760, C.gold);
    addCallout(slide, "02", "Track", "Choose an in-progress badge such as a two-hour Flow and keep the next milestone visible.", 395, 351, 760, C.teal);
    addCallout(slide, "03", "Explore", "Badge Book, Within Reach, collections, and celebration states create multiple discovery paths.", 395, 478, 760, C.ocean);
    addText(slide, "The system recognizes duration, variety, collection, Stillwater participation, and other forms of engagement.", { left: 398, top: 617, width: 750, height: 40 }, { fontSize: 19, bold: true, color: C.ink });
    addNotes(slide, "Badges cover more than streaks. Point out duration, variety, collection, and Stillwater achievements in the populated showcase.");
  }

  // 16 — complete feature system (Codex Grid slide 06 adaptation)
  {
    const slide = slideBase(presentation, "System", 16, "The breadth resolves into four coherent value layers", "This is the complete feature map represented in the walkthrough.");
    const cols = [
      ["01", "Act", "Flow • Soft Flow • Surge\nJourney tagging • Live runtime\nPulse during Flow • Arc continuation", C.teal],
      ["02", "Remember", "Story • Sagas • Chronicles\nText • Audio • Photo • Video\nDay/Week/Month analytics", C.ocean],
      ["03", "Plan", "Paths • Planned Flows • Arcs\nFive-step Studio • Suggested Scenes\nScratchPad • Idea Grove", C.plum],
      ["04", "Return", "Shell • Pearls • Lookout • Voyage\nFocus Room • Stillwater • The Blue\nChest • Creatures • Badges", C.gold],
    ];
    cols.forEach((c, i) => {
      const x = 52 + i * 295;
      addShape(slide, "rect", { left: x, top: 220, width: 270, height: 377 }, i % 2 === 0 ? C.paper : C.panel, { style: "solid", fill: C.rule, width: 1 });
      addText(slide, c[0], { left: x + 22, top: 244, width: 60, height: 34 }, { fontSize: 21, bold: true, color: c[3] });
      addText(slide, c[1], { left: x + 22, top: 302, width: 220, height: 46 }, { fontSize: 29, bold: true, color: C.black });
      addText(slide, c[2], { left: x + 22, top: 373, width: 225, height: 175 }, { fontSize: 17, color: C.muted });
    });
    addText(slide, "One data spine: sessions, journeys, reflections, plans, rewards, and collection state reinforce each other.", { left: 123, top: 625, width: 1034, height: 36 }, { fontSize: 20, bold: true, alignment: "center", color: C.ink });
    addNotes(slide, "Use this as the completeness check. Every named feature in the app belongs to one of four user-facing value layers.");
  }

  // 17 — demo path
  {
    const slide = slideBase(presentation, "Live demo", 17, "A five-minute demo can show the entire product promise", "Follow the natural product loop and end in the world that makes progress memorable.");
    const items = [
      ["1", "Start", "Open Story, choose a journey, start a Flow.", "32-story-current.png"],
      ["2", "Focus", "Show live time, Surge, and Pulse capture.", "27-flow-active.png"],
      ["3", "Reflect", "Open Chronicle and show media choices.", "06-chronicle-capture.png"],
      ["4", "Reward", "Complete the Flow and swipe the reveal.", "30-reward-reveal.png"],
      ["5", "Return", "Enter The Shell and descend into The Blue.", "20-blue-populated.png"],
    ];
    for (let i = 0; i < items.length; i += 1) {
      const x = 49 + i * 246;
      await addPhone(slide, items[i][3], x + 42, 224, 118, "", { shadow: false, frameFill: C.ink });
      addText(slide, items[i][0], { left: x, top: 505, width: 34, height: 34 }, { fontSize: 23, bold: true, color: i === 4 ? C.gold : C.teal });
      addText(slide, items[i][1], { left: x + 42, top: 505, width: 165, height: 34 }, { fontSize: 21, bold: true, color: C.black });
      addText(slide, items[i][2], { left: x, top: 548, width: 207, height: 72 }, { fontSize: 15, color: C.muted });
      if (i < items.length - 1) {
        addText(slide, "→", { left: x + 208, top: 348, width: 34, height: 34 }, { fontSize: 27, bold: true, color: C.rule });
      }
    }
    addText(slide, "Optional coda: open Paths to show that the next session is already plan-able.", { left: 262, top: 647, width: 756, height: 26 }, { fontSize: 17, bold: true, alignment: "center", color: C.ink });
    addNotes(slide, "Use this exact route for a concise stakeholder demo. It shows action, reflection, reward, and retention without getting trapped in menus.");
  }

  // 18 — close / deliberate resolution
  {
    const slide = presentation.slides.add();
    slide.background.fill = C.ink;
    await addImage(slide, "app-icon.png", { left: 58, top: 58, width: 80, height: 80 }, { fit: "cover", alt: "Scyra app icon", radius: 22 });
    addText(slide, "SCYRA", { left: 164, top: 73, width: 210, height: 32 }, { fontSize: 16, bold: true, color: C.tealSoft });
    addText(slide, "A productivity system\nusers can inhabit", { left: 58, top: 205, width: 650, height: 148 }, { fontSize: 52, bold: true, color: C.paper });
    addText(slide, "Focus creates evidence. Evidence creates meaning. Meaning creates a reason to return.", { left: 61, top: 390, width: 600, height: 78 }, { fontSize: 23, color: C.tealSoft });
    addText(slide, "Decision to advance", { left: 61, top: 576, width: 260, height: 25 }, { fontSize: 13, bold: true, color: C.gold });
    addText(slide, "Align on the live-demo story, target audience, and next validation milestone.", { left: 61, top: 610, width: 620, height: 52 }, { fontSize: 19, bold: true, color: C.paper });
    await addPhone(slide, "19-shell-populated.png", 768, 70, 192, "", { frameFill: C.paper, frameLine: C.paper });
    await addPhone(slide, "20-blue-populated.png", 984, 140, 192, "", { frameFill: C.oceanSoft, frameLine: C.oceanSoft });
    addNotes(slide, "Close by resolving the opening: the product is a continuous system that turns intentional time into a world users can revisit. Invite alignment on the next validation milestone.");
  }

  for (const [index, slide] of presentation.slides.items.entries()) {
    const stem = `slide-${String(index + 1).padStart(2, "0")}`;
    await writeBlob(path.join(RENDERED, `${stem}.png`), await presentation.export({ slide, format: "png", scale: 1 }));
    const layout = await slide.export({ format: "layout" });
    await fs.writeFile(path.join(RENDERED, `${stem}.layout.json`), await layout.text());
  }

  await writeBlob(path.join(RENDERED, "deck-montage.webp"), await presentation.export({ format: "webp", montage: true, scale: 1 }));
  const pptx = await PresentationFile.exportPptx(presentation);
  await pptx.save(FINAL_PPTX);
  await pptx.save(path.join(OUTPUT, "Scyra_Investor_Product_Demo.pptx"));
  const snapshot = await presentation.inspect({ kind: "slide,textbox,image,notes", maxChars: 24000 });
  await fs.writeFile(path.join(OUTPUT, "deck-inspect.ndjson"), snapshot.ndjson);
  console.log(JSON.stringify({ slides: presentation.slides.items.length, final: FINAL_PPTX, rendered: RENDERED }));
}

buildDeck().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
