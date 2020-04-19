package cqrs

import login.LoginAggregator
import manager.Manager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.Test
import report.Average
import report.Report
import java.sql.Connection
import kotlin.test.assertEquals
import kotlin.test.assertFails


class Test {

    @Before
    fun before() {
        database = Database.connect("jdbc:sqlite:test0", driver = "org.sqlite.JDBC").also {
            transaction(Connection.TRANSACTION_SERIALIZABLE, 3, it) {
                SchemaUtils.drop(DB.Events)
                SchemaUtils.createMissingTablesAndColumns(DB.Events)
            }
        }
    }


    @Test
    fun `test login and managing`() {
        val clock = ManagedClock()
        val ticketDuration = 100L
        val login = LoginAggregator(clock, ticketDuration)
        val manager = Manager(clock)

        manager.start()
        login.start()

        assertFails("Ticket does not exist") {
            login.enter(0)
        }

        val ticket1 = manager.buyTicket()

        assertFails("Can't leave fitness club without entering") {
            login.leave(ticket1)
        }

        login.enter(ticket1)

        // Can safely leave club
        login.leave(ticket1)

        // Next day, ticket expired
        clock.millis += ticketDuration

        assertFails {
            login.enter(ticket1)
        }

        manager.prolongTicket(ticket1)

        login.enter(ticket1)

        assertFails("Can't enter twice") {
            login.enter(ticket1)
        }
    }

    @Test
    fun `test statistics`() {

        val dayDuration = 100L
        val clock = ManagedClock()
        val login = LoginAggregator(clock, 100)
        val manager = Manager(clock)

        manager.start()
        login.start()
        // Assume day is 100 units
        val report = Report {
            "Day ${(it / dayDuration + 1)}"
        }

        val ticket1 = manager.buyTicket()
        val ticket2 = manager.buyTicket()
        val ticket3 = manager.buyTicket()

        repeat(10) {
            clock.millis++
            login.enter(ticket1)
            clock.millis++
            login.leave(ticket1)
        }

        repeat(5) {
            clock.millis++
            login.enter(ticket2)
            clock.millis++
            login.leave(ticket2)
        }

        // Let read past events
        report.start()

        repeat(2) {
            clock.millis++
            login.enter(ticket3)
            clock.millis++
            login.leave(ticket3)
        }

        repeat(20) {
            clock.millis += 10
            manager.prolongTicket(ticket1)
            login.enter(ticket1)
            clock.millis += 10
            login.leave(ticket1)

        }

        assertEquals(Average(2, 1.0), report.getAverageDuration(ticket3))
        val str = """
            Day 1 Ticket enter ($ticket1) -- 13 times
            Day 1 Ticket enter ($ticket2) -- 5 times
            Day 1 Ticket enter ($ticket3) -- 2 times
            Day 2 Ticket enter ($ticket1) -- 5 times
            Day 3 Ticket enter ($ticket1) -- 5 times
            Day 4 Ticket enter ($ticket1) -- 5 times
            Day 5 Ticket enter ($ticket1) -- 2 times
        """.trimIndent()
        assertEquals(str, report.getReport())
    }
}