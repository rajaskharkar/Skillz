import SwiftUI

struct AppRootView: View {
    let container: AppDependencyContainer

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.95, green: 0.98, blue: 0.98),
                    Color(red: 0.86, green: 0.94, blue: 0.93)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 12) {
                Text("Scyra")
                    .font(.system(size: 44, weight: .semibold, design: .default))
                    .foregroundStyle(Color(red: 0.25, green: 0.56, blue: 0.55))

                Text("iOS foundation ready")
                    .font(.system(.headline, design: .default, weight: .regular))
                    .foregroundStyle(.secondary)
            }
            .padding(32)
            .frame(maxWidth: .infinity)
        }
    }
}

#Preview {
    AppRootView(container: AppDependencyContainer())
}
