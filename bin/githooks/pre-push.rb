require 'trent'

ci = Trent.new(:color => :light_blue, :local => true)

# Assert example app builds
ci.sh('./gradlew :app:assembleDebug')
# Assert example app instrumentation tests build
ci.sh('./gradlew :app:assembleDebugAndroidTest')
# Assert unit tests build
ci.sh('./gradlew :teller:testDebugUnitTest')
# Assert instrumentation tests build 
ci.sh('./gradlew :teller:assembleDebugAndroidTest')
