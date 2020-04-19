package login

import cqrs.*

class LoginAggregator(
    private val clock: Clock,
    private val ticketDuration: Long
) : InMemoryEventAggregator() {
    private val tickets = mutableMapOf<Int, LongRange>()
    private val logined = mutableSetOf<Int>()

    override fun onEvent(event: Event) {
        when (event) {
            is TicketBought -> {
                tickets[event.ticket] = event.time until event.time + ticketDuration
            }
            is TicketProlonged -> {
                tickets[event.ticket] = event.time until event.time + ticketDuration
            }
            is UserEnter -> {
                logined += event.ticket
            }
            is UserLeave -> {
                logined -= event.ticket
            }
        }
    }

    fun enter(ticket: Int) {
        if (ticket in logined) {
            error("Already in")
        }
        val activeTicket = tickets[ticket] ?: error("Ticket does not exist")
        if (clock.currentTimeMillis() !in activeTicket) {
            error("Ticket expired")
        }
        postEvent(UserEnter(ticket, clock.currentTimeMillis()))
    }

    fun leave(ticket: Int) {
        if (ticket !in logined) {
            error("Not entered!")
        }
        postEvent(UserLeave(ticket, clock.currentTimeMillis()))
    }
}