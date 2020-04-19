package cqrs

import org.jetbrains.exposed.sql.Op
import kotlin.concurrent.thread

abstract class InMemoryEventAggregator {
    private var lastReadEventId: Int? = null

    fun start() {
        EventNotification.subscribe {
            readEvents()
        }
        readEvents()
    }

    private fun readEvents() {
        for (event in loadEvents()) {
            onEvent(event)
        }
    }

    @Synchronized
    private fun loadEvents(): List<Event> {
        val lastId = lastReadEventId
        val (events, maxId) = tx {
            val events = EEvent.find {
                if (lastId != null) {
                    DB.Events.id greater lastId
                } else {
                    Op.TRUE
                }
            }
            events.sortedBy { it.id }.map { it.event } to events.map { it.id.value }.max()
        }
        lastReadEventId = maxId
        return events
    }

    abstract fun onEvent(event: Event)
}