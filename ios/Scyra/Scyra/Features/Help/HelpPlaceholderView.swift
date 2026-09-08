import SwiftUI

struct HelpView: View {
    @ObservedObject var movementController: MovementController
    @ObservedObject var preferences: AppPreferencesModel
    @State private var selectedPage = 0
    @State private var showsLanguagePicker = false

    private var text: HelpText { HelpText(language: preferences.language) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(text.string("help_screen_title", "Help"))
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(ScyraColors.textPrimary.opacity(0.80))
                    .accessibilityAddTraits(.isHeader)

                PreferenceToggleRow(
                    title: text.string("help_pref_keep_score_title", "Keep Score Visible"),
                    description: text.string("help_pref_keep_score_description", "Show score totals in Story, and score labels in cards."),
                    isOn: preferences.showScoreUI,
                    accessibilityIdentifier: "help-show-score-toggle",
                    onChange: preferences.setShowScoreUI
                )
                PreferenceToggleRow(
                    title: text.string("help_pref_calm_mode_title", "Calm Mode"),
                    description: text.string("help_pref_calm_mode_description", "Hides Arc information and timer in Flow."),
                    isOn: preferences.calmMode,
                    accessibilityIdentifier: "help-calm-mode-toggle",
                    onChange: preferences.setCalmMode
                )

                Button { showsLanguagePicker = true } label: {
                    HStack(alignment: .top, spacing: ScyraSpacing.md) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(text.string("help_language_title", "App Language"))
                                .font(ScyraTypography.cardTitle)
                                .foregroundStyle(ScyraColors.textPrimary)
                            Text(text.string("help_language_description", "Follow your device language, or choose one for this app."))
                                .font(ScyraTypography.caption)
                                .foregroundStyle(ScyraColors.textSecondary)
                                .multilineTextAlignment(.leading)
                        }
                        Spacer(minLength: 8)
                        Text(text.languageName(preferences.language))
                            .font(ScyraTypography.label)
                            .foregroundStyle(ScyraColors.primary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(ScyraColors.primaryContainer)
                            .clipShape(Capsule())
                    }
                    .padding(16)
                    .background(ScyraColors.surfaceVariant.opacity(0.35))
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(text.string("help_language_title", "App Language")). \(text.languageName(preferences.language))")
                .accessibilityHint(text.string("help_language_description", "Follow your device language, or choose one for this app."))

                MovementSettingsCard(controller: movementController)
                helpCarousel
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 18)
        }
        .background(ScyraColors.background)
        .sheet(isPresented: $showsLanguagePicker) {
            LanguagePickerSheet(
                text: text,
                selection: preferences.language,
                onDone: {
                    preferences.setLanguage($0)
                    showsLanguagePicker = false
                },
                onCancel: { showsLanguagePicker = false }
            )
            .presentationDetents([.medium])
        }
    }

    private var helpCarousel: some View {
        let pages = text.pages
        return VStack(spacing: 14) {
            TabView(selection: $selectedPage) {
                ForEach(Array(pages.enumerated()), id: \.offset) { index, page in
                    HelpInfoCard(page: page)
                        .padding(.horizontal, 34)
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: 390)
            .overlay(alignment: .topTrailing) {
                Text("\(selectedPage + 1)/\(pages.count)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(ScyraColors.textPrimary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(ScyraColors.surface.opacity(0.96), in: Capsule())
                    .shadow(color: .black.opacity(0.14), radius: 6, y: 2)
                    .padding(.top, 12)
                    .padding(.trailing, 42)
            }
            .accessibilityLabel("Help topics")
            .accessibilityValue("Help topic \(selectedPage + 1) of \(pages.count)")

            HStack(spacing: 8) {
                ForEach(pages.indices, id: \.self) { index in
                    Button { withAnimation { selectedPage = index } } label: {
                        Capsule()
                            .fill(index == selectedPage ? ScyraColors.primary : ScyraColors.border)
                            .frame(width: index == selectedPage ? 18 : 6, height: 6)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Go to help topic \(index + 1)")
                    .accessibilityValue(index == selectedPage ? "On" : "Off")
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(ScyraColors.surfaceVariant.opacity(0.42))
            .clipShape(Capsule())
        }
    }
}

private struct PreferenceToggleRow: View {
    let title: String
    let description: String
    let isOn: Bool
    let accessibilityIdentifier: String
    let onChange: (Bool) -> Void

    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: onChange)) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(ScyraTypography.cardTitle)
                Text(description)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
            }
        }
        .tint(ScyraColors.primary)
        .padding(14)
        .background(ScyraColors.surfaceVariant.opacity(0.35))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .accessibilityLabel("\(title). \(description)")
        .accessibilityValue(isOn ? "On" : "Off")
        .accessibilityIdentifier(accessibilityIdentifier)
    }
}

