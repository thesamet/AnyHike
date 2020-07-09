import zio.test._
import anyhike.server.HikeService1
import anyhike.service.AddLocationsRequest
import anyhike.service.Location
import zio.test.environment.TestClock
import zio.duration._
import Assertion._
import anyhike.service.AddLocationsResponse
import zio.test.environment.TestConsole

object HikeService1Spec extends DefaultRunnableSpec {
  def spec =
    suite("HikingService1Spec")(
      testM("addLocations works (test has bug!)") {
        for {
          res <- HikeService1.addLocations(AddLocationsRequest(Seq(Location())))
        } yield assert(res)(equalTo(AddLocationsResponse(1)))
      }
  )
}
