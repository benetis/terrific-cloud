package prices.services

import cats.implicits._
import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.headers._
import org.http4s.MediaType
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io.GET
import org.http4s.headers.Authorization
import prices.data._

object SmartcloudInstanceKindService {

  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Concurrent](
      config: Config,
      client: Client[F],
      cacheRef: Ref[F, List[InstanceKind]]
  ): InstanceKindService[F] =
    new SmartcloudInstanceKindService(config, client, cacheRef)

  private final class SmartcloudInstanceKindService[F[_]: Concurrent](
      config: Config,
      client: Client[F],
      cacheRef: Ref[F, List[InstanceKind]]
  ) extends InstanceKindService[F]
      with Http4sClientDsl[F] {

    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] =
      jsonOf[F, List[String]]

    val getAllUri = s"${config.baseUri}/instances"

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

    private def requestInstanceKinds(): F[List[InstanceKind]] =
      Uri
        .fromString(getAllUri)
        .liftTo[F]
        .flatMap { uri =>
          val request: Request[F] = GET(
            uri,
            Authorization(Credentials.Token(AuthScheme.Bearer, config.token)),
            Accept(MediaType.application.json)
          )

          client
            .fetchAs[List[String]](request)(instanceKindsEntityDecoder)
            .map(_.map(InstanceKind))
        }
  }

}
