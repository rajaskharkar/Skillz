import Combine
import SwiftUI

@MainActor
final class VoyageHallViewModel: ObservableObject {
    @Published private(set) var stats: VoyageHallStats?
    @Published private(set) var errorMessage: String?
    @Published var selectedPage = 0

    private let repository: any ScyraRepository
    private let now: () -> Date
    private let calendar: Calendar

    init(
        repository: any ScyraRepository,
        now: @escaping () -> Date = Date.init,
        calendar: Calendar = .current
    ) {
        self.repository = repository
        self.now = now
        self.calendar = calendar
    }

    func refresh() {
        do {
            stats = VoyageStatsCalculator.calculate(
                sessions: try repository.fetchAllSessions(), now: now(), calendar: calendar
            )
            errorMessage = nil
        } catch {
            stats = nil
            errorMessage = error.localizedDescription
        }
    }

    func duration(_ milliseconds: Int64) -> String {
        let minutes = max(0, milliseconds / 60_000)
        let hours = minutes / 60
        let remainder = minutes % 60
        if hours > 0 { return remainder > 0 ? "\(hours)h \(remainder)m" : "\(hours)h" }
        return "\(minutes)m"
    }

    func period(_ start: Date, _ end: Date) -> String {
        if calendar.isDate(start, inSameDayAs: end) {
            return start.formatted(date: .abbreviated, time: .omitted)
        }
        return "\(start.formatted(date: .abbreviated, time: .omitted)) – \(end.formatted(date: .abbreviated, time: .omitted))"
    }
}

