**Install Android studio and Android SDK**

*Install Java, Android Studio, and Android SDK*
```
brew install caskroom/cask/brew-cask
brew cask install java
brew cask install android-studio
brew install android-sdk
```

*Setup your environment (ZSH)*
```
echo 'export STUDIO_JDK=/Library/Java/JavaVirtualMachines/jdk1.8.0_51.jdk' >> ~/.zshrc
export 'ANDROID_HOME=/usr/local/opt/android-sdk' >> ~/.zshrc
export 'PATH=$PATH:$ANDROID_HOME/bin:$ANDROID_HOME/tools' >> ~/.zshrc
```

**Install and run the app**


*Setup your Android device*

1. Plug your devices
2. Make sure it is recognised
```
adb devices
```

*Build the App*
```
./gradlew assembleDebug
```

*Install the App* (on real devices pluuged throuht USB - could not make it work with an AVD)
```
adb install -r app/build/outputs/apk/app-debug.apk 
```

*Start the App*  (on real devices pluuged throuht USB - could not make it work with an AVD)
```
adb shell am start -n com.thoughtworks.jdamore.androidfirst/com.thoughtworks.jdamore.androidfirst.FirstActivity
```
