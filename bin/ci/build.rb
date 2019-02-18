require 'trent'

ci = Trent.new(color: :light_blue)

# Below is what JitPack runs when it wants to build a lib.
ci.sh('./gradlew assembleDebug')