import SwiftUI


struct RootView: View {
    @EnvironmentObject private var session: SessionManager
    @EnvironmentObject private var container: AppContainer

    var body: some View {
        ZStack {
            switch session.state {
            case .loading:
//                ProgressView()
//                    .controlSize(.large)
                
                SplashView()
            case .loggedOut:
                AuthFlowView()
                    .transition(.opacity)
            case .loggedIn(let user):
                ChatListView(user: user, container: container)
                    .id(user.id) // a different user never inherits the previous user's view model
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: session.state)
    }
}

enum AuthRoute: Hashable {
    case register
}

struct AuthFlowView: View {
    @EnvironmentObject private var container: AppContainer

    var body: some View {
        NavigationStack {
            LoginView(container: container)
                .navigationDestination(for: AuthRoute.self) { route in
                    switch route {
                    case .register:
                        RegisterView(container: container)
                    }
                }
        }
    }
}
