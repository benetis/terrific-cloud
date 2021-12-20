package prices

import cats.effect._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.Response
import org.http4s.client.middleware.Retry
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import prices.config.Config
import prices.data.InstanceKind
import prices.routes.InstanceRoutes
import prices.services.SmartcloudInstanceKindService

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    val serviceWithDependencies = for {
      client <- EmberClientBuilder
                  .default[IO]
                  .build
                  .use(IO.pure)
      clientWithRetries =
        Retry[IO]((_, eith, num) => retryPolicy(eith, num))(client)
      cache <- Ref[IO].of(List.empty[InstanceKind])
    } yield SmartcloudInstanceKindService.make[IO](
      SmartcloudInstanceKindService.Config(
        config.smartcloud.baseUri,
        config.smartcloud.token
      ),
      clientWithRetries,
      cache
    )

    Stream
      .eval(
        serviceWithDependencies.flatMap { instanceKindService =>
          val httpApp =
            InstanceRoutes[IO](instanceKindService).routes.orNotFound

          EmberServerBuilder
            .default[IO]
            .withHost(Host.fromString(config.app.host).get)
            .withPort(Port.fromInt(config.app.port).get)
            .withHttpApp(Logger.httpApp(true, true)(httpApp))
            .build
            .useForever
        }
      )
  }

  private def retryPolicy(
      eith: Either[Throwable, Response[IO]],
      num: Int
  ): Option[FiniteDuration] = {

    def retry3() = if (num > 3)
      None
    else
      Some(10.millis)

    eith match {
      case Left(_)      => retry3()
      case Right(value) => if (value.status.isSuccess) None else retry3()
    }
  }

}
