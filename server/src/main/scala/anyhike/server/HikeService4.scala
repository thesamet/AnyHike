package anyhike.server

import io.grpc._

import anyhike.service.ZioService._
import anyhike.service._
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio._
import zio.stream.Stream

class HikeService4(
    locsRef: Ref[List[Location]],
    queue: Queue[Location],
    subscribe: UIO[Stream[Nothing, Location]]
) extends HikeStore {

  def addLocations(
      req: AddLocationsRequest
  ): IO[Status, AddLocationsResponse] =
    locsRef.update(req.locations.toList ::: _) *>
      queue.offerAll(req.locations) *>
      locsRef.get.map(locs => AddLocationsResponse(locs.size))

  def getLocations(req: GetLocationsRequest): IO[Status, GetLocationsResponse] =
    locsRef.get.map(GetLocationsResponse(_))

  def streamLocations(
      request: GetLocationsRequest
  ): Stream[Status, Location] = Stream.unwrap(subscribe)
}

object Main4 extends ServerMain {

  val hikeService: UManaged[HikeService4] = for {
    q         <- Queue.unbounded[Location].toManaged_
    mainStream = Stream.fromQueue(q)
    subscribe <- mainStream.broadcastDynamic(1000)
    locsRef   <- Ref.makeManaged[List[Location]](Nil)
  } yield new HikeService4(locsRef, q, subscribe)

  def services: ServiceList[zio.ZEnv] = ServiceList.addManaged(hikeService)
}
