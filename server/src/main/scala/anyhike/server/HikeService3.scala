package anyhike.server

import io.grpc._

import anyhike.service.ZioService._
import anyhike.service._
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio._
import zio.duration._
import zio.random.Random
import zio.stream.ZStream

class HikeService3(locsRef: Ref[List[Location]]) extends ZHikeStore[ZEnv, Any] {
  def addLocations(
      req: AddLocationsRequest
  ): ZIO[ZEnv, Status, AddLocationsResponse] =
    locsRef.update(req.locations.toList ::: _) *>
      locsRef.get.map(locs => AddLocationsResponse(locs.size))

  def getLocations(req: GetLocationsRequest): IO[Status, GetLocationsResponse] =
    locsRef.get.map(GetLocationsResponse(_))

  def nextLocation(loc: Location): URIO[Random, Option[(Location, Location)]] =
    for {
      dx <- random.nextIntBetween(-10, 11)
      dy <- random.nextIntBetween(-10, 11)
      l   = Location(loc.lat + dx, loc.lng + dy, loc.timestamp + 1)
    } yield Some((l, l))

  def streamLocations(
      request: GetLocationsRequest
  ): ZStream[ZEnv, Status, Location] =
    ZStream
      .unfoldM(Location(450, 300))(nextLocation(_))
      .schedule(Schedule.spaced(50.millis))
}

object Main3 extends ServerMain {
  val myService = for {
    locsRef <- Ref.makeManaged[List[Location]](Nil)
  } yield new HikeService3(locsRef)

  def services: ServiceList[zio.ZEnv] =
    ServiceList.addManaged[ZEnv, HikeService3](myService)
}
