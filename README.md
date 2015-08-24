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
echo 'export STUDIO_JDK=/Library/Java/JavaVirtualMachines/<your_jdk>' >> ~/.zshrc
echo 'export ANDROID_HOME=/Users/<you>/Library/Android/sdk' >> ~/.zshrc
echo 'export PATH=$ANDROID_HOME/bin:$ANDROID_HOME/tools:$PATH' >> ~/.zshrc
```

**Install and run the app**


*On your physical Android device*

1. Plug your Android phone/tablet to the USB port
2. Make sure it is listed in the recognised devices
```
adb devices
```

*Build, install and run the App*
```
chmod +x ./go.sh
./go.sh
```
It will compile the app, install it on your device, and run it

*On the emulator*

1. To create a new Android Virtual Device (AVD), you need to launch the AVD manager
```
android avd
```

2. Click on the tab Devices Definition
3. Choose a device
4. Cick on the Create AVD... button on the left hand side
5. On the Create new Android Virtual Device (AVD screen)
	a. Give it a simple and meaningful name e.g. Nexus_9
	b. Choose a Target API Level 
	c. Choose a CPU/ABI
	d. Choose Skin: No Skin
	e. Select Use Host GPU
	f. Click OK
6. Close the AVD manager

*Build, install and run the App on the AVD*
```
chmod +x ./go-avd.sh
./go-avd.sh <avd-name> 
```
For instance: ./go-avd.sh Nexus_9.
It will compile the app, start the emulator with the specified AVD, wait 60 seconds, install the app, and run it