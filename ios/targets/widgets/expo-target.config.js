/** @type {import('@bacons/apple-targets/app.plugin').ConfigFunction} */
module.exports = (config) => ({
  type: 'widget',
  name: 'SweepWidgets',
  displayName: 'Sweep',
  // Appended to the app's bundle id → us.northlafayette.sweep.widgets
  bundleIdentifier: '.widgets',
  // Interactive widgets (Button(intent:)) need iOS 17.
  deploymentTarget: '17.0',
  icon: '../../assets/icon.png',
  frameworks: ['SwiftUI', 'WidgetKit', 'AppIntents'],
  colors: {
    $accent: '#143B7B',
  },
  // Same App Group as the main app, so the widgets read the token and
  // database choices the app stores (modules/sweep-shared).
  entitlements: {
    'com.apple.security.application-groups': config.ios.entitlements['com.apple.security.application-groups'],
  },
});
