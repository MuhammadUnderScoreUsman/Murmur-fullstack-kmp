import Combine
import UIKit
import SharedLogic


@MainActor
final class RealtimeController {
    private var cancellables = Set<AnyCancellable>()

    init(session: SessionManager, client: RealtimeClient) {
        let center = NotificationCenter.default

        // Not .inactive: pulling down Notification Center or an incoming call also makes the app
        // inactive, and that shouldn't drop the connection.
        let isInForeground = center.publisher(for: UIApplication.willEnterForegroundNotification).map { _ in true }
            .merge(with: center.publisher(for: UIApplication.didEnterBackgroundNotification).map { _ in false })
            .prepend(UIApplication.shared.applicationState != .background)

        Publishers.CombineLatest(session.$state, isInForeground)
            .map { state, foreground -> String? in
                guard foreground, case .loggedIn(let user) = state else { return nil }
                return user.id
            }
            .removeDuplicates()
            .sink { userId in
                if let userId {
                    client.connect(userId: userId)
                } else {
                    client.disconnect()
                }
            }
            .store(in: &cancellables)
    }
}
