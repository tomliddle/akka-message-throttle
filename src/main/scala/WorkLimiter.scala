import akka.actor.{ActorRef, FSM}
import scala.collection.immutable.Queue
import scala.concurrent.duration._


sealed trait State
case object Idle extends State
case object Active extends State


case class Work[T](queue: Queue[T], workLeft: Int)
case class EnQueue[T](msg: T)
case object TimePeriod


class WorkLimiter[T](duration: FiniteDuration, workAmount: Int, target: ActorRef) extends FSM[State, Work[T]] {

	startWith(Idle, Work(Queue[T](), workAmount))

	when(Idle)(FSM.NullFunction)

	when(Active) {
		// New time period but no messages
		case Event(TimePeriod, d @ Work(Queue(), _)) =>
			goto(Idle)

		// New time period, so send more messages
		case Event(TimePeriod, d @ Work(_, _)) =>
			stay using deliverMessages(d.copy(workLeft = workAmount))
	}

	whenUnhandled {
		// Common to all states - receive a message
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

		toProcess.foreach(str => target ! str)

		data.copy(queue = tail, workLeft = data.workLeft - toProcess.length)
	}
}


