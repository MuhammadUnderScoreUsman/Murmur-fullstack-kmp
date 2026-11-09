import SharedLogic

final class KotlinAuthGateway: AuthGateway {
    private let container: SharedContainer

    init(container: SharedContainer) {
        self.container = container
    }

    func restoreSession() async -> AppUser? {
        // `try?` also covers the case where Swift imports the result as optional or non-optional
        guard let user = try? await container.authRepository.restoreSession() else { return nil }
        return AppUser(id: user.id, username: user.username)
    }

    func logout() async {
        try? await container.authRepository.logout()
    }

    func makeLoginForm(onAuthenticated: @escaping (AppUser) -> Void) -> LoginFormController {
        KotlinLoginForm(
            viewModel: container.createLoginViewModel { id, name in
                onAuthenticated(AppUser(id: id, username: name))
            }
        )
    }

    func makeRegisterForm(onAuthenticated: @escaping (AppUser) -> Void) -> RegisterFormController {
        KotlinRegisterForm(
            viewModel: container.createRegisterViewModel { id, name in
                onAuthenticated(AppUser(id: id, username: name))
            }
        )
    }
}

private final class KotlinLoginForm: LoginFormController {
    private let viewModel: SharedLoginViewModel

    init(viewModel: SharedLoginViewModel) {
        self.viewModel = viewModel
    }

    func updates() -> AsyncStream<AuthFormState> {
        // Only the latest state matters, so a slow reader never builds up a backlog
        AsyncStream(AuthFormState.self, bufferingPolicy: .bufferingNewest(1)) { continuation in
            // Kotlin calls this on the main thread, now and on every change
            let handle = viewModel.watch { state in
                continuation.yield(AuthFormState(isLoading: state.isLoading, errorMessage: state.errorMessage))
            }
            continuation.onTermination = { _ in handle.dispose() }
        }
    }

    func setUsername(_ value: String) { viewModel.onUsernameChange(value: value) }
    func setPassword(_ value: String) { viewModel.onPasswordChange(value: value) }
    func submit() { viewModel.login() }
    func close() { viewModel.clear() }
}

private final class KotlinRegisterForm: RegisterFormController {
    private let viewModel: SharedRegisterViewModel

    init(viewModel: SharedRegisterViewModel) {
        self.viewModel = viewModel
    }

    func updates() -> AsyncStream<AuthFormState> {
        AsyncStream(AuthFormState.self, bufferingPolicy: .bufferingNewest(1)) { continuation in
            let handle = viewModel.watch { state in
                continuation.yield(AuthFormState(isLoading: state.isLoading, errorMessage: state.errorMessage))
            }
            continuation.onTermination = { _ in handle.dispose() }
        }
    }

    func setUsername(_ value: String) { viewModel.onUsernameChange(value: value) }
    func setPassword(_ value: String) { viewModel.onPasswordChange(value: value) }
    func setConfirmPassword(_ value: String) { viewModel.onConfirmPasswordChange(value: value) }
    func submit() { viewModel.register() }
    func close() { viewModel.clear() }
}
