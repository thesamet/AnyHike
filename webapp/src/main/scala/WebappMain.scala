package anyhike.webapp

import io.grpc.Status

import org.scalajs.dom
import org.scalajs.dom.raw.MouseEvent

import anyhike.service.AddLocationsRequest
import anyhike.service.GetLocationsRequest
import anyhike.service.Location
import anyhike.service.ZioService.HikeStoreClient
import scalapb.grpc.Channels
import scalapb.zio_grpc.ZManagedChannel
import zio.Queue
import zio.UIO
import zio._
import zio.stream.Stream

object WebappMain extends App {
  sealed trait InteractionEvent
  case class MouseMove(m: MouseEvent) extends InteractionEvent
  case class MouseUp(m: MouseEvent)   extends InteractionEvent
  case class MouseDown(m: MouseEvent) extends InteractionEvent

  sealed trait State
  case object Idle                         extends State
  case class InProgress(l: List[Location]) extends State
  case class Complete(l: List[Location])   extends State

  val canvas   = dom.document.getElementById("pad").asInstanceOf[dom.html.Canvas]
  val rect     = canvas.getBoundingClientRect()
  val renderer =
    canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  def mark(l: Location, color: String): UIO[Unit] =
    ZIO.effectTotal {
      renderer.fillStyle = color
      renderer.fillRect(l.lat, l.lng, 5, 5)
    }

  val appLogic: ZIO[HikeStoreClient, Status, Unit] = for {
    _  <- HikeStoreClient
            .streamLocations(GetLocationsRequest())
            .foreach {
              mark(_, "red")
            }
            .fork
    q  <- Queue.unbounded[InteractionEvent]
    rt <- ZIO.runtime[Any]
    _  <- ZIO.effectTotal {
            canvas.onmousedown = { e => rt.unsafeRunSync(q.offer(MouseDown(e))) }
            canvas.onmouseup = { e => rt.unsafeRunSync(q.offer(MouseUp(e))) }
            canvas.onmousemove = { e => rt.unsafeRunSync(q.offer(MouseMove(e))) }
          }
    _  <- Stream
            .fromQueue(q)
            .foldM(Idle: State) {
              case (Idle, MouseDown(e))             =>
                val loc = Location(
                  e.clientX - rect.left,
                  e.clientY - rect.top,
                  System.currentTimeMillis()
                )
                mark(loc, "green").as(InProgress(loc :: Nil))

              case (InProgress(locs), MouseMove(e)) =>
                val loc = Location(
                  e.clientX - rect.left,
                  e.clientY - rect.top,
                  System.currentTimeMillis()
                )
                mark(loc, "green").as(InProgress(loc :: locs))

              case (InProgress(l), MouseUp(_))      =>
                HikeStoreClient
                  .addLocations(AddLocationsRequest(l.toSeq))
                  .as(Idle)

              case (s, _)                           => ZIO.succeed(s)
            }
  } yield ()

  val clientLayer = HikeStoreClient.live(
    ZManagedChannel(Channels.grpcwebChannel(""))
  )

  def run(args: List[String]) =
    (appLogic.provideLayer(clientLayer).ignore).exitCode
}
