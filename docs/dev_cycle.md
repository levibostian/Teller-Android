# Dev cycle

For contributors to the Teller-Android project, this is the current development cycle followed to get code built, tested, merged, and released for all to enjoy.

* Any new feature, bug, fix, etc. done to the code base, make a branch off of the `development` branch.

* After done with your code changes, make a pull request into the `development` branch.

A CI server is configured for Teller to build and test the project. You will see the status of the tests via the GitHub pull request you create.

**For people who are not core contributors of the Teller-Android project, your dev cycle ends here as the public does not have a way to deploy the code. If you are curious what magic goes on behind the scenes, read on!**

* After the development branch is full of commits that we want to turn into a new release, create a new branch off of `development` and make the appropriate changes to it to prepare for a release:

1. Update the `teller/build.gradle` file to change the `ext -> libraryVersion` value to the version of the next release.
2. Add an entry to `CHANGELOG.md` with release notes.
3. Make a pull request into `development` branch.

* Make a pull request from `development` into the `master` branch.

* Off of the newly updated `master` branch, create a new git tag for the new release:

```
$> git tag -as 0.1.0-alpha
```

When the text editor pops up to enter release notes, I like to copy/paste the CHANGELOG.md entry in there.

Push up the new git tag: `git push --tags`. Then, make a new GitHub release for this new git tag.

* Next up is to make a release of the library to jCenter for all people to enjoy.

If you have not already, add the following entries to your `local.properties` file in the root directory for Teller-Android:

```
bintray.user=levibostian
bintray.apikey=your-api-key-here
```

Find your api key [here](https://bintray.com/profile/edit) once you are logged into bintray.

*Reminder: Your `local.properties` file should be added to your root `.gitignore` file.

Then, lets build and upload to bintray:

```
$> ./gradlew clean install
$> ./gradlew bintrayUpload
```

Success!