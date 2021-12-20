package prices

import cats.effect.IO
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.syntax.all._
import prices.data.{ InstanceKind, InstanceWithPrice, Price }
import prices.routes.InstanceKindRoutes
import prices.services.InstanceKindService
import squants.market.{ JPY, Money }

class MyHttpRoutesSuite extends munit.Http4sHttpRoutesSuite {

  implicit val instanceKindsEntityDecoder
      : EntityDecoder[IO, List[InstanceKind]] =
    jsonOf[IO, List[InstanceKind]]

  override val routes: HttpRoutes[IO] =
    InstanceKindRoutes[IO](new InstanceKindService[IO] {
      override def getAll(): IO[List[InstanceKind]] =
        IO.pure(List(InstanceKind("hetzner-1")))

      override def getPrice(kind: InstanceKind): IO[InstanceWithPrice] =
        IO.pure(InstanceWithPrice(kind, price = Price(Money(0.1, JPY))))
    }).routes

  test(GET(uri"instance-kinds")).alias("Test available instance kinds") {
    response =>
      assertIO(response.as[List[InstanceKind]], List(InstanceKind("hetzner-1")))
  }

}
