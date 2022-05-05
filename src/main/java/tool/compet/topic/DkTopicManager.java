/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */

package tool.compet.topic;

import androidx.annotation.MainThread;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

/**
 * TopicManager is a way to share data between Views (App, Activity, Fragment,...)
 * with considering app's configuration-changes.
 *
 * As we known, when app's configuration got changed, a View which is not retained will be destroyed,
 * All data associated with that View also be deleted and collected by GC (Garbge Collector) soon.
 *
 * To overcome config-change, this TopicManager provides non-config {@link MyHost} and {@link MyClient}
 * which can survive instances while config changed.
 *
 * To use this class, clients (App, Activity, Fragment,...) just refer to a topic, and obtain any model.
 * Note: this does not provide observation-mechanism on model, but clients can use put other objects of
 * publisher-consumer mechanism into model to archive it, for eg,. LiveData, Eventbus,....
 *
 * <p>
 * By default, `host` will listen `onCleared()` method on a client and perform unregister
 * when the client got destroyed.
 *
 * But to avoid reuse model of each topic since it is maybe not released, this also introduce a rule to cleanup
 * topic's resource and remove topic if needed. The rule is:
 * When register, each client must provide its role (`owner`, `viewer`) inside topic,
 * - Owner: topic will be removed from the host when all owners were destroyed.
 * - Viewer: if NO owner listen to the topic, resource of topic will be cleaned up if viewer-count down from 2 to 1.
 *
 * We recommend at least one client is owner of the topic by calling `ownTopic()` inside the View.
 * Other clients which just view topic should be considered as viewer by calling `viewTopic()` inside the View.
 *
 * # Ref: https://github.com/dhabensky/scoped-vm
 *
 * @param <T> Topic.
 */
@MainThread // Should be performed at main/ui thread
@SuppressWarnings("rawtypes")
public class DkTopicManager<T extends TheTopic> {
	private final ViewModelStoreOwner host;
	private final String topicId;
	private final Class<T> topicType;

	public DkTopicManager(ViewModelStoreOwner host, String topicId, Class<T> topicType) {
		this.host = host;
		this.topicId = topicId;
		this.topicType = topicType;
	}

	/**
	 * Make a client (activity, fragment,...) listen to the topic.
	 * If `changeToTopicOwner` is TRUE given, this changes role of client to `owner`.
	 *
	 * @param client Normally it is App, Activity, Fragment,...
	 * @param changeToOwner Set to TRUE to make the client is owner of the topic.
	 */
	public T registerClient(ViewModelStoreOwner client, boolean changeToOwner) {
		return nonConfigHost().registerClientAtTopic(topicId, topicType, nonConfigClient(client), changeToOwner);
	}

	/**
	 * Remove a client from the topic.
	 *
	 * @param client Normally it is activity, fragment.
	 */
	public void unregisterClient(ViewModelStoreOwner client) {
		nonConfigHost().unregisterClientAtTopic(topicId, nonConfigClient(client));
	}

	/**
	 * Obtain non-config instance for `host`.
	 * This instance is remained until the `host` got destroyed.
	 */
	private MyHost nonConfigHost() {
		return new ViewModelProvider(host).get(MyHost.class);
	}

	/**
	 * Obtain non-config instance for `client`.
	 * This instance is remained until the `client` got destroyed.
	 */
	private MyClient nonConfigClient(ViewModelStoreOwner client) {
		return new ViewModelProvider(client).get(MyClient.class);
	}
}
