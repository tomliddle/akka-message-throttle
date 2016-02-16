
import akka.actor.{ActorSystem, ActorRef, Props}
import org.scalatest._
import akka.testkit._
import scala.concurrent.duration._
//import scala.language.implicitConversions

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
		}
	}

	private def createWorkLimiterActor(target: ActorRef): ActorRef = {
		TestActorRef(Props(new WorkLimiter[String](1 second, 1, target)))
	}

}