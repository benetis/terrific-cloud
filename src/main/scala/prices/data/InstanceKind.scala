package prices.data

import io.circe._

final case class InstanceKind(getString: String) extends AnyVal

object InstanceKind {
  implicit val decoder: Decoder[InstanceKind] =
    (c: HCursor) =>
      for {
        kind <- c.downField("kind").as[String]
      } yield InstanceKind(kind)
}
