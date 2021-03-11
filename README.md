# e-mission phone android

[![GitHub](https://img.shields.io/github/license/ericafenyo/e-mission-phone-android)](LICENSE)

:construction: WORK IN PROGRESS :construction:

This project aims at repackaging the [e-mission][emission] cordova plugins to be compatible with native android apps.

**The e-mission plugins:**

https://github.com/e-mission/e-mission-data-collection<br>
https://github.com/e-mission/cordova-connection-settings<br>
https://github.com/e-mission/cordova-server-sync<br>
https://github.com/e-mission/cordova-server-communication<br>
https://github.com/e-mission/cordova-usercache<br>
https://github.com/e-mission/cordova-unified-logger<br>
https://github.com/e-mission/cordova-jwt-auth<br>


## Difficullties
1. Keeping the android modules in-sync (up to date) with the Cordova plugins.

We are still looking for a better way to achieve this. [Shankari](https://github.com/shankari), the author of the e-mission project, proposed generating JAR files for each plugins using GitHub actions.<br>
https://medium.com/@alexander.volminger/ci-cd-for-java-maven-using-github-actions-d009a7cb4b8f

Though we can solve the current situation with this method, I discovered that some plugins contain [androud resource] (https://developer.android.com/guide/topics/resources/providing-resources) files, and we can't easily store resources in the JAR file.
In the meantime, I will add the plugins as git submodules and pull updates if necessary. Then construct an android library module for each plugin.

> @see [Pushing AARs to Maven Central](https://chris.banes.dev/pushing-aars-to-maven-central/)

## License

This project is licensed under the BSD 3-Clause License - see the [LICENSE](LICENSE) file for details.

[version]: https://github.com/ericafenyo/tasti/releases
[emission]: https://github.com/e-mission
