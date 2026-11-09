import Foundation

enum SessionState: Equatable {
    /// App just started, checking whether a user was saved.
    case loading
    case loggedOut
    case loggedIn(AppUser)
}

@MainActor
final class SessionManager: ObservableObject {
    @Published private(set) var state: SessionState = .loading

    private let gateway: AuthGateway

    init(gateway: AuthGateway)  {
        self.gateway = gateway
        Task {
            try? await Task.sleep(for: .seconds(2))
            await restore()
        }
    }

 
    func onAuthenticated(_ user: AppUser) {
        state = .loggedIn(user)
    }

    func logout() {
        Task {
            await gateway.logout()
            state = .loggedOut
        }
    }

    private func restore() async {
        if let user = await gateway.restoreSession() {
            state = .loggedIn(user)
        } else {
            state = .loggedOut
        }
    }
}
