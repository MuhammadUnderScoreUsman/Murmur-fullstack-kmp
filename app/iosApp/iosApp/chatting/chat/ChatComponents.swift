import SwiftUI
import SharedLogic

// Small pieces used by both the chat list and the chat screen.

/// Thin bar shown only while the live connection isn't up, so a missing green dot is explained.
struct ConnectionBanner: View {
    /// Kotlin's `ConnectionState` (disconnected, connecting, connected)
    let connection: ConnectionState

    var body: some View {
        if connection != ConnectionState.connected {
            HStack(spacing: 8) {
                ProgressView()
                    .controlSize(.small)
                Text(connection == ConnectionState.connecting ? "Connecting…" : "Not connected. Retrying…")
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
            .background(.bar)
            .transition(.move(edge: .top).combined(with: .opacity))
        }
    }
}

struct MessageStateView: View {
    let symbol: String
    let title: String
    var message: String? = nil
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 48))
                .foregroundStyle(.tertiary)
            Text(title)
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            if let message {
                Text(message)
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
            }
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(.borderedProminent)
                    .padding(.top, 4)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
