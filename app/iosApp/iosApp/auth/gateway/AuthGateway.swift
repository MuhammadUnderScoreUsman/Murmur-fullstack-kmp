import Foundation
struct AppUser: Equatable, Identifiable {
    let id: String
    let username: String
}

struct AuthFormState: Equatable {
    var isLoading = false
    var errorMessage: String?
}

protocol LoginFormController: AnyObject {

    func updates() -> AsyncStream<AuthFormState>
    func setUsername(_ value: String)
    func setPassword(_ value: String)
    func submit()

    func close()
}

protocol RegisterFormController: AnyObject {
    func updates() -> AsyncStream<AuthFormState>
    func setUsername(_ value: String)
    func setPassword(_ value: String)
    func setConfirmPassword(_ value: String)
    func submit()
    func close()
}

protocol AuthGateway {
    func restoreSession() async -> AppUser?
    func logout() async

    /// `onAuthenticated` is called on the main thread after the server accepts the user.
    func makeLoginForm(onAuthenticated: @escaping (AppUser) -> Void) -> LoginFormController
    func makeRegisterForm(onAuthenticated: @escaping (AppUser) -> Void) -> RegisterFormController
}
