import SwiftUI

struct LoginView: View {
    @StateObject private var viewModel: LoginViewModel
    @FocusState private var focus: Field?

    private enum Field { case username, password }

    init(container: AppContainer) {
        _viewModel = StateObject(wrappedValue: container.makeLoginViewModel())
    }

    var body: some View {
        Form {
            Section {
                AuthHeader(symbol: "bubble.left.and.bubble.right.fill", title: "Welcome back", subtitle: "Log in to continue")
            }
            .listRowBackground(Color.clear)

            Section {
                TextField("Username", text: $viewModel.username)
                    .textContentType(.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.next)
                    .focused($focus, equals: .username)
                    .onSubmit { focus = .password }

                PasswordField(title: "Password", text: $viewModel.password)
                    .submitLabel(.go)
                    .focused($focus, equals: .password)
                    .onSubmit(viewModel.login)
            } footer: {
                FormError(message: viewModel.errorMessage)
            }

            Section {
                PrimaryButton(title: "Log in", isLoading: viewModel.isLoading, action: viewModel.login)
            }

            Section {
                NavigationLink(value: AuthRoute.register) {
                    Text("New here? Create an account")
                }
            }
        }
        .disabled(viewModel.isLoading)
        // Follows the shared view model's state until this screen is gone
        .task { await viewModel.run() }
        .scrollDismissesKeyboard(.interactively)
        .toolbar(.hidden, for: .navigationBar)
    }
}
