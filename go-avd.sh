./gradlew clean assembleDebug
emulator -avd $1 -netspeed full -netdelay none &
sleep 60
adb -s emulator-5554 install -r app/build/outputs/apk/app-debug.apk
adb shell am start -n com.thoughtworks.jdamore.androidfirst/com.thoughtworks.jdamore.androidfirst.BluetoothActivity
