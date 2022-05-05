/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */

package tool.compet.topic;

import androidx.collection.SimpleArrayMap;
import androidx.lifecycle.ViewModel;

import tool.compet.core.DkLogcats;
import tool.compet.core.DkRuntimeException;

/**
 * This is subclass of {@link ViewModel}, is stored in a {@link androidx.lifecycle.ViewModelStoreOwner}
 * (for eg,. {@link androidx.fragment.app.FragmentActivity}, {@link androidx.fragment.app.Fragment}...).
 *
 * Design principle:
 * <ul>
 *    <li> Each View (app, activity, fragment,...) will contain only a Host.
 *    <li> Each Host holds multiple topics. A topic holds multiple clients.
 *    <li> When a Client got destroyed, it must notify to the Host disconnect-event.
 *    <li> When the Host got destroyed, all topics will be removed and cleaned up.
 * </ul>
 */
@SuppressWarnings("unchecked")
public class MyHost extends ViewModel implements MyClient.Listener {
	// All topics (topicId vs topic) under this host
	private final SimpleArrayMap<String, TheTopic<?>> allTopics = new SimpleArrayMap<>();

	/**
	 * Get or Create new topic from given `topicId`.
	 */
	<T extends TheTopic<?>> T obtainTopic(String topicId, Class<T> topicType) {
		try {
			TheTopic<?> topic = allTopics.get(topicId);
			if (topic == null) {
				topic = topicType.newInstance();
				topic.id = topicId;

				allTopics.put(topicId, topic);
			}
			return (T) topic;
		}
		catch (Exception e) {
			throw new DkRuntimeException(this, e, "Could not create new topic: " + topicType.getName());
		}
	}

	/**
	 * Register a client.
	 *
	 * @param topicId Id of target topic which client will be added.
	 * @param client For eg,. activity or fragment...
	 */
	<T extends TheTopic<?>> T registerClientAtTopic(String topicId, Class<T> topicType, MyClient client, boolean changeToOwner) {
		T topic = obtainTopic(topicId, topicType);

		// Make host listen changes of the client.
		// When a client was destroyed, all topics maybe afftected.
		client.addListener(this);

		// Add client to topic
		topic.addClient(client, changeToOwner);

		return topic;
	}

	/**
	 * Unregister a client.
	 */
	void unregisterClientAtTopic(String topicId, MyClient client) {
		// Just remove client from the topic, but don't stop listen client from host
		// since the host still need to listen destroy-event of this client to update topics.
		TheTopic<?> topic = allTopics.get(topicId);
		if (topic != null) {
			removeClientFromTopic(topic, client);
		}
	}

	// Called when a client was cleared (killed)
	@Override
	public void onClientClosed(MyClient client) {
		// Now this is time to stop listen client from host
		client.removeListener(this);

		// Loop over topics to remove the client
		for (int index = allTopics.size() - 1; index >= 0; --index) {
			removeClientFromTopic(allTopics.valueAt(index), client);
		}
	}

	private void removeClientFromTopic(TheTopic<?> topic, MyClient client) {
		final int prevClientCount = topic.clients.size();
		final int prevOwnerCount = topic.ownerCount();

		// Remove client from topic (maybe false if not found that client in the topic).
		// After removed, we also try perform cleanup topic, or remove topic from host
		if (topic.removeClient(client)) {
			final int curClientCount = topic.clients.size();
			final int curOwnerCount = topic.ownerCount();

			// Client: 2+ -> 1-, or Owner: 1+ -> 0-
			if ((prevClientCount >= 2 && curClientCount <= 1) || (prevOwnerCount >= 1 && curOwnerCount <= 0)) {
				topic.cleanupResource();

				if (BuildConfig.DEBUG) {
					DkLogcats.info(this, "Resource of topic `%s` was cleaned up ! ClientCount: %d -> %d, OwnerCount: %d -> %d",
						topic.id, prevClientCount, curClientCount, prevOwnerCount, curOwnerCount);
				}
			}

			// Client: 1- -> 0
			if (curClientCount <= 0) {
				allTopics.remove(topic.id);

				if (BuildConfig.DEBUG) {
					DkLogcats.info(this, "Topic `%s` was removed from host ! ClientCount: %d -> %d",
						topic.id, prevClientCount, curClientCount);
				}
			}
		}
	}

	// Called when this host was closed (killed)
	@Override
	protected void onCleared() {
		super.onCleared();

		// Cleanup host and all topics
		// We don't need unregister host from clients since
		// this is called after all clients was left (destroyed)
		for (int index = allTopics.size() - 1; index >= 0; --index) {
			allTopics.valueAt(index).cleanupResource();
		}
		allTopics.clear();

		if (BuildConfig.DEBUG) {
			DkLogcats.info(this, "All topics was removed since host was destroyed");
		}
	}

	/**
	 * Just clear resource of given topic.
	 */
	public void cleanupTopic(String topicId) {
		TheTopic<?> topic = allTopics.get(topicId);

		if (topic != null) {
			topic.cleanupResource();

			if (BuildConfig.DEBUG) {
				DkLogcats.info(this, "Resource of topic `%s` was cleaned up as caller's request", topicId);
			}
		}
	}

	/**
	 * Remove topic and Clear its resource.
	 */
	public void closeTopic(String topicId) {
		TheTopic<?> topic = allTopics.get(topicId);
		if (topic != null) {
			topic.cleanupResource();
			allTopics.remove(topicId);

			if (BuildConfig.DEBUG) {
				DkLogcats.info(this, "Topic `%s` was closed (remove + cleanup) as caller's request", topicId);
			}
		}
	}
}
