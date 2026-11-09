import SwiftUI
import SharedLogic

private let maxMessageLength = 2000
private let bottomID = "bottom"


struct ChatView: View {
    @StateObject private var viewModel: ChatViewModel

    init(route: ChatRoute, container: AppContainer) {
        _viewModel = StateObject(wrappedValue: container.makeChatViewModel(route: route))
    }

    var body: some View {
        ChatContent(
            state: viewModel.state,
            onReload: { viewModel.reload() },
            onLoadOlder: { viewModel.loadOlder() },
            onSend: { viewModel.send($0) },
            onRetry: { viewModel.retry($0) },
            onDiscard: { viewModel.discard($0) }
        )
    }
}

struct ChatContent: View {
    let state: ChatUiState
    let onReload: () -> Void
    let onLoadOlder: () -> Void
    let onSend: (String) -> Bool
    let onRetry: (String) -> Void
    let onDiscard: (String) -> Void

    @State private var draft = ""
    /// Whether the newest message is on screen. New messages from the other person only scroll the view when it is.
    @State private var atBottom = true
    /// The oldest message before an older page was requested, so the view can stay on it when the page arrives.
    @State private var anchorBeforeOlder: String?

    var body: some View {
        content
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) { header }
            }
            .safeAreaInset(edge: .top, spacing: 0) {
                ConnectionBanner(connection: state.connection)
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                inputBar
            }
            .animation(.easeInOut(duration: 0.2), value: state.connection)
    }

    private var header: some View {
        HStack(spacing: 8) {
            AvatarView(name: state.peerUsername, size: 32, showsOnlineDot: state.peerOnline)
            VStack(alignment: .leading, spacing: 0) {
                Text(state.peerUsername)
                    .font(.headline)
                Text(state.peerOnline ? "Online" : "Offline")
                    .font(.caption2)
                    .foregroundStyle(state.peerOnline ? Color.green : Color.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var content: some View {
        if state.isLoading && state.messages.isEmpty {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let message = state.errorMessage, state.messages.isEmpty {
            MessageStateView(symbol: "wifi.exclamationmark", title: message, actionTitle: "Retry", action: onReload)
        } else if state.messages.isEmpty {
            MessageStateView(
                symbol: "bubble.left.and.bubble.right",
                title: "No messages yet",
                message: "Say hello to @\(state.peerUsername)."
            )
        } else {
            messageList
        }
    }

    private var messageList: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 6) {
                    if state.isLoadingOlder {
                        ProgressView().padding(8)
                    }
                    ForEach(state.messages) { row in
                        MessageBubble(
                            row: row,
                            onRetry: { onRetry(row.id) },
                            onDiscard: { onDiscard(row.id) }
                        )
                        .id(row.id)
                        .onAppear {
                            // The oldest loaded message came into view: ask for the previous page
                            // (the shared code ignores this if there is nothing older)
                            if row.id == state.messages.first?.id, !state.isLoadingOlder {
                                anchorBeforeOlder = row.id
                                onLoadOlder()
                            }
                        }
                    }
                    Color.clear
                        .frame(height: 1)
                        .id(bottomID)
                        .onAppear { atBottom = true }
                        .onDisappear { atBottom = false }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
            }
            .defaultScrollAnchor(.bottom) // opens on the newest message
            .scrollDismissesKeyboard(.interactively)
            // Follow new messages, but don't yank the view if the user has scrolled up to read; their own sends always scroll
            .onChange(of: state.messages.last?.id) { _, _ in
                guard let last = state.messages.last else { return }
                if last.isMine || atBottom {
                    withAnimation(.easeOut(duration: 0.2)) { proxy.scrollTo(bottomID, anchor: .bottom) }
                }
            }
            // An older page was added above: stay on the message the user was looking at
            .onChange(of: state.messages.first?.id) { _, _ in
                if let anchor = anchorBeforeOlder {
                    anchorBeforeOlder = nil
                    proxy.scrollTo(anchor, anchor: .top)
                }
            }
        }
    }

    private var inputBar: some View {
        HStack(alignment: .bottom, spacing: 8) {
            TextField("Message", text: $draft, axis: .vertical)
                .lineLimit(1...5)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                .onChange(of: draft) { _, new in
                    if new.count > maxMessageLength { draft = String(new.prefix(maxMessageLength)) }
                }

            Button {
                if onSend(draft) { draft = "" }
            } label: {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 34))
            }
            .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            .accessibilityLabel("Send")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(.bar)
    }
}

private struct MessageBubble: View {
    let row: MessageUi
    let onRetry: () -> Void
    let onDiscard: () -> Void

    // Kotlin enums arrive as classes with one static value per case, so compare with ==
    private var failed: Bool { row.status == SendStatus.failed }

    private var shape: UnevenRoundedRectangle {
        // The corner nearest the sender is tighter, like the tail of a speech bubble
        UnevenRoundedRectangle(
            topLeadingRadius: 18,
            bottomLeadingRadius: row.isMine ? 18 : 4,
            bottomTrailingRadius: row.isMine ? 4 : 18,
            topTrailingRadius: 18,
            style: .continuous
        )
    }

    private var background: Color {
        if failed { return Color.red.opacity(0.18) }
        return row.isMine ? Color.accentColor : Color(.secondarySystemBackground)
    }

    private var foreground: Color {
        if failed { return .primary }
        return row.isMine ? .white : .primary
    }

    var body: some View {
        VStack(alignment: row.isMine ? .trailing : .leading, spacing: 2) {
            Text(row.text)
                .foregroundStyle(foreground)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(background, in: shape)
                .contextMenu {
                    Button {
                        UIPasteboard.general.string = row.text
                    } label: {
                        Label("Copy", systemImage: "doc.on.doc")
                    }
                    if failed {
                        Button(action: onRetry) { Label("Retry", systemImage: "arrow.clockwise") }
                        Button(role: .destructive, action: onDiscard) { Label("Delete", systemImage: "trash") }
                    }
                }

            footer
        }
        .frame(maxWidth: .infinity, alignment: row.isMine ? .trailing : .leading)
        .padding(.leading, row.isMine ? 48 : 0)
        .padding(.trailing, row.isMine ? 0 : 48)
    }

    @ViewBuilder
    private var footer: some View {
        if failed {
            HStack(spacing: 8) {
                Image(systemName: "exclamationmark.circle.fill")
                    .foregroundStyle(.red)
                Text(row.failureReason ?? "Not sent")
                    .foregroundStyle(.red)
                Button("Retry", action: onRetry)
                Button("Delete", role: .destructive, action: onDiscard)
            }
            .font(.caption)
            .padding(.horizontal, 4)
        } else if row.status == SendStatus.sending {
            Text("Sending…")
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 4)
        } else if let time = row.time {
            Text(time)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 4)
        }
    }
}
