if git.modified_files.include? "build.gradle" or git.modified_files.include? "teller/build.gradle"
   warn "I see you edited a `build.gradle` file. Keep in mind that unless you are simply upgrading version numbers of libraries, that is ok. If you are adding dependencies, you may get your PR denied to keep the library slim."
end

if github.branch_for_base != "development" && github.pr_author != "levibostian"
  fail "Sorry, wrong branch. Close this pull request and then create a new pull request into the `development` branch instead."
end

if github.branch_for_base == "master" && github.branch_for_head != "development"
  fail "You must merge from the `development` branch into `master`."
end

if github.branch_for_base == "master"
  if github.pr_body.include? "#non-release"
    if !git.modified_files.include? "teller/*"
      warn 'You edited some files in `teller/`. Are you sure this is a #non-release?'
    end    
  else
    if !git.modified_files.include? "CHANGELOG.md"
      fail 'You need to edit the CHANGELOG.md file.'
    end
    android_version_change.assert_version_name_changed("teller/build.gradle")
  end
end
