package app.noor.prayer.core.media
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class PlaybackFeedback @Inject constructor() {
    val errors = MutableSharedFlow<Int>(extraBufferCapacity = 1)
}