private struct HelpInfoCard: View {
    let page: HelpPage

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 8) {
                    ZStack {
                        Circle().fill(ScyraColors.onPrimary.opacity(0.16))
                        Image("scyraTurtle")
                            .resizable()
                            .scaledToFit()
                            .padding(4)
                    }
                    .frame(width: 28, height: 28)
                    .accessibilityHidden(true)
                    Text(page.kicker)
                        .font(ScyraTypography.label)
                }
                .foregroundStyle(ScyraColors.onPrimary)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(ScyraColors.primary, in: Capsule())
                Text(page.title)
                    .font(.title2.weight(.bold))
                    .accessibilityAddTraits(.isHeader)
                Text(page.subtitle)
                    .font(ScyraTypography.body)
                    .foregroundStyle(ScyraColors.textSecondary)
                Text(page.body)
                    .font(ScyraTypography.body)
                    .foregroundStyle(ScyraColors.textPrimary)
                Spacer(minLength: 0)
                Text(page.tip)
                    .font(ScyraTypography.caption)
                    .foregroundStyle(ScyraColors.textSecondary)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(ScyraColors.background.opacity(0.60))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .accessibilityLabel("Tip: \(page.tip)")
            }
            .frame(maxWidth: .infinity, minHeight: 245, alignment: .topLeading)
        }
        .padding(18)
        .frame(maxWidth: .infinity, minHeight: 245, alignment: .topLeading)
        .background(ScyraColors.surfaceVariant.opacity(0.40))
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

private struct LanguagePickerSheet: View {
    let text: HelpText
    let onDone: (AppLanguage) -> Void
    let onCancel: () -> Void
    @State private var pending: AppLanguage

    init(text: HelpText, selection: AppLanguage, onDone: @escaping (AppLanguage) -> Void, onCancel: @escaping () -> Void) {
        self.text = text
        self.onDone = onDone
        self.onCancel = onCancel
        _pending = State(initialValue: selection)
    }

    var body: some View {
        NavigationStack {
            List(AppLanguage.allCases) { language in
                Button { pending = language } label: {
                    HStack {
                        Text(text.languageName(language)).foregroundStyle(ScyraColors.textPrimary)
                        Spacer()
                        if language == pending {
                            ScyraCanonicalIcon(systemName: "checkmark.circle.fill").foregroundStyle(ScyraColors.primary)
                        }
                    }
                }
                .accessibilityValue(language == pending ? "Selected" : "Not selected")
            }
            .navigationTitle(text.string("help_language_dialog_title", "Choose app language"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel", action: onCancel) }
                ToolbarItem(placement: .confirmationAction) {
                    Button(text.string("help_language_dialog_confirm", "Done")) { onDone(pending) }
                }
            }
        }
    }
}

struct HelpPage: Equatable, Sendable {
    let systemImage: String
    let kicker: String
    let title: String
    let subtitle: String
    let body: String
    let tip: String
}

struct HelpText: Equatable, Sendable {
    let language: AppLanguage

    func string(_ key: String, _ fallback: String) -> String {
        AppLocalization.string(key, language: language, fallback: fallback)
    }

    func languageName(_ value: AppLanguage) -> String {
        switch value {
        case .system: string("help_language_system_default", "System default")
        case .english: string("help_language_english", "English")
        case .spanish: string("help_language_spanish", "Español")
        case .hindi: string("help_language_hindi", "हिन्दी")
        case .marathi: string("help_language_marathi", "मराठी")
        }
    }

    var pages: [HelpPage] {
        [
            page("flow", "sparkles", "Flow", "A Flow is one focused session.", "Start a Flow when you want to intentionally spend time on something that matters.", "A Flow tracks your time, keeps the session alive in the background, and lets you add notes so the work becomes part of your Story. Every Flow is a clean, deliberate chapter of effort.", "Use Flow for deep work, practice, study, writing, workouts, or anything you want to do with intention."),
            page("soft_flow", "leaf", "Soft Flow", "Soft Flow is a gentler session.", "Use Soft Flow when you want to stay present with an activity without turning it into a scored push.", "Soft Flow still tracks time and lets the moment become part of your Story, but it does not award score. It is designed for quieter sessions where the goal is simply to show up, breathe, recover, reflect, or move gently without pressure.", "Use Soft Flow for walks, stretching, journaling, meditation, light reading, recovery time, or any session you want to honor without gamifying."),
            page("pulse", "brain.head.profile", "Pulse", "A Pulse is a quick moment log.", "Use a Pulse when you want to capture something small, meaningful, or immediate without starting a full Flow.", "A Pulse helps you record a thought, feeling, realization, event, or micro-win in seconds. It is lighter than a Flow, but still becomes part of your Story, helping you remember the moments that shaped your day.", "Use Pulse for quick reflections, ideas, breakthroughs, emotions, check-ins, or little moments you do not want to lose."),
            page("arc", "flame.fill", "Arc", "Arcs reward continuity.", "An Arc forms when you continue your momentum across consecutive Flows.", "Keep going and your Arc grows. After each completed Flow of 10 minutes or more, the Arc multiplier increases by +0.1. You have a short grace window between Flows, so continuing quickly helps preserve the chain.", "Think of an Arc as momentum across sessions. Save Flow, continue, and keep the chain alive."),
            page("surge", "bolt.fill", "Surge", "Surge rewards precision.", "Set a target duration before you begin and try to finish close to it.", "Surge is for sessions where you want to commit to a specific amount of time. The closer your actual session is to the planned duration, the better the reward. It adds a satisfying sense of control and intentional execution.", "Use Surge when you want a clear mission, such as 20 minutes of reading, 45 minutes of coding, or 30 minutes of practice.")
        ]
    }

    private func page(_ key: String, _ image: String, _ kicker: String, _ title: String, _ subtitle: String, _ body: String, _ tip: String) -> HelpPage {
        HelpPage(
            systemImage: image,
            kicker: string("help_page_\(key)_kicker", kicker),
            title: string("help_page_\(key)_title", title),
            subtitle: string("help_page_\(key)_subtitle", subtitle),
            body: string("help_page_\(key)_body", body),
            tip: string("help_page_\(key)_tip", tip)
        )
    }
}
