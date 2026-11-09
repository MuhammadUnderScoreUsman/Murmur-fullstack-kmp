import SwiftUI

/// The Murmur mark on a 108-unit grid: two speech bubbles. `kind` picks which one.
private struct BubbleShape: Shape {
    enum Kind { case back, front }
    let kind: Kind

    func path(in rect: CGRect) -> Path {
        let s = rect.width / 108
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: rect.minX + x * s, y: rect.minY + y * s) }
        func box(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect {
            CGRect(x: rect.minX + x * s, y: rect.minY + y * s, width: w * s, height: h * s)
        }

        var path = Path()
        switch kind {
        case .back:
            path.addRoundedRect(in: box(44, 40, 42, 34), cornerSize: CGSize(width: 14 * s, height: 14 * s))
            path.move(to: p(62, 72)); path.addLines([p(78, 72), p(80, 84)]); path.closeSubpath()
        case .front:
            path.addRoundedRect(in: box(22, 26, 46, 34), cornerSize: CGSize(width: 14 * s, height: 14 * s))
            path.move(to: p(32, 58)); path.addLines([p(46, 58), p(32, 70)]); path.closeSubpath()
        }
        return path
    }
}

/// Murmur's iOS splash: a light, airy screen with soft gradient bubbles that float up into place, a gentle
/// "typing" ripple, and a rounded wordmark. No assets: the logo is drawn in code.
///
/// Show it while the session is loading, e.g. in your root view:
///
///     ZStack {
///         content
///         if session.state == .loading { SplashView().transition(.opacity) }
///     }
///     .animation(.easeInOut(duration: 0.35), value: session.state == .loading)
struct SplashView: View {
    private let size: CGFloat = 128
    private let indigo = Color(red: 0.31, green: 0.27, blue: 0.90)
    private let sky = Color(red: 0.42, green: 0.62, blue: 1.0)

    @State private var backIn = false
    @State private var frontIn = false
    @State private var textIn = false
    @State private var ripple = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(.systemBackground), Color(red: 0.93, green: 0.94, blue: 1.0)],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 22) {
                ZStack {
                    BubbleShape(kind: .back)
                        .fill(LinearGradient(colors: [sky.opacity(0.9), sky.opacity(0.55)], startPoint: .top, endPoint: .bottom))
                        .offset(y: backIn ? 0 : 40)
                        .opacity(backIn ? 1 : 0)

                    ZStack {
                        BubbleShape(kind: .front)
                            .fill(LinearGradient(colors: [indigo, Color(red: 0.45, green: 0.35, blue: 1.0)], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .shadow(color: indigo.opacity(0.35), radius: 14, y: 8)

                        HStack(spacing: size * 2.4 / 108) { // dot centres are 8 units apart, each dot is 5.6 wide
                            ForEach(0..<3, id: \.self) { i in
                                Circle()
                                    .fill(.white)
                                    .frame(width: size * 5.6 / 108, height: size * 5.6 / 108)
                                    .offset(y: ripple ? -size * 0.045 : 0)
                                    .animation(
                                        .easeInOut(duration: 0.45).repeatForever(autoreverses: true).delay(Double(i) * 0.14),
                                        value: ripple
                                    )
                            }
                        }
                        // dots sit at (45, 43) on the grid; the grid centre is (54, 54)
                        .offset(x: -size * 9 / 108, y: -size * 11 / 108)
                    }
                    .offset(y: frontIn ? 0 : 56)
                    .opacity(frontIn ? 1 : 0)
                }
                .frame(width: size, height: size)

                VStack(spacing: 4) {
                    Text("Murmur")
                        .font(.system(size: 32, weight: .semibold, design: .rounded))
                        .foregroundStyle(Color(red: 0.12, green: 0.11, blue: 0.30))
                    Text("Say it softly")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                .opacity(textIn ? 1 : 0)
                .offset(y: textIn ? 0 : 8)
            }
        }
        .onAppear {
            withAnimation(.spring(response: 0.7, dampingFraction: 0.78)) { backIn = true }
            withAnimation(.spring(response: 0.7, dampingFraction: 0.78).delay(0.14)) { frontIn = true }
            withAnimation(.easeOut(duration: 0.55).delay(0.45)) { textIn = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) { ripple = true }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Murmur, loading")
    }
}
