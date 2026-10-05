package com.jarvisai

/** Tracks the current plan without granting any extra Android privilege. */
class ExecutionSession {
    var command: String = ""
        private set
    var steps: List<ActionStep> = emptyList()
        private set
    var currentIndex: Int = -1
        private set
    var lastStatus: Status = Status.IDLE
        private set

    enum class Status { IDLE, RUNNING, SUCCEEDED, FAILED, WAITING_FOR_CONFIRMATION }

    fun begin(command: String, steps: List<ActionStep>) {
        this.command = command
        this.steps = steps
        currentIndex = if (steps.isEmpty()) -1 else 0
        lastStatus = if (steps.isEmpty()) Status.FAILED else Status.RUNNING
    }

    fun advance() {
        if (currentIndex >= 0) currentIndex++
        if (currentIndex >= steps.size) lastStatus = Status.SUCCEEDED
    }

    fun fail() { lastStatus = Status.FAILED }
    fun waitingForConfirmation() { lastStatus = Status.WAITING_FOR_CONFIRMATION }
    fun reset() { command = ""; steps = emptyList(); currentIndex = -1; lastStatus = Status.IDLE }
}
