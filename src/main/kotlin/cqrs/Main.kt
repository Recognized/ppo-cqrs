package cqrs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object DB {

    object Events : IntIdTable() {
        val json = text("event")
        val type = text("type")
    }
}

class EEvent(id: EntityID<Int>) : IntEntity(id) {

    companion object : IntEntityClass<EEvent>(DB.Events)

    var json by DB.Events.json
    var type by DB.Events.type

    val event: Event by lazy {
        jacksonObjectMapper().registerModule(KotlinModule()).readValue(json, Class.forName(type)) as Event
    }
}


fun postEvent(event: Event) {
    val payload = jacksonObjectMapper().registerModule(KotlinModule()).writeValueAsString(event)
    tx {
        EEvent.new {
            json = payload
            type = event.javaClass.name
        }
    }
    EventNotification.fire()
}

fun <T> tx(fn: () -> T): T {
    return transaction(Connection.TRANSACTION_SERIALIZABLE, 3, database) {
        fn()
    }
}

var database: Database? = null
