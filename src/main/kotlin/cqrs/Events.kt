package cqrs

interface Event

data class TicketBought(val ticket: Int, val time: Long) : Event

data class TicketProlonged(val ticket: Int, val time: Long) : Event

data class UserEnter(val ticket: Int, val time: Long) : Event

data class UserLeave(val ticket: Int, val time: Long) : Event