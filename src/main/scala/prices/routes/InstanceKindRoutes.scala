package prices.routes

import cats.implicits._
import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import protocol.InstanceWithPriceResponse._
import prices.routes.protocol._
import prices.services.InstanceKindService

final case class InstanceKindRoutes[F[_]: Sync](
    instanceKindService: InstanceKindService[F]
) extends Http4sDsl[F] {

  implicit val instanceKindResponseEncoder =
    jsonEncoderOf[F, List[InstanceKindResponse]]

  implicit val instanceKindWithPriceResponseEncoder =
    jsonEncoderOf[F, InstanceWithPriceResponse]

  private val getInstanceKinds: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root =>
      instanceKindService
        .getAll()
        .flatMap(kinds => Ok(kinds.map(k => InstanceKindResponse(k))))
  }

  private val getPrice: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root :? InstanceKindAPIQueryParamMatcher(kind) =>
      instanceKindService
        .getPrice(kind)
        .flatMap(withPrice => Ok(InstanceWithPriceResponse(withPrice)))
  }

  def routes: HttpRoutes[F] =
    Router(
      "/instance-kinds" -> getInstanceKinds,
      "/prices" -> getPrice
    )

}
