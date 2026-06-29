import SwiftUI

struct NotepadPlaceholderView: View {
    var body: some View {
        PlaceholderRouteView(
            route: .notepad,
            message: "Notepad will hold quick notes connected to your Scyra practice."
        )
    }
}

#Preview { NotepadPlaceholderView() }
