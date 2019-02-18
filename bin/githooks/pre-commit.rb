require 'trent'

ci = Trent.new(color: :light_blue, local: true)

ci.sh('./gradlew dokka')
ci.sh('git add docs/')