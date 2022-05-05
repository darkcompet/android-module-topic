/*
 * Copyright (c) 2017-2021 DarkCompet. All rights reserved.
 */

package tool.compet.topic;

import androidx.collection.ArraySet;
import androidx.lifecycle.ViewModel;

import java.util.Set;

/**
 * Client is non-config object (ViewModel) of a View (Activity, Fragment...),
 * also is used to notify destroy-event to listeners.
 * <p>
 * Normally, each client connect to different topics at different scope. And a client
 * object is not recreated until the View got destroyed. So,
 * -> Should NOT hold data which be modified at 2 topics, for eg,. `isTopicOwner`,...
 */
public class MyClient extends ViewModel { // MUST be public for creation
	public interface Listener {
		void onClientClosed(MyClient client);
	}

	// Normally, they are `host`s
	private final Set<Listener> listeners = new ArraySet<>();

	@Override
	protected void onCleared() {
		super.onCleared();

		for (Listener listener : listeners) {
			listener.onClientClosed(this);
		}

		listeners.clear();
	}

	boolean addListener(Listener listener) {
		return listeners.add(listener);
	}

	boolean removeListener(Listener listener) {
		return listeners.remove(listener);
	}
}
