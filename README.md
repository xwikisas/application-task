## Task Manager Application

XWiki Task Manager Application let users create and assign tasks.

* Project Lead: [Alex Cotiugă](https://github.com/acotiuga)
* [Documentation](https://store.xwiki.com/xwiki/bin/view/Extension/TaskManagerApplication)
* Communication: [Forum and mailing list](http://dev.xwiki.org/xwiki/bin/view/Community/MailingLists), [chat](http://dev.xwiki.org/xwiki/bin/view/Community/IRC)
* [Development Practices](http://dev.xwiki.org)
* License: LGPL 2.1+
* Minimal XWiki version supported: XWiki 9.11
* Translations: N/A
* Sonar Dashboard: N/A
* Continuous Integration Status: [![Build Status](http://ci.xwikisas.com/view/All/job/xwikisas/job/application-task/job/master/badge/icon)](http://ci.xwikisas.com/view/All/job/xwikisas/job/application-task/job/master/)

# Release

```
mvn release:prepare -Pintegration-tests -DskipTests -Darguments="-N"
mvn release:perform -Pintegration-tests -DskipTests -Darguments="-DskipTests"
```