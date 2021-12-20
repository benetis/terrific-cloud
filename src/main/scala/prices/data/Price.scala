package prices.data
import io.circe._
import squants.market.{ JPY, Money }

final case class Price(price: Money) extends AnyVal

final case class InstanceWithPrice(instanceKind: InstanceKind, price: Price)

final case class InstanceWithPriceFromSmartcloud(
    kind: InstanceKind,
    price: BigDecimal
) {
  def toInstanceWithPrice: InstanceWithPrice =
    InstanceWithPrice(kind, Price(Money.apply(price, JPY)))
}

object InstanceWithPriceFromSmartcloud {
  implicit val decodeFoo: Decoder[InstanceWithPriceFromSmartcloud] =
    (c: HCursor) =>
      for {
        kind <- c.downField("kind").as[String].map(InstanceKind)
        price <- c.downField("price").as[BigDecimal]
      } yield new InstanceWithPriceFromSmartcloud(kind, price)
}
