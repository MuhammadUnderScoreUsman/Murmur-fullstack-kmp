import SwiftUI
import SharedLogic

/// Owns the view model and navigation. The drawing is in `ChatListContent`, which only needs a Kotlin state.
struct ChatListView: View {
    let user: AppUser
    let container: AppContainer

    @StateObject private var viewModel: ChatListViewModel
    @State private var path: [ChatRoute] = []

    init(user: AppUser, container: AppContainer) {
        self.user = user
        self.container = container
        _viewModel = StateObject(wrappedValue: container.makeChatListViewModel())
    }

    var body: some View {
        NavigationStack(path: $path) {
            ChatListContent(
                username: user.username,
                state: viewModel.state,
                alertMessage: $viewModel.alertMessage,
                onOpen: { contact in
                    Task {
                        if let route = await viewModel.open(contact) { path.append(route) }
                    }
                },
                onRefresh: { viewModel.refresh() },
                onLogout: { viewModel.logout() }
            )
            .navigationDestination(for: ChatRoute.self) { route in
                ChatView(route: route, container: container)
            }
        }
    }
}

/// Everything on screen, from a state. No view model, no container: previews just build a Kotlin state.
struct ChatListContent: View {
    let username: String
    let state: ChatListUiState
    @Binding var alertMessage: String?
    let onOpen: (ContactItem) -> Void
    let onRefresh: () -> Void
    let onLogout: () -> Void

    @State private var searchText = ""
    @State private var showLogoutConfirmation = false

    private var filteredContacts: [ContactItem] {
        let query = searchText.trimmingCharacters(in: .whitespaces)
        guard !query.isEmpty else { return state.contacts }
        return state.contacts.filter { $0.username.localizedCaseInsensitiveContains(query) }
    }

    var body: some View {
        content
            .navigationTitle("Chats")
            .searchable(text: $searchText, prompt: "Search")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Text("Signed in as @\(username)")
                        Button(role: .destructive) {
                            showLogoutConfirmation = true
                        } label: {
                            Label("Log out", systemImage: "rectangle.portrait.and.arrow.right")
                        }
                    } label: {
                        AvatarView(name: username, size: 30)
                    }
                }
            }
            .confirmationDialog("Log out of @\(username)?", isPresented: $showLogoutConfirmation, titleVisibility: .visible) {
                Button("Log out", role: .destructive, action: onLogout)
            }
            .alert(
                "Couldn't open the chat",
                isPresented: Binding(
                    get: { alertMessage != nil },
                    set: { if !$0 { alertMessage = nil } }
                )
            ) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(alertMessage ?? "")
            }
            .safeAreaInset(edge: .top, spacing: 0) {
                ConnectionBanner(connection: state.connection)
            }
            .animation(.easeInOut(duration: 0.2), value: state.connection)
    }

    @ViewBuilder
    private var content: some View {
        if state.isLoading && state.contacts.isEmpty {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let message = state.errorMessage, state.contacts.isEmpty {
            MessageStateView(symbol: "wifi.exclamationmark", title: message, actionTitle: "Retry", action: onRefresh)
        } else if state.contacts.isEmpty {
            MessageStateView(
                symbol: "person.2",
                title: "No one else is here yet",
                message: "Signed in as @\(username).\nRegister another account to see them appear."
            )
        } else {
            List(filteredContacts) { contact in
                Button { onOpen(contact) } label: {
                    ContactRow(contact: contact, isOpening: state.openingUserId == contact.id)
                }
                .buttonStyle(.plain)
                .disabled(state.openingUserId != nil)
            }
            .listStyle(.plain)
            .refreshable { onRefresh() }
            .animation(.default, value: filteredContacts)
        }
    }
}

private struct ContactRow: View {
    let contact: ContactItem
    let isOpening: Bool

    var body: some View {
        HStack(spacing: 12) {
            AvatarView(name: contact.username, showsOnlineDot: contact.isOnline)
            VStack(alignment: .leading, spacing: 2) {
                Text(contact.username)
                    .font(.headline)
                if let preview = contact.lastMessagePreview {
                    Text(preview)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                } else {
                    Text(contact.isOnline ? "Online" : "Offline")
                        .font(.subheadline)
                        .foregroundStyle(contact.isOnline ? Color.green : Color.secondary)
                }
            }
            Spacer(minLength: 8)
            if isOpening {
                ProgressView()
            } else if let time = contact.lastMessageTime {
                Text(time)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle()) // the whole row is tappable, including the empty space
        .accessibilityElement(children: .combine)
    }
}
