package tool.compet.topic;

import androidx.lifecycle.ViewModelStoreOwner;

public interface DkTopicProvider {
	<T extends TheTopic<?>> T topic(String topicId, Class<T> topicType);

	<T extends TheTopic<?>> T topic(String topicId, Class<T> topicType, ViewModelStoreOwner scope);
}
