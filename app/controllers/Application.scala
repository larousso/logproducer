package controllers

import play.api.mvc._
import play.api.libs.concurrent.Akka
import akka.actor.Props
import play.api.Play.current
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsValue
import play.api.libs.iteratee.{Enumerator, Enumeratee, Concurrent}
import play.api.libs.EventSource
import play.api.libs.iteratee.Concurrent.Channel


object Application extends Controller {

  val actorRef = Akka.system.actorOf(Props[LogActor])

  val (chatOut: Enumerator[JsValue], chatChannel: Channel[JsValue]) = Concurrent.broadcast[JsValue]

  def index = Action {
    Ok(views.html.index())
  }

  def connDeathWatch(addr: String): Enumeratee[JsValue, JsValue] = Enumeratee.onIterateeDone{ () =>
    Logger.info(s"$addr - SSE disconnected")
  }

  def start = Action {req =>
    Logger.info("Start !")
    actorRef ! Start(chatChannel)
    Ok("")
  }

  def feeds  = Action {req =>
    Ok.feed(chatOut
      &> Concurrent.buffer(50)
      &> connDeathWatch(req.remoteAddress)
      &> EventSource()
    ).as("text/event-stream")
  }

  def stop = Action {
    Logger.info("Stop !")
    actorRef ! Stop
    Ok("")
  }

}