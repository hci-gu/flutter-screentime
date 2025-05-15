import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'screentime_platform_interface.dart';

/// An implementation of [ScreentimePlatform] that uses method channels.
class MethodChannelScreentime extends ScreentimePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('screentime');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
