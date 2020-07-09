package anyhike.client

import anyhike.service.ZioService.HikeStoreClient
import anyhike.service.AddLocationsRequest
import anyhike.service.Location
import scalapb.zio_grpc.ZManagedChannel
import io.grpc.ManagedChannelBuilder
import zio.ExitCode
import io.grpc.CallOptions
import java.util.concurrent.TimeUnit
import anyhike.service.ZioService.HikeStore
import zio.console._

object HikeClient extends zio.App {
  val clientLayer = HikeStoreClient.live(
    ZManagedChannel(
      ManagedChannelBuilder.forAddress("localhost", 9000).usePlaintext()
    ),
    options = CallOptions.DEFAULT.withDeadlineAfter(1, TimeUnit.SECONDS)
  )

  def appLogic =
    HikeStoreClient
      .addLocations(
        AddLocationsRequest(locations = Seq(Location(12.34, 56.68)))
      )
      .flatMap { r =>
        putStrLn(r.toProtoString)
      }

  def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] = {
    appLogic.provideCustomLayer(clientLayer).exitCode
  }

}
