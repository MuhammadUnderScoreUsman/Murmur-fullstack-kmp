import Foundation


@MainActor
final class RegisterViewModel: ObservableObject {
    @Published var username = "" { didSet { if username != oldValue { form.setUsername(username) } } }
    @Published var password = "" { didSet { if password != oldValue { form.setPassword(password) } } }
    @Published var confirmPassword = "" { didSet { if confirmPassword != oldValue { form.setConfirmPassword(confirmPassword) } } }
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?

    private let form: RegisterFormController

    init(auth: AuthGateway, session: SessionManager) {
        form = auth.makeRegisterForm { user in
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

    func register() {
        form.submit()
    }
}
