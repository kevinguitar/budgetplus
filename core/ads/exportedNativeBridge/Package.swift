
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "exportedNativeBridge",
    platforms: [.iOS("16.6"),.macOS("10.13"),.tvOS("12.0"),.watchOS("4.0")],
    products: [
        .library(
            name: "exportedNativeBridge",
            type: .static,
            targets: ["exportedNativeBridge"])
    ],
    dependencies: [
        .package(url: "https://github.com/googleads/swift-package-manager-google-mobile-ads.git", exact: "13.2.0")
    ],
    targets: [
        .target(
            name: "exportedNativeBridge",
            dependencies: [
                .product(name: "GoogleMobileAds", package: "swift-package-manager-google-mobile-ads")
            ],
            path: "Sources"
            
            
        )
        
    ]
)
        