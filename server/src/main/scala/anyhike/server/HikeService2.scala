package anyhike.server

import io.grpc._

import anyhike.service.ZioService._
import anyhike.service._
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio._
import zio.stream.Stream

class HikeService2(locationsRef: Ref[List[Location]]) extends HikeStore {
  def addLocations(
      req: AddLocationsRequest
  ): IO[Status, AddLocationsResponse] =
    locationsRef.update(locs => req.locations.toList ::: locs) *>
      locationsRef.get.map(locs => AddLocationsResponse(locs.size))

  def getLocations(req: GetLocationsRequest): IO[Status, GetLocationsResponse] =
    locationsRef.get.map(GetLocationsResponse(_))

  def streamLocations(
      req: GetLocationsRequest
  ): Stream[Status, Location] =
    ???
}

object Main2 extends ServerMain {
  val myService = Ref.make(List[Location]()).map(new HikeService2(_))

  def services: ServiceList[zio.ZEnv] = ServiceList.addM(myService)
}
