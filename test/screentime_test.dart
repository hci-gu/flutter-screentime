import 'package:flutter_test/flutter_test.dart';
import 'package:screentime/screentime.dart';
import 'package:screentime/screentime_platform_interface.dart';
import 'package:screentime/screentime_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockScreentimePlatform
    with MockPlatformInterfaceMixin
    implements ScreentimePlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final ScreentimePlatform initialPlatform = ScreentimePlatform.instance;

  test('$MethodChannelScreentime is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelScreentime>());
  });

  test('getPlatformVersion', () async {
    Screentime screentimePlugin = Screentime();
    MockScreentimePlatform fakePlatform = MockScreentimePlatform();
    ScreentimePlatform.instance = fakePlatform;

    expect(await screentimePlugin.getPlatformVersion(), '42');
  });
}
