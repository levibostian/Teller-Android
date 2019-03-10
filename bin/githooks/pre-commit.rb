require 'trent'

ci = Trent.new(color: :light_blue, local: true)

ci.sh('./gradlew :teller-android:dokka')
ci.sh('git add docs/')
