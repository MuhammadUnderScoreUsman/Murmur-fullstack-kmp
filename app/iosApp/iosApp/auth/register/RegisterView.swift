import SwiftUI

struct RegisterView: View {
    @StateObject private var viewModel: RegisterViewModel
    @FocusState private var focus: Field?
    @Environment(\.dismiss) private var dismiss

    private enum Field { case username, password, confirm }

    init(container: AppContainer) {
        _viewModel = StateObject(wrappedValue: container.makeRegisterViewModel())
    }

    var body: some View {
        Form {
            Section {
                AuthHeader(symbol: "person.crop.circle.badge.plus", title: "Create account", subtitle: "Pick a username and password")
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

                PasswordField(title: "Password", text: $viewModel.password, contentType: .newPassword)
                    .submitLabel(.next)
                    .focused($focus, equals: .password)
                    .onSubmit { focus = .confirm }

                PasswordField(title: "Confirm password", text: $viewModel.confirmPassword, contentType: .newPassword)
                    .submitLabel(.go)
                    .focused($focus, equals: .confirm)
                    .onSubmit(viewModel.register)
            } footer: {
                FormError(message: viewModel.errorMessage)
            }

            Section {
                PrimaryButton(title: "Create account", isLoading: viewModel.isLoading, action: viewModel.register)
            }

            Section {
                Button("Already have an account? Log in") { dismiss() }
            }
        }
        .disabled(viewModel.isLoading)
        // Follows the shared view model's state until this screen is gone
        .task { await viewModel.run() }
        .scrollDismissesKeyboard(.interactively)
        .navigationBarTitleDisplayMode(.inline)
    }
}