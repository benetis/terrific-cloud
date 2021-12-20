package prices

import cats.effect.IO
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.syntax.all._
import prices.data.{
  InstanceKind,
  InstanceWithPrice,
  InstanceWithPriceFromSmartcloud,
  Price
}
import prices.routes.InstanceKindRoutes
import prices.routes.protocol.InstanceWithPriceResponse
import prices.services.InstanceKindService
import squants.market.{ JPY, Money }
import io.circe._

class TestInstanceKinds extends munit.Http4sHttpRoutesSuite {

  implicit val instanceKindsEntityDecoder
      : EntityDecoder[IO, List[InstanceKind]] =
    jsonOf[IO, List[InstanceKind]]

  implicit val decoderInstancePrice = new Decoder[InstanceWithPriceResponse] {
    final def apply(c: HCursor): Decoder.Result[InstanceWithPriceResponse] =
      for {
        kind <- c.downField("kind").as[String].map(InstanceKind(_))
        price <-
          c.downField("amount").as[BigDecimal].map(d => Price(Money(d, JPY)))
      } yield new InstanceWithPriceResponse(InstanceWithPrice(kind, price))
  }

  implicit val instancePriceResponseDecoder
      : EntityDecoder[IO, InstanceWithPriceResponse] =
    jsonOf[IO, InstanceWithPriceResponse]

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

  test(GET(uri"prices?kind=hetzner-1")).alias("Test price of instance") {
    response =>
      assertIO(
        response.as[InstanceWithPriceResponse],
        InstanceWithPriceResponse(
          InstanceWithPrice(InstanceKind("hetzner-1"), Price(Money(0.1, JPY)))
        )
      )
  }

}