struct VoyageHallView: View {
    @ObservedObject var viewModel: VoyageHallViewModel
    @State private var detail: VoyageRecordDetail?

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: ScyraSpacing.lg) {
                header
                if let error = viewModel.errorMessage {
                    ScyraEmptyState(
                        systemImage: "exclamationmark.triangle",
                        title: "Voyage Hall could not load",
                        message: "Your records are still safe. \(error)"
                    )
                } else if let stats = viewModel.stats, stats.hasEligibleFlows {
                    Picker("Voyage Hall records", selection: $viewModel.selectedPage) {
                        Text("Core stats").tag(0)
                        Text("Bonus stats").tag(1)
                    }
                    .pickerStyle(.segmented)
                    .accessibilityIdentifier("voyage-tabs")
                    if viewModel.selectedPage == 0 { core(stats) } else { bonus(stats) }
                } else {
                    ScyraEmptyState(
                        systemImage: "sailboat",
                        title: "No records yet",
                        message: "Complete your first Flow to begin filling Voyage Hall."
                    )
                }
            }
            .padding(ScyraSpacing.screenPadding)
        }
        .background(background)
        .onAppear(perform: viewModel.refresh)
        .sheet(item: $detail) { detailSheet($0) }
        .accessibilityIdentifier("voyage-hall-screen")
    }

    private var header: some View {
        ScyraCard(style: .elevated, padding: ScyraSpacing.xl) {
            HStack(spacing: ScyraSpacing.md) {
                ScyraCanonicalIcon(systemName: "sailboat.fill")
                    .font(.system(size: 34, weight: .semibold))
                    .foregroundStyle(ScyraColors.primary)
                    .frame(width: 58, height: 58)
                    .background(ScyraColors.primaryContainer)
                    .clipShape(Circle())
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 3) {
                    Text("Voyage Hall").font(ScyraTypography.screenTitle)
                    Text("Your highest records from every Flow and Arc.")
                        .font(ScyraTypography.body).foregroundStyle(ScyraColors.textSecondary)
                    Text("The Shell remembers the effort you return to.")
                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.primary)
                }
            }
        }
    }

    @ViewBuilder private func core(_ stats: VoyageHallStats) -> some View {
        ScyraSectionHeader(title: "Momentum", subtitle: "Daily consistency")
        momentum(stats)
        ScyraSectionHeader(title: "Arc records", subtitle: "Your strongest linked journeys")
        record(
            "Longest Arc by time",
            value: stats.longestArcByTime.map { viewModel.duration($0.totalDurationMs) } ?? "Not yet",
            detail: stats.longestArcByTime.map { "\($0.flowCount) chained Flows" },
            action: stats.longestArcByTime.map { arc in { detail = .arc("Longest Arc by time", self.viewModel.duration(arc.totalDurationMs), arc) } }
        )
        record(
            "Highest Arc multiplier",
            value: stats.highestArcMultiplier.map { String(format: "%.1f×", $0.multiplier) } ?? "Not yet",
            detail: stats.highestArcMultiplier.map { "Reached across \($0.flowCount) Flows" },
            action: stats.highestArcMultiplier.map { item in { detail = .multiplier("Highest Arc multiplier", String(format: "%.1f×", item.multiplier), item) } }
        )
        record(
            "Most chained Flows",
            value: stats.mostChainedFlowsInArc.map { "\($0.flowCount) Flows" } ?? "Not yet",
            detail: stats.mostChainedFlowsInArc.map { viewModel.duration($0.totalDurationMs) },
            action: stats.mostChainedFlowsInArc.map { arc in { detail = .arc("Most chained Flows", "\(arc.flowCount) Flows", arc) } }
        )
        ScyraSectionHeader(title: "Point records", subtitle: "Best regular-Flow totals")
        pointsRecord("Best day by Scyra Points", stats.bestDayByPoints)
        pointsRecord("Best week by Scyra Points", stats.bestWeekByPoints)
        pointsRecord("Best month by Scyra Points", stats.bestMonthByPoints)
    }

    private func momentum(_ stats: VoyageHallStats) -> some View {
        ScyraCard(style: .elevated, padding: ScyraSpacing.xl) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Current daily streak").font(ScyraTypography.cardTitle)
                    Text(stats.currentDailyStreak.map { "\($0.days) days" } ?? "Not active")
                        .font(ScyraTypography.rewardNumber).foregroundStyle(ScyraColors.primary)
                    Text(stats.currentDailyStreak == nil ? "Return today to begin again." : "Your active chain of daily effort.")
                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 3) {
                    Text("Best").font(ScyraTypography.caption).foregroundStyle(ScyraColors.textMuted)
                    Text(stats.longestDailyStreak.map { "\($0.days) days" } ?? "—")
                        .font(ScyraTypography.cardTitle)
                }
            }
        }
        .accessibilityIdentifier("voyage-current-streak")
    }

    @ViewBuilder private func bonus(_ stats: VoyageHallStats) -> some View {
        ScyraSectionHeader(title: "Flow records", subtitle: "Single-Flow and daily peaks")
        record(
            "Best Flow by Scyra Points",
            value: stats.bestFlowByPoints.map { "\($0.points.formatted()) Scyra Points" } ?? "Not yet",
            detail: stats.bestFlowByPoints.map { viewModel.duration($0.durationMs) },
            action: stats.bestFlowByPoints.map { flow in { detail = .flows("Best Flow by Scyra Points", "\(flow.points.formatted()) Scyra Points", [flow]) } }
        )
        record(
            "Longest Flow",
            value: stats.longestFlow.map { viewModel.duration($0.durationMs) } ?? "Not yet",
            detail: stats.longestFlow.map { "\($0.points.formatted()) Scyra Points" },
            action: stats.longestFlow.map { flow in { detail = .flows("Longest Flow", self.viewModel.duration(flow.durationMs), [flow]) } }
        )
        countRecord("Most Flows in a day", stats.mostFlowsInDay, suffix: "Flows")
        ScyraSectionHeader(title: "Time records", subtitle: "Regular-Flow time by period")
        durationRecord("Most time in a day", stats.mostTimeInDay)
        durationRecord("Most time in a week", stats.mostTimeInWeek)
        durationRecord("Most time in a month", stats.mostTimeInMonth)
        ScyraSectionHeader(title: "Arc volume records", subtitle: "Arcs grouped by their latest Flow")
        arcCountRecord("Most Arcs in a day", stats.mostArcsInDay)
        arcCountRecord("Most Arcs in a week", stats.mostArcsInWeek)
    }

    private func pointsRecord(_ title: String, _ item: VoyagePeriodPointsRecord?) -> some View {
        record(
            title,
            value: item.map { "\($0.points.formatted()) Scyra Points" } ?? "Not yet",
            detail: item.map { "\(viewModel.period($0.startDate, $0.endDate)) · \($0.flowCount) Flows" },
            action: item.map { item in { detail = .flows(title, "\(item.points.formatted()) Scyra Points", item.flows) } }
        )
    }

    private func durationRecord(_ title: String, _ item: VoyagePeriodDurationRecord?) -> some View {
        record(
            title,
            value: item.map { viewModel.duration($0.durationMs) } ?? "Not yet",
            detail: item.map { "\(viewModel.period($0.startDate, $0.endDate)) · \($0.flowCount) Flows" },
            action: item.map { item in { detail = .flows(title, self.viewModel.duration(item.durationMs), item.flows) } }
        )
    }

    private func countRecord(_ title: String, _ item: VoyagePeriodCountRecord?, suffix: String) -> some View {
        record(
            title,
            value: item.map { "\($0.count) \(suffix)" } ?? "Not yet",
            detail: item.map { viewModel.period($0.startDate, $0.endDate) },
            action: item.map { item in { detail = .flows(title, "\(item.count) \(suffix)", item.flows) } }
        )
    }

    private func arcCountRecord(_ title: String, _ item: VoyagePeriodCountRecord?) -> some View {
        record(
            title,
            value: item.map { "\($0.count) Arcs" } ?? "Not yet",
            detail: item.map { viewModel.period($0.startDate, $0.endDate) },
            action: item.map { item in { detail = .arcs(title, "\(item.count) Arcs", item.arcs) } }
        )
    }

    private func record(
        _ title: String,
        value: String,
        detail subtitle: String?,
        action: (() -> Void)?
    ) -> some View {
        ScyraCard(style: action == nil ? .plain : .elevated, action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title).font(ScyraTypography.label)
                    Text(value).font(ScyraTypography.cardTitle).foregroundStyle(ScyraColors.primary)
                    if let subtitle {
                        Text(subtitle).font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                    }
                }
                Spacer()
                if action != nil {
                    ScyraCanonicalIcon(systemName: "chevron.right").foregroundStyle(ScyraColors.textMuted).accessibilityHidden(true)
                }
            }
        }
        .accessibilityLabel([title, value, subtitle, action == nil ? nil : "Tap for details"].compactMap { $0 }.joined(separator: ". "))
    }

    private func detailSheet(_ detail: VoyageRecordDetail) -> some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: ScyraSpacing.md) {
                    Text(detail.value).font(ScyraTypography.rewardNumber).foregroundStyle(ScyraColors.primary)
                    if !detail.arcs.isEmpty {
                        ForEach(Array(detail.arcs.enumerated()), id: \.element.id) { index, arc in
                            ScyraCard {
                                VStack(alignment: .leading, spacing: ScyraSpacing.sm) {
                                    Text("Arc \(index + 1)").font(ScyraTypography.cardTitle)
                                    Text("\(arc.flowCount) Flows · \(viewModel.duration(arc.totalDurationMs)) · \(arc.totalPoints.formatted()) Scyra Points")
                                        .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                                    ForEach(arc.flows) { flowRow($0, highlighted: detail.highlightedSessionID == $0.sessionID) }
                                }
                            }
                        }
                    } else {
                        ForEach(detail.flows) { flowRow($0, highlighted: detail.highlightedSessionID == $0.sessionID) }
                    }
                }
                .padding(ScyraSpacing.screenPadding)
            }
            .background(background)
            .navigationTitle(detail.title)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Close") { self.detail = nil } } }
        }
    }

    private func flowRow(_ flow: VoyageFlowSummary, highlighted: Bool) -> some View {
        ScyraCard {
            VStack(alignment: .leading, spacing: 3) {
                Text(flow.title).font(ScyraTypography.label)
                Text("\(flow.journeyName ?? "Untagged") · \(flow.completedAt.formatted(date: .abbreviated, time: .shortened))")
                    .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textSecondary)
                Text("\(viewModel.duration(flow.durationMs)) · \(flow.points.formatted()) points" + (flow.arcMultiplierUsed.map { String(format: " · %.1f×", $0) } ?? ""))
                    .font(ScyraTypography.caption).foregroundStyle(ScyraColors.textMuted)
                if highlighted { Text("Peak reached here").font(ScyraTypography.caption).foregroundStyle(ScyraColors.primary) }
            }
        }
    }

    private var background: some View {
        LinearGradient(colors: [ScyraColors.background, ScyraColors.backgroundBottom], startPoint: .top, endPoint: .bottom)
            .ignoresSafeArea()
    }
}

private struct VoyageRecordDetail: Identifiable {
    let id = UUID()
    let title: String
    let value: String
    let flows: [VoyageFlowSummary]
    let arcs: [VoyageArcSummary]
    let highlightedSessionID: UUID?

    static func flows(_ title: String, _ value: String, _ flows: [VoyageFlowSummary]) -> Self {
        .init(title: title, value: value, flows: flows, arcs: [], highlightedSessionID: nil)
    }

    static func arcs(_ title: String, _ value: String, _ arcs: [VoyageArcSummary]) -> Self {
        .init(title: title, value: value, flows: [], arcs: arcs, highlightedSessionID: nil)
    }

    static func arc(_ title: String, _ value: String, _ arc: VoyageArcRecord) -> Self {
        .init(title: title, value: value, flows: arc.flows, arcs: [], highlightedSessionID: nil)
    }

    static func multiplier(_ title: String, _ value: String, _ item: VoyageMultiplierRecord) -> Self {
        .init(title: title, value: value, flows: item.flows, arcs: [], highlightedSessionID: item.reachedInSessionID)
    }
}

#Preview {
    VoyageHallView(viewModel: VoyageHallViewModel(repository: InMemoryFlowRepository()))
}
