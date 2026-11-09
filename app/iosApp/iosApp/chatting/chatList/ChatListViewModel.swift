import SharedLogic
import Foundation
 

struct ChatRoute: Hashable {
    let conversationId: String
    let peerId: String
    let peerUsername: String
}
 
@MainActor
final class ChatListViewModel: ObservableObject {
    /// Kotlin's `ChatListUiState`: contacts, isLoading, errorMessage, connection, openingUserId.
    @Published private(set) var state: ChatListUiState
    /// Set when opening a chat fails; the view shows it in an alert and clears it.
    @Published var alertMessage: String?
 
    private let shared: SharedChatListViewModel
    private let session: SessionManager
    private var observer: KotlinObserver?
 
    init(shared: SharedChatListViewModel, session: SessionManager) {
        self.shared = shared
        self.session = session
        self.state = shared.state.value as! ChatListUiState
        observer = KotlinObserver(watch: { callback in
            let handle = shared.watch(onState: callback)
            return { handle.dispose() }
        }) { [weak self] state in
            self?.state = state
        }
    }
 
    deinit { shared.clear() }
 
    func refresh() { shared.refresh() }
 
    func logout() { session.logout() }
 
    /// Where to go when a person is tapped. Returns nil if it failed (an alert is shown) or another tap is still being handled.
    func open(_ contact: ContactItem) async -> ChatRoute? {
        // Kotlin's sealed OpenChatResult becomes a protocol with one class per case
        guard let result = try? await shared.openChat(contactId: contact.id) else {
            alertMessage = "Couldn't open the chat. Please try again."
            return nil
        }
        if let opened = result as? OpenChatResultOpened {
            return ChatRoute(conversationId: opened.conversationId, peerId: opened.peerId, peerUsername: opened.peerUsername)
        }
        if let failed = result as? OpenChatResultFailed {
            Haptics.error()
            alertMessage = failed.message
        }
        return nil // Ignored, or failed
    }
}
 
