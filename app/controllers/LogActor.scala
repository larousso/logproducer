package controllers

import akka.actor.Actor
import scala.util.Random
import org.slf4j.{Logger, LoggerFactory}
import play.api
import rx.lang.scala._

import scala.concurrent.duration.DurationDouble
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{Writes, Json, JsValue}
import java.text.SimpleDateFormat

/**
 * Actor qui génère des logs
 * Created by adelegue on 18/04/2014.
 */
case class Start(channel: Channel[JsValue])
case object Stop

case class Log(level: String, logger:Logger, wood:String, message: String)

class LogActor extends Actor{

  override def receive: Actor.Receive = staging

  var run = false
  var subsc:Subscription = _
  var eventSource:Subscription = _

  val logFile = LoggerFactory.getLogger("file")
  val logSyslog = LoggerFactory.getLogger("syslog")

  val coordGps = Map(
    "Peuplier" -> List((47.749832, 1.274534), (44.414088, 2.262237)),
    "Chêne" -> List((50.025319, 26.300323), (49.992244, 10.629901), (54.963777, 28.471697), (45.934388, 11.596697), (47.143447, 23.374041)),
    "Sequoia" -> List((38.085533, -120.063454)),
    "Acajou" -> List((6.602720, 18.715840), (35.838557, -97.475564)),
    "Cerisier" -> List((37.669289, -103.979472), (47.830333, 1.489276), (34.978961, 135.083024), (36.688938, 85.512713))
  )

  implicit val loggerWrite:Writes[Logger] = Writes(logger => Json.obj("logger" -> logger.getName))
  implicit val logWrite = Json.writes[Log]

  val ticks:Observable[Log] = Observable.interval(500 milliseconds).map[Log]{count =>
    val (wood, lat, lng) = pickWood
    val minuteFormat = new SimpleDateFormat("dd-MM-yyyy")
    val log = Log(pickLogLevel, pickAppender, wood, s"message numéro $count current time ${minuteFormat.format(new java.util.Date())}, bois : $wood, coord : [$lat, $lng]")
    api.Logger.info(s"new Message $log")
    log
  }

  def staging: Actor.Receive = {
    case Start(channel) =>
      api.Logger.info("start message")
      subsc = ticks.subscribe(handleLog(_))
      eventSource = ticks.subscribe(pushLog(_, channel))
      api.Logger.info("become running")
      context become running
  }

  def running : Actor.Receive = {
    case Stop =>
      api.Logger.info("stop message")
      subsc.unsubscribe()
      eventSource.unsubscribe()
      api.Logger.info("becomine staging")
      context become staging
  }

  def handleLog(log: Log):Unit = {
    log match {
      case Log("ERROR", logger, wood, msg) => logger.error(msg, new Exception)
      case Log("INFO", logger, wood, msg) => logger.info(msg)
      case Log("DEBUG", logger, wood, msg) => logger.debug(msg)
    }
  }

  def pushLog(log: Log, channel: Channel[JsValue]):Unit = {
    channel.push(Json.toJson(log))
  }

  def pickWood = {
    Random.nextInt(100) match {
      case i if i<= 25 =>
        val (lat, lng) = pickGps("Chêne")
        ("Chêne", lat, lng)
      case i if i > 25 && i <= 50 =>
        val (lat, lng) = pickGps("Peuplier")
        ("Peuplier", lat, lng)
      case i if i > 50 && i <= 80 =>
        val (lat, lng) = pickGps("Cerisier")
        ("Cerisier", lat, lng)
      case i if i > 80 && i <= 90 =>
        val (lat, lng) = pickGps("Sequoia")
        ("Sequoia", lat, lng)
      case _ =>
        val (lat, lng) = pickGps("Acajou")
        ("Acajou", lat, lng)
    }
  }

  def pickGps(wood: String) = {
    val coords = coordGps(wood)
    coords(Random.nextInt(coords.length))
  }

  def pickAppender = {
    Random.nextInt(100) match {
      case i if i<= 60 => logFile
      case _ => logSyslog
    }
  }

  def pickLogLevel = {
    Random.nextInt(100) match {
      case i if i<= 10 => "ERROR"
      case i if i > 20 && i <= 50 => "DEBUG"
      case _ => "INFO"
    }
  }
}
