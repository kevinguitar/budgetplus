package com.kevlina.budgetplus.core.common.nav

const val APP_DEEPLINK_HOST = "budgetplus.cchi.tw"
const val APP_DEEPLINK = "https://$APP_DEEPLINK_HOST"

const val APP_DEEPLINK_SCHEME = "budgetplus"
const val APP_DEEPLINK_CUSTOM = "$APP_DEEPLINK_SCHEME://"

/**
 * The list of accepted deeplink prefixes. Supports both the universal/app link
 * (https://budgetplus.cchi.tw/...) and the custom scheme (budgetplus://...).
 */
val APP_DEEPLINK_PREFIXES = listOf(APP_DEEPLINK, APP_DEEPLINK_CUSTOM)

const val NAV_RECORD_PATH = "record"
const val NAV_RECORD_WIDGET_PATH = "recordWidget"
const val NAV_JOIN_PATH = "join"
const val NAV_SETTINGS_PATH = "settings"
const val NAV_COLORS_PATH = "colors"
const val NAV_UNLOCK_PREMIUM_PATH = "unlockPremium"
const val NAV_OVERVIEW_PATH = "overview"
const val NAV_UI_TEST_PREMIUM_PATH = "uiTestPremium"