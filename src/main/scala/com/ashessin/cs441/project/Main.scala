package com.ashessin.cs441.project

import akka.actor.ActorSystem
import com.ashessin.cs441.project.chord.FingerTable
import com.ashessin.cs441.project.workers.{FrontEnd, MasterSingleton, Worker}
import com.typesafe.config.{Config, ConfigFactory}

object Main {

  // note that 2551 and 2552 are expected to be seed nodes though, even if
  // the back-end starts at 2000
  val backEndPortRange: Range.Inclusive = 2000 to 2999

  val frontEndPortRange: Range.Inclusive = 3000 to 3999

  def main(args: Array[String]): Unit = {
    args.headOption match {

      case None =>
        startClusterInSameJvm()

      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt
        if (backEndPortRange.contains(port)) startBackEnd(port)
        else if (frontEndPortRange.contains(port)) startFrontEnd(port)
        else startWorker(port, args.lift(1).map(_.toInt).getOrElse(1))

    }
  }

  def startClusterInSameJvm(): Unit = {
    // backend nodes
    startBackEnd(2551)

    // chord ring abstraction with some worker actors (or nodes)
    val numberOfPositions = 3
    startWorker(5001, Math.pow(2, numberOfPositions).toInt)

    // front-end nodes
    val numberOfUsers = 2
    require(numberOfUsers < 2000, "Due to port range limitation please use lower value for numberOfUsers")
    for (i <- 3000 until 3000 + numberOfUsers) {
      startFrontEnd(i)
    }
  }

  /**
   * Start a node with the role backend on the given port
   */
  def startBackEnd(port: Int): Unit = {
    val system = ActorSystem("ClusterSystem", config(port, "back-end"))
    MasterSingleton.startSingleton(system)
  }

  /**
   * Start a front end node that will submit work to the backend nodes
   */
  // #front-end
  def startFrontEnd(port: Int): Unit = {
    val x = port - 3000 + 1
    val system = ActorSystem("ClusterSystem", config(port, "front-end"))
    system.actorOf(FrontEnd.props(x.toString), s"front-end-$x")
  }
  // #front-end

  /**
   * Start a worker node, with n actual workers that will accept and process workloads
   */
  // #worker
  def startWorker(port: Int, workers: Int): Unit = {
    val system = ActorSystem("ClusterSystem", config(port, "worker"))
    val masterProxy = system.actorOf(
      MasterSingleton.proxyProps(system),
      name = "masterProxy")

    // Initialize chord ring with worker nodes
    (0 until workers).foreach(n => {
      val fingerTable = new FingerTable(n, workers)
      system.actorOf(Worker.props(masterProxy, n.toString, fingerTable.finger, workers), s"worker-$n")
    })
  }
  // #worker

  def config(port: Int, role: String): Config =
    ConfigFactory.parseString(s"""
      akka.remote.netty.tcp.port=$port
      akka.cluster.roles=[$role]
    """).withFallback(ConfigFactory.load())

}
