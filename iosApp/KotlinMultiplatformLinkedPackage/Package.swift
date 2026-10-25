// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("16.6")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      exact: "12.19.3"
    ),
    .package(
      url: "https://github.com/google/GoogleSignIn-iOS.git",
      exact: "10.1.0"
    ),
    .package(path: "subpackages/_core_ads")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(
          name: "FirebaseAnalytics",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseAuth",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseCore",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseCrashlytics",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseFirestore",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseFunctions",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseMessaging",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseRemoteConfig",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "GoogleSignIn",
          package: "GoogleSignIn-iOS"
        ),
        .product(name: "_core_ads", package: "_core_ads")
      ]
    )
  ]
)
