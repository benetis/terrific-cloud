package prices.services

import cats.implicits._
import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.{ Client, UnexpectedStatus }
import org.http4s.headers._
import org.http4s.MediaType
import org.http4s.client.dsl.Http4sClientDsl
import prices.data.InstanceWithPriceFromSmartcloud._
import org.http4s.dsl.io.GET
import org.http4s.headers.Authorization
import prices.data._

object SmartcloudInstanceKindService {

  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Temporal](
      config: Config,
      client: Client[F],
      cacheRef: Ref[F, List[InstanceKind]]
  ): InstanceKindService[F] =
    new SmartcloudInstanceKindService(config, client, cacheRef)

  private final class SmartcloudInstanceKindService[F[_]: Temporal](
      config: Config,
      client: Client[F],
      cacheRef: Ref[F, List[InstanceKind]]
  ) extends InstanceKindService[F]
      with Http4sClientDsl[F] {

    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] =
      jsonOf[F, List[String]]

    implicit val instancePrice
        : EntityDecoder[F, InstanceWithPriceFromSmartcloud] =
      jsonOf[F, InstanceWithPriceFromSmartcloud]

    implicit val getUri = s"${config.baseUri}/instances"

    override def getAll(): F[List[InstanceKind]] =
      cacheRef.get
        .flatMap(cache =>
          if (cache.isEmpty)
            requestInstanceKinds().flatMap { kinds =>
              cacheRef.update(_ => kinds).map(_ => kinds)
            }
          else
            cacheRef.get
        )

    override def getPrice(kind: InstanceKind): F[InstanceWithPrice] =
      Uri
        .fromString(s"$getUri/${kind.getString}")
        .liftTo[F]
        .flatMap { uri =>
          client
            .expect[InstanceWithPriceFromSmartcloud](buildRequest(uri))(
              instancePrice
            )
            .map(_.toInstanceWithPrice)
            .adaptError(err => handleErrorsSmartcloudErrors(err))
        }

    private def requestInstanceKinds(): F[List[InstanceKind]] =
      Uri
        .fromString(getUri)
        .liftTo[F]
        .flatMap { uri =>
          client
            .fetchAs[List[String]](buildRequest(uri))(
              instanceKindsEntityDecoder
            )
            .map(_.map(str => InstanceKind(str)))
            .adaptError(err => handleErrorsSmartcloudErrors(err))
        }

    private def buildRequest(uri: Uri): Request[F] = GET(
      uri,
      Authorization(Credentials.Token(AuthScheme.Bearer, config.token)),
      Accept(MediaType.application.json)
    )

    private def handleErrorsSmartcloudErrors(
        throwable: Throwable
    ): InstanceKindService.Exception = throwable match {
      case UnexpectedStatus(Status.TooManyRequests, _, _) =>
        InstanceKindService.Exception.TooManyRequests
      case t: Throwable =>
        InstanceKindService.Exception.APICallFailure(t.getMessage)
    }
  }

}
