import SwiftUI


struct AvatarView: View {
    let name: String
    var size: CGFloat = 48
    var showsOnlineDot = false

    private static let palette: [Color] = [.blue, .green, .orange, .pink, .purple, .teal, .indigo, .red]

    private var color: Color {
        // Not hashValue: Swift randomises it on every launch, which would change colours each run
        let sum = name.unicodeScalars.reduce(UInt32(0)) { $0 &+ $1.value }
        return Self.palette[Int(sum % UInt32(Self.palette.count))]
    }

    var body: some View {
        Text(String(name.prefix(1)).uppercased())
            .font(.system(size: size * 0.42, weight: .semibold, design: .rounded))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(color.gradient, in: Circle())
            .overlay(alignment: .bottomTrailing) {
                if showsOnlineDot {
                    // The ring in the screen's background colour separates the dot from the avatar
                    Circle()
                        .fill(Color.green)
                        .frame(width: size * 0.3, height: size * 0.3)
                        .overlay(Circle().stroke(Color(.systemBackground), lineWidth: 2.5))
                        .offset(x: 2, y: 2)
                }
            }
            .accessibilityHidden(true)
    }
}
