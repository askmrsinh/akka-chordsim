package worker

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._

import scala.collection.mutable
import scala.concurrent.duration._

/**
 * The worker is actually more of a middle manager, delegating the actual work
 * to the WorkExecutor, supervising it and keeping itself available to interact with the work master.
 */
object Worker {

  var id = 0
  var ft = new mutable.HashMap[String, String]()
  var workers = 1

  def props(masterProxy: ActorRef,
            d: String, fingerTable: mutable.HashMap[String, String], workers: Int): Props = Props(
    new Worker(masterProxy, id.toString, fingerTable, workers))

}

class Worker(masterProxy: ActorRef, id: String, ft: mutable.HashMap[String, String], workers: Int)
  extends Actor with Timers with ActorLogging {
  import MasterWorkerProtocol._
  import context.dispatcher

  val workerId: String = id
  var fingerTable: mutable.HashMap[String, String] = ft
  log.info("Worker {}, {}, Fingertable: {}", id, self.path, ft)

  val registerInterval: FiniteDuration = context.system.settings.config.getDuration("distributed-workers.worker-registration-interval").getSeconds.seconds

  val registerTask: Cancellable = context.system.scheduler.schedule(0.seconds, registerInterval, masterProxy, RegisterWorker(workerId))

  val workExecutor: ActorRef = createWorkExecutor()

  var currentWorkId: Option[String] = None
  def workId: String = currentWorkId match {
    case Some(workId) => workId
    case None         => throw new IllegalStateException("Not working")
  }

  def receive: Receive = idle

  def idle: Receive = {
    case WorkIsReady =>
      // this is the only state where we reply to WorkIsReady
      masterProxy ! WorkerRequestsWork(workerId)

    case Work(workId, job: Int) =>
      // TODO: Fix this
      val workIdHash = (workId.toArray.foldLeft(0)(_ + _.toInt) % workers).toString
      print("TESTING", workId, job, workerId, workIdHash)
      if (workIdHash == workerId) {
        log.info("Worker {}, Got work: {}, {}", workerId, job, workId)
        currentWorkId = Some(workId)
        workExecutor ! WorkExecutor.DoWork(job)
        context.become(working)
      } else {
        log.info("Worker {}, Forwarded work: {}, {} to worker: {}", workerId, job, workId, workIdHash)
        val toWorker = context.actorSelection(s"../worker-$workIdHash")
        toWorker ! Work(workId, job)
      }
  }

  def working: Receive = {
    case WorkExecutor.WorkComplete(result) =>
      log.info("Worker {}, work {} is complete. Result {}.", workerId, workId, result)
      masterProxy ! WorkIsDone(workerId, workId, result)
      context.setReceiveTimeout(5.seconds)
      context.become(waitForWorkIsDoneAck(result))

    case _: Work =>
      log.warning("Worker {}, Yikes. Master told me to do work, while I'm already working.", workerId)

  }

  def waitForWorkIsDoneAck(result: Any): Receive = {
    case Ack(id) if id == workId =>
      masterProxy ! WorkerRequestsWork(workerId)
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case ReceiveTimeout =>
      log.info("Worker {}, No ack from master, resending work {} result", workerId, workId)
      masterProxy ! WorkIsDone(workerId, workId, result)

  }

  def createWorkExecutor(): ActorRef =
    // in addition to starting the actor we also watch it, so that
    // if it stops this worker will also be stopped
    context.watch(context.actorOf(WorkExecutor.props, "work-executor"))

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: Exception =>
      currentWorkId foreach { workId => masterProxy ! WorkFailed(workerId, workId) }
      context.become(idle)
      Restart
  }

  override def postStop(): Unit = {
    registerTask.cancel()
    masterProxy ! DeRegisterWorker(workerId)
  }

}