import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'screentime_method_channel.dart';

abstract class ScreentimePlatform extends PlatformInterface {
  /// Constructs a ScreentimePlatform.
  ScreentimePlatform() : super(token: _token);

  static final Object _token = Object();

  static ScreentimePlatform _instance = MethodChannelScreentime();

  /// The default instance of [ScreentimePlatform] to use.
  ///
  /// Defaults to [MethodChannelScreentime].
  static ScreentimePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [ScreentimePlatform] when
  /// they register themselves.
  static set instance(ScreentimePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
