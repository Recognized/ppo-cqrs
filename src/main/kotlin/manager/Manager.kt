package manager

import cqrs.*
import kotlin.random.Random

data class TicketInfo(
    val usages: Int,
    val prolongations: Int,
    val firstUsage: Long?,
    val lastUsage: Long?,
    val boughtAt: Long
)

class Manager(private val clock: Clock) : InMemoryEventAggregator() {
    private val tickets = mutableMapOf<Int, TicketInfo>()

    override fun onEvent(event: Event) {
        when (event) {
            is TicketBought -> {
                tickets[event.ticket] = TicketInfo(
                    usages = 0,
                    prolongations = 0,
                    firstUsage = null,
                    lastUsage = null,
                    boughtAt = event.time
                )
            }
            is TicketProlonged -> {
                updateTicket(event.ticket) {
                    copy(prolongations = prolongations + 1)
                }
            }
            is UserEnter -> {
                updateTicket(event.ticket) {
                    copy(usages = usages + 1, firstUsage = firstUsage ?: event.time, lastUsage = event.time)
                }
            }
        }
    }

    // Compute next immutable state of ticket
    private fun updateTicket(ticket: Int, upd: TicketInfo.() -> TicketInfo) {
        tickets[ticket]?.let {
            tickets[ticket] = it.upd()
        }
    }

    fun buyTicket(): Int {
        var newTicket: Int? = null
        while (newTicket == null || newTicket in tickets) {
            newTicket = Random.nextInt()
        }
        postEvent(TicketBought(newTicket, clock.currentTimeMillis()))
        return newTicket
    }

    fun prolongTicket(ticket: Int) {
        if (ticket !in tickets) {
            error("Ticket does not exist")
        }
        postEvent(TicketProlonged(ticket, clock.currentTimeMillis()))
    }
}