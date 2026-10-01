import SwiftUI

struct BlueRealmSelector: View {
    let onSelect: (CreatureRealm) -> Void

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                LandEnvironmentView(zone: .goldenFields)
                VStack(spacing: 0) {
                    Color.clear.frame(height: proxy.size.height * 0.49)
                    LinearGradient(colors: [Color(red: 0.525, green: 0.667, blue: 0.651), Color(red: 0.271, green: 0.427, blue: 0.486)], startPoint: .top, endPoint: .bottom)
                        .overlay {
                            Canvas { context, size in
                                for index in 0..<6 {
                                    var line = Path()
                                    line.move(to: CGPoint(x: 0, y: size.height * (0.12 + Double(index) * 0.137)))
                                    line.addLine(to: CGPoint(x: size.width, y: size.height * (0.06 + Double(index) * 0.137)))
                                    context.stroke(line, with: .color(.white.opacity(0.12)), lineWidth: 1)
                                }
                            }
                        }
                }
                VStack(spacing: 20) {
                    VStack(spacing: 6) {
                        Text("The Blue").font(ScyraTypography.screenTitle)
                        Text("One living Earth").font(ScyraTypography.cardTitle)
                    }
                    .foregroundStyle(.black).multilineTextAlignment(.center)
                    Spacer(minLength: 8)
                    entry(.land, title: "Land", description: "Golden fields, ancient woods, and the great wild.\nLife earned through the depth of your Arcs.")
                    Spacer(minLength: 20)
                    entry(.sea, title: "Sea", description: "From the sunlit reef to the great blue.\nLife earned through the duration of your Flows.")
                    Spacer(minLength: 8)
                }
                .padding(24)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("blue-realm-selector")
    }

    private func entry(_ realm: CreatureRealm, title: String, description: String) -> some View {
        Button { onSelect(realm) } label: {
            TheBlueOverlaySurface {
                VStack(alignment: .leading, spacing: 10) {
                    Text(title).font(ScyraTypography.screenTitle)
                    Text(description).font(ScyraTypography.body)
                    Text("Enter \(title) →").font(ScyraTypography.label).foregroundStyle(ScyraColors.primary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 12)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Enter \(title)")
        .accessibilityIdentifier("blue-realm-\(realm.rawValue)")
    }
}
