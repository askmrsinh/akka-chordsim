package worker

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator

object WorkResultConsumer {
  def props: Props = Props(new WorkResultConsumer)
}

// #work-result-consumer
class WorkResultConsumer extends Actor with ActorLogging {

  val mediator: ActorRef = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(Master.ResultsTopic, self)

  def receive: PartialFunction[Any, Unit] = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Consumed result: {}", result)
  }

}
// #work-result-consumer