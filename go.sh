./gradlew assembleDebug
adb install -r app/build/outputs/apk/app-debug.apk
adb shell am start -n com.thoughtworks.jdamore.androidfirst/com.thoughtworks.jdamore.androidfirst.FirstActivity
