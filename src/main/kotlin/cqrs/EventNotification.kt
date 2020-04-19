package cqrs

object EventNotification {
    private val subscribers = mutableListOf<() -> Unit>()

    @Synchronized
    fun subscribe(callback: () -> Unit) {
        subscribers += callback
    }

    @Synchronized
    fun fire() {
        subscribers.forEach {
            it()
        }
    }
}