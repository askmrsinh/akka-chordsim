package worker

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._

/**
 * Dummy front-end that periodically sends a workload to the master.
 */
object FrontEnd {

  var id = 3000
  def props: Props = Props(new FrontEnd(id.toString))

  private case object NotOk
  private case object Tick
  private case object Retry
}

// #front-end
class FrontEnd(id: String) extends Actor with ActorLogging with Timers {
  import FrontEnd._
  import context.dispatcher

  val masterProxy: ActorRef = context.actorOf(
    MasterSingleton.proxyProps(context.system),
    name = "masterProxy")

  val frontEndId: String = id
  var workCounter = 0

  def nextWorkId(): String = UUID.randomUUID().toString

  override def preStart(): Unit = {
    timers.startSingleTimer("tick", Tick, 5.seconds)
  }

  def receive: Receive = idle

  def idle: Receive = {
    case Tick =>
      workCounter += 1
      FrontEnd.props.args.foreach(_ => print(_)
      )
      log.info("FrontEnd {} produced work: {}", frontEndId, workCounter)
      val work = Work(nextWorkId(), workCounter)
      context.become(busy(work))
  }

  def busy(workInProgress: Work): Receive = {
    sendWork(workInProgress)

    {
      case Master.Ack(workId) =>
        log.info("FrontEnd {} got ack for workId {}", frontEndId, workId)
        val nextTick = ThreadLocalRandom.current.nextInt(3, 10).seconds
        timers.startSingleTimer(s"tick", Tick, nextTick)
        context.become(idle)

      case NotOk =>
        log.info("FrontEnd {}, work {} not accepted, retry after a while", frontEndId, workInProgress.workId)
        timers.startSingleTimer("retry", Retry, 3.seconds)

      case Retry =>
        log.info("FrontEnd {}, Retrying work {}", frontEndId, workInProgress.workId)
        sendWork(workInProgress)
    }
  }

  def sendWork(work: Work): Unit = {
    log.info("FrontEnd {} sent work: {}", frontEndId, workCounter)
    implicit val timeout: Timeout = Timeout(5.seconds)
    (masterProxy ? work).recover {
      case _ => NotOk
    } pipeTo self
  }

}
// #front-end