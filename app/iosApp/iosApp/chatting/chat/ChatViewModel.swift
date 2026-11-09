import Foundation
import SharedLogic

@MainActor
final class ChatViewModel: ObservableObject {
 
    @Published private(set) var state: ChatUiState

    private let shared: SharedChatViewModel
    private var observer: KotlinObserver?

    init(shared: SharedChatViewModel) {
        self.shared = shared
        self.state = shared.state.value as! ChatUiState
        observer = KotlinObserver(watch: { callback in
            let handle = shared.watch(onState: callback)
            return { handle.dispose() }
        }) { [weak self] state in
            self?.state = state
        }
    }

    deinit { shared.clear() }

    func reload() { shared.reload() }
    func loadOlder() { shared.loadOlder() }
    /// Returns true if the message was accepted (so the screen can clear the text box).
    func send(_ text: String) -> Bool { shared.send(text: text) }
    func retry(_ messageId: String) { shared.retry(messageId: messageId) }
    func discard(_ messageId: String) { shared.discard(messageId: messageId) }
}
