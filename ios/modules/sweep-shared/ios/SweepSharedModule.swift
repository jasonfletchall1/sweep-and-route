import ExpoModulesCore
import WidgetKit

/// Bridges the App Group's UserDefaults to JS. The widget extension
/// (targets/widgets/SharedStore.swift) reads the same suite and keys.
public class SweepSharedModule: Module {
  private static let suiteName = "group.us.northlafayette.sweep"

  private var defaults: UserDefaults? {
    UserDefaults(suiteName: SweepSharedModule.suiteName)
  }

  public func definition() -> ModuleDefinition {
    Name("SweepShared")

    Function("get") { (key: String) -> String? in
      self.defaults?.string(forKey: key)
    }

    Function("set") { (key: String, value: String?) in
      guard let defaults = self.defaults else { return }
      if let value = value {
        defaults.set(value, forKey: key)
      } else {
        defaults.removeObject(forKey: key)
      }
    }

    Function("reloadWidgets") {
      WidgetCenter.shared.reloadAllTimelines()
    }
  }
}
