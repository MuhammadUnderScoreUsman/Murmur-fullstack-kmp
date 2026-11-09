import Foundation
import SharedLogic


@MainActor
final class AppContainer: ObservableObject {
    let shared: SharedContainer
    let auth: AuthGateway
    let session: SessionManager

    // Keeps the socket in step with "logged in and visible"
    private let realtimeController: RealtimeController

    /// `auth` is only replaced in previews.
    init(shared: SharedContainer = SharedContainer(baseUrl: AppConfig.baseURL), auth: AuthGateway? = nil) {
        self.shared = shared
        let auth = auth ?? KotlinAuthGateway(container: shared)
        self.auth = auth
        session = SessionManager(gateway: auth)
        realtimeController = RealtimeController(session: session, client: shared.realtimeClient)
    }

    func makeLoginViewModel() -> LoginViewModel {
        LoginViewModel(auth: auth, session: session)
    }

    func makeRegisterViewModel() -> RegisterViewModel {
        RegisterViewModel(auth: auth, session: session)
    }

    func makeChatListViewModel() -> ChatListViewModel {
        ChatListViewModel(shared: shared.createChatListViewModel(myId: currentUserId), session: session)
    }

    func makeChatViewModel(route: ChatRoute) -> ChatViewModel {
        // A chat screen only exists while someone is logged in
        ChatViewModel(
            shared: shared.createChatViewModel(
                conversationId: route.conversationId,
                peerId: route.peerId,
                peerUsername: route.peerUsername,
                myId: currentUserId ?? ""
            )
        )
    }

    private var currentUserId: String? {
        if case .loggedIn(let user) = session.state { return user.id }
        return nil
    }
}
