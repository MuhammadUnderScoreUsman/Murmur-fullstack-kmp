import SharedLogic

final class KotlinObserver {
    private let dispose: () -> Void

    init<State>(
        watch: (@escaping (State) -> Void) -> () -> Void,
        onChange: @escaping @MainActor (State) -> Void
    ) {
        // Kotlin always calls back on the main thread (its scope uses Dispatchers.Main.immediate)
        dispose = watch { state in
            MainActor.assumeIsolated { onChange(state) }
        }
    }

    deinit { dispose() }
}
extension ContactItem: @retroactive Identifiable {}
extension MessageUi: @retroactive Identifiable {}
