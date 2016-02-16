package tomliddle.messagethrottle

import akka.actor.{ActorRef, FSM}
import tomliddle.messagethrottle.MessageThrottle._

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.reflect.ClassTag


object MessageThrottle {

	protected sealed trait State
	protected case class Work[T](queue: Queue[T], workLeft: Int)

	case class EnQueue[T](msg: T)
}

/**
  * Queues messages T and forwards them on at a rate of messageCount per duration to specified target actor
  *
  * @param duration duration of timer
  * @param messageCount number of messages per duration
  * @param target target actor
  * @tparam T Work message type to send
  */
class MessageThrottle[T: ClassTag](duration: FiniteDuration, messageCount: Int, target: ActorRef) extends FSM[State, Work[T]] {

	private case object TimePeriod
	private case object Idle extends MessageThrottle.State
	private case object Active extends MessageThrottle.State

	// Start in Idle state with an empty queue
	startWith(Idle, Work(Queue[T](), messageCount))

	// To avoid duplicating state for receiving an EnQueue message in both Idle and Active, use FSM.NullFunction
	when(Idle)(FSM.NullFunction)

	when(Active) {
		// New time period but no messages - enter idle state (resetting timer)
		case Event(TimePeriod, d @ Work(Queue(), _)) =>
			goto(Idle)

		// New time period, so send more messages
		case Event(TimePeriod, d @ Work(_, _)) =>
			stay using deliverMessages(d.copy(workLeft = messageCount))
	}

	whenUnhandled {
		// Common to all states - receive an enqueue work message
		case Event(EnQueue(msg: T), d @ Work(queue, _)) =>
			goto(Active) using deliverMessages(d.copy(queue = queue.enqueue(msg)))
	}

	onTransition {
		case Idle -> Active => startTimer()
		case Active -> Idle => stopTimer()
	}

	initialize()

	private def startTimer() = setTimer("scheduler", TimePeriod, duration, true)

	private def stopTimer() = cancelTimer("scheduler")

	private def deliverMessages(data: Work[T]): Work[T] = {
		val (toProcess, tail) = data.queue.splitAt(data.workLeft)

		toProcess.foreach(work => target ! work)

		data.copy(queue = tail, workLeft = data.workLeft - toProcess.length)
	}
}


