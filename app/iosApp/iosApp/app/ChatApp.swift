import SwiftUI

@main
struct ChatApp: App {
    @StateObject private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(container)
                // SessionManager is its own ObservableObject, so it has to be injected directly
                // for RootView to refresh when the session changes
                .environmentObject(container.session)
        }
    }
}
