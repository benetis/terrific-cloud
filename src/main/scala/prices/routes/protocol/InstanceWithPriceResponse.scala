package prices.routes.protocol
import io.circe._
import io.circe.syntax._
import prices.data._

final case class InstanceWithPriceResponse(value: InstanceWithPrice)

object InstanceWithPriceResponse {
  implicit val encoder: Encoder[InstanceWithPriceResponse] =
    Encoder.instance[InstanceWithPriceResponse] {
      case InstanceWithPriceResponse(
            kindWithPrice: InstanceWithPrice
          ) =>
        Json.obj(
          "kind" -> kindWithPrice.instanceKind.getString.asJson,
          "amount" -> kindWithPrice.price.price.amount.asJson
        )
    }

}
