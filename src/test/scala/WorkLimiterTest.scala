
import akka.actor.{ActorSystem, ActorRef, Props}
import org.scalatest._
import akka.testkit._
import scala.concurrent.duration._

class WorkLimiterTest extends TestKit(ActorSystem("system"))
	with DefaultTimeout
	with ImplicitSender
	with WordSpecLike
	with Matchers
	with BeforeAndAfterEach
	with BeforeAndAfterAll {

	"WorkLimiter" when {

		"running" should {

			"send a single message" in {
				val testProbe = TestProbe()
				val workLimiterActor = createWorkLimiterActor(testProbe.ref)

				workLimiterActor ! EnQueue("Test")
				testProbe.expectMsg("Test")

				testProbe.expectNoMsg()
			}

			"send and receive 3 messages" in {
				val testProbe = TestProbe()
				val workLimiterActor = createWorkLimiterActor(testProbe.ref)

				workLimiterActor ! EnQueue("Test1")
				workLimiterActor ! EnQueue("Test2")
				workLimiterActor ! EnQueue("Test3")
				testProbe.expectMsg("Test1")
				testProbe.expectMsg("Test2")
				testProbe.expectMsg("Test3")

				testProbe.expectNoMsg()
			}

			"send and receive 3 messages in a second" in {
				val testProbe = TestProbe()
				val workLimiterActor = createWorkLimiterActor(testProbe.ref)

				within (1000 milliseconds) {
					workLimiterActor ! EnQueue("Test1")
					workLimiterActor ! EnQueue("Test2")
					workLimiterActor ! EnQueue("Test3")
					workLimiterActor ! EnQueue("Test4")
					workLimiterActor ! EnQueue("Test5")
					testProbe.expectMsg("Test1")
					testProbe.expectMsg("Test2")
					testProbe.expectMsg("Test3")
					expectNoMsg()
				}
			}

			"send and receive 6 messages in two seconds" in {
				val testProbe = TestProbe()
				val workLimiterActor = createWorkLimiterActor(testProbe.ref)

				within(2000 milliseconds) {
					workLimiterActor ! EnQueue("Test1")
					workLimiterActor ! EnQueue("Test2")
					workLimiterActor ! EnQueue("Test3")
					workLimiterActor ! EnQueue("Test4")
					workLimiterActor ! EnQueue("Test5")
					workLimiterActor ! EnQueue("Test6")
					workLimiterActor ! EnQueue("Test7")
					testProbe.expectMsg("Test1")
					testProbe.expectMsg("Test2")
					testProbe.expectMsg("Test3")
					testProbe.expectMsg("Test4")
					testProbe.expectMsg("Test5")
					testProbe.expectMsg("Test6")
					expectNoMsg()
				}
			}

			"send 600 messages and receive 6 messages in two seconds" in {
				val testProbe = TestProbe()
				val workLimiterActor = createWorkLimiterActor(testProbe.ref)

				within (2000 milliseconds) {
					(1 to 600).foreach(i => workLimiterActor ! EnQueue(s"Test$i"))
					testProbe.expectMsg("Test1")
					testProbe.expectMsg("Test2")
					testProbe.expectMsg("Test3")
					testProbe.expectMsg("Test4")
					testProbe.expectMsg("Test5")
					testProbe.expectMsg("Test6")
					expectNoMsg()
				}
			}
		}
	}

	private def createWorkLimiterActor(target: ActorRef, sendLimit: Int = 3): ActorRef = {
		TestActorRef(Props(new WorkLimiter[String](1 second, sendLimit, target)))
	}

}