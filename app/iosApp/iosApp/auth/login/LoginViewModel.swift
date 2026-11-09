import Foundation


@MainActor
final class LoginViewModel: ObservableObject {
    // The text fields own the text; each edit is forwarded to the shared view model
    @Published var username = "" { didSet { if username != oldValue { form.setUsername(username) } } }
    @Published var password = "" { didSet { if password != oldValue { form.setPassword(password) } } }
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?

    private let form: LoginFormController

    init(auth: AuthGateway, session: SessionManager) {
        form = auth.makeLoginForm { user in
            // Kotlin calls this on the main thread. RootView switches to the chat list once the session changes.
            Task { @MainActor in
                Haptics.success()
                session.onAuthenticated(user)
            }
        }
    }

    deinit {
        form.close()
    }

    /// Run from the view with `.task`: follows the shared state until the view goes away.
    func run() async {
        for await state in form.updates() {
            if isLoading != state.isLoading { isLoading = state.isLoading }
            if errorMessage != state.errorMessage {
                errorMessage = state.errorMessage
                if state.errorMessage != nil { Haptics.error() }
            }
        }
    }

    func login() {
        form.submit()
    }
}
