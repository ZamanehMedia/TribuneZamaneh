## Dev Setup

Follow these steps to setup your dev environment:

1. Checkout paik git repo
2. Init and update git submodules

    git submodule update --init --recursive

3. Build Project

   ./gradlew assembleDebug (for all debug builds)
   	     or
   ./gradlew assembleMaster (for all builds of the flavor "master")

   For a list of tasks that can be used above, see "./gradlew tasks". 
