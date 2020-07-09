package anyhike.server

import io.grpc._

import anyhike.service.ZioService._
import anyhike.service._
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio._
import zio.clock.Clock
import zio.console._
import zio.duration._
import zio.stream.Stream

object HikeService1 extends ZHikeStore[Clock with Console, Any] {

  def addLocations(
      req: AddLocationsRequest
  ): ZIO[Clock with Console, Status, AddLocationsResponse] =
    ZIO.bracket(IO.succeed(42))(_ => putStrLn("Released!")) { _ =>
      putStrLn(".").repeat(Schedule.recurs(20) && Schedule.spaced(50.millis)) *>
        putStrLn("Done!") *>
        IO.succeed(AddLocationsResponse(17))
    }

  def getLocations(req: GetLocationsRequest): IO[Status, GetLocationsResponse] =
    ???

  def streamLocations(
      req: GetLocationsRequest
  ): Stream[Status, Location] =
    ???
}

object Main1 extends ServerMain {
  override def port: Int = 9000

  def services = ServiceList.add(HikeService1)
}
