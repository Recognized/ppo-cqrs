package report

import cqrs.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.concurrent.TimeUnit

data class Average(val count: Int, val average: Double)

private val format = DateTimeFormat.forPattern("dd-mm-YYYY")

class Report(val getDate: (Long) -> String = { format.print(DateTime(it)) }) : InMemoryEventAggregator() {
    private val clock = ManagedClock()
    private val eventStatistics =
        EventStatisticsImpl(clock, period = TimeUnit.DAYS.toMillis(31))
    private val usersInside = mutableMapOf<Int, Long>()
    private val averageDuration = mutableMapOf<Int, Average>()

    override fun onEvent(event: Event) {
        when (event) {
            is UserLeave -> {
                val enterTime = usersInside.remove(event.ticket)
                if (enterTime != null) {
                    val duration = event.time - enterTime
                    val previous = averageDuration[event.ticket] ?: Average(0, 0.0)
                    averageDuration[event.ticket] = previous.copy(
                        count = previous.count + 1,
                        average = (previous.count * previous.average + duration) / (previous.count + 1)
                    )
                }
            }
            is UserEnter -> {
                clock.millis = event.time
                val day = getDate(event.time)
                eventStatistics.incEvent("$day Ticket enter (${event.ticket})")
                usersInside[event.ticket] = event.time
            }
        }
    }

    fun getReport() = eventStatistics.getAllEventStatistics().joinToString(separator = "\n") {
        "${it.name} -- ${it.times} times"
    }

    fun getAverageDuration(ticket: Int) = averageDuration[ticket]
}