Pod::Spec.new do |s|
  s.name           = 'SweepShared'
  s.version        = '1.0.0'
  s.summary        = 'App Group shared storage + WidgetKit reload bridge for Sweep'
  s.description    = 'Reads/writes the App Group UserDefaults shared with the Sweep widget extension.'
  s.author         = 'Jason Fletchall'
  s.homepage       = 'https://github.com/jasonfletchall1/sweep-and-route'
  s.license        = 'MIT'
  s.platforms      = { :ios => '17.0' }
  s.source         = { git: '' }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'
  s.frameworks = 'WidgetKit'

  # Swift/Objective-C compatibility
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
  }

  s.source_files = "**/*.{h,m,mm,swift,hpp,cpp}"
end
