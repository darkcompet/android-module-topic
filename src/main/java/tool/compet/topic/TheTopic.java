/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */

package tool.compet.topic;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import tool.compet.core.DkRunner1;
import tool.compet.livedata.DkLiveEvent;

/**
 * Topic is a place to hold models for clients.
 *
 * By obtain a model from a topic, a client can put observable objects to communicate
 * with other clients via communication-method as LiveData, Eventbus,...
 *
 * @param <M> Model which holds various data for this topic.
 */
public abstract class TheTopic<M> {
	/**
	 * Subclass must provide how to make new topic.
	 */
	protected abstract M newModel();

	/**
	 * Called when this topic's resource is cleaned up.
	 * Subclass can release/close model's resource (Bitmap, IOStream,...) at this time.
	 */
	protected void onCleanupResource() {
	}

	// Unique id under the `host`
	String id;

	// Data of this topic
	@Nullable protected M model;

	// List of clients (Activity, Fragment,...) which observes this topic
	// We use it to manage active-state of this topic.
	final SimpleArrayMap<MyClient, ClientInfo> clients = new SimpleArrayMap<>();

	// Number of client which is owner of this topic.
	private int ownerCount;

	// Holds clients who is observing a mock.
	final SimpleArrayMap<String, DkLiveEvent<M>> mock2livedata = new SimpleArrayMap<>();

	/**
	 * Listen changes at given mockid.
	 *
	 * @param mockId It identities an action, should be unique string inside this topic, for eg,. `topic.on_new_picture`,...
	 */
	public void listen(String mockId, LifecycleOwner lifecycleOwner, Observer<M> observer) {
		DkLiveEvent<M> liveData = this.mock2livedata.get(mockId);
		if (liveData == null) {
			liveData = new DkLiveEvent<>();
			this.mock2livedata.put(mockId, liveData);
		}
		final String observerId = this.id + ":" + mockId;
		liveData.observe(lifecycleOwner, observerId, observer);
	}

	public void notifyListeners(String mockId) {
		notifyListeners(mockId, null);
	}

	/**
	 * Modify model and Notify the change to observers if and only if given mock exists.
	 *
	 * @param modifier To modify the model's attributes.
	 */
	public void notifyListeners(String mockId, @Nullable DkRunner1<M> modifier) {
		DkLiveEvent<M> liveData = this.mock2livedata.get(mockId);
		if (liveData != null) {
			final M model = model();
			// Apply new properties for model
			if (modifier != null) {
				modifier.run(model);
			}
			// Notify the change to observers
			liveData.sendValue(model);
		}
	}

	/**
	 * This will perform cleanup below staff inside this topic:
	 * - Unset the model.
	 * - Unset value in LiveData at all mocks.
	 */
	@MainThread
	public void cleanupResource() {
		this.onCleanupResource();
		this.model = null;
		for (int index = this.mock2livedata.size() - 1; index >= 0; --index) {
			this.mock2livedata.valueAt(index).unsetValue();
		}
	}

	/**
	 * Get or Create new model instance of given type at key as name of model.
	 */
	@MainThread
	public M model() {
		return this.model != null ? this.model : (this.model = newModel());
	}

	/**
	 * @return TRUE if given client is NOT found in clients.
	 */
	boolean addClient(MyClient client, boolean changeToOwner) {
		boolean newlyAdded = false;
		ClientInfo clientInfo = this.clients.get(client);

		if (clientInfo == null) {
			newlyAdded = true;
			clientInfo = new ClientInfo();

			this.clients.put(client, clientInfo);
		}

		if (changeToOwner) {
			if (! clientInfo.isTopicOwner) {
				++this.ownerCount;
			}
			clientInfo.isTopicOwner = true;
		}

		return newlyAdded;
	}

	/**
	 * @return TRUE if given client is existing in clients.
	 */
	boolean removeClient(MyClient client) {
		ClientInfo clientInfo = this.clients.remove(client);
		if (clientInfo != null && clientInfo.isTopicOwner) {
			--this.ownerCount;
		}
		return clientInfo != null;
	}

	int ownerCount() {
		return this.ownerCount;
	}

	static class ClientInfo {
		boolean isTopicOwner;
	}
}
