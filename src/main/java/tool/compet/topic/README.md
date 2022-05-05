# LiveTopic for Android

Like with Eventbus or LiveData, this package provides publisher-consumer mechanism,
to help Views (App, Activity, Fragment,..) can communicate with each other.

The specialness of this is, it can retrieve topic instance via configuration-changes which
destroy instance of Views. The topic will be deleted only when the associated View was destroyed
by the app (not configuration changes).

By using this, we can keep our data not be deleted on configuration changed. But we need take
care a bit about amount of data which be held inside a topic.
Because in mobile app development world, we always must care about memory-leak, memory-lack problems,
so by use some caching method, we can overcome them.


## Dependencies

- tool.compet.livedata
- tool.compet.core
