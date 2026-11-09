import Foundation

enum AppConfig {
    /// Real iPhone: your computer's LAN IP, same Wi-Fi. Keep the trailing slash.
//    private static let deviceBaseURL = "http://192.168.1.20:8080/"

    /// The simulator shares your Mac's network, so localhost works there.
    static var baseURL: String {
        #if targetEnvironment(simulator)
        return "see setup, your local ip goes here"
        #else
        return deviceBaseURL
        #endif
    }
}
