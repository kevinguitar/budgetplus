
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
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", exact: "12.10.0")
    ],
    targets: [
        .target(
            name: "exportedNativeBridge",
            dependencies: [
                .product(name: "FirebaseAnalytics", package: "firebase-ios-sdk"),.product(name: "FirebaseFirestore", package: "firebase-ios-sdk")
            ],
            path: "Sources"
            
        )
        
    ]
)
        