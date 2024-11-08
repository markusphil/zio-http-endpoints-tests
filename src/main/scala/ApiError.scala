import zio.schema.Schema
import zio.schema.DeriveSchema

enum ApiError(val code: String) {
  case InvalidPushToken extends ApiError("3001")
  case UserNotFound extends ApiError("2001")
  case UnexpectedError extends ApiError("0000")
}

object ApiError {
  given Schema[ApiError] = DeriveSchema.gen
  given Schema[ApiError.InvalidPushToken.type] = DeriveSchema.gen
  given Schema[ApiError.UserNotFound.type] = DeriveSchema.gen
  given Schema[ApiError.UnexpectedError.type] = DeriveSchema.gen

}
