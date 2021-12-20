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

final case class InstanceRoutes[F[_]: Sync](
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
        .handleErrorWith(errorResponsesHandler)
  }

  private val getPrice: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root :? InstanceKindAPIQueryParamMatcher(kind) =>
      instanceKindService
        .getPrice(kind)
        .flatMap(withPrice => Ok(InstanceWithPriceResponse(withPrice)))
        .handleErrorWith(errorResponsesHandler)
  }

  def routes: HttpRoutes[F] =
    Router(
      "/instance-kinds" -> getInstanceKinds,
      "/prices" -> getPrice
    )

  private def errorResponsesHandler(t: Throwable) = t match {
    case InstanceKindService.Exception.TooManyRequests =>
      TooManyRequests("Too many requests. Quota is 1000 per 24 hours")

    case InstanceKindService.Exception.APICallFailure(msg) =>
      println(msg)
      InternalServerError()

    case t: Throwable =>
      println(t.getMessage)
      InternalServerError()
  }

}
