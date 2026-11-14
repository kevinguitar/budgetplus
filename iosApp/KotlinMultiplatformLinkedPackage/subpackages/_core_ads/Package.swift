// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_core_ads",
  platforms: [
    .iOS("16.6")
  ],
  products: [
    .library(
      name: "_core_ads",
      type: .none,
      targets: ["_core_ads"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/googleads/swift-package-manager-google-mobile-ads.git",
      exact: "13.12.0"
    ),
    .package(
      url: "https://github.com/googleads/swift-package-manager-google-user-messaging-platform.git",
      exact: "3.1.0"
    )
  ],
  targets: [
    .target(
      name: "_core_ads",
      dependencies: [
        .product(
          name: "GoogleMobileAds",
          package: "swift-package-manager-google-mobile-ads"
        ),
        .product(
          name: "GoogleUserMessagingPlatform",
          package: "swift-package-manager-google-user-messaging-platform"
        )
      ]
    )
  ]
)
