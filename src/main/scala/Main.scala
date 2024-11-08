import zio._
import zio.http._
import zio.http.endpoint._
import zio.http.Method._
import zio.http.codec.QueryCodec
import zio.http.codec.PathCodec._
import zio.http.endpoint.openapi.OpenAPIGen
import zio.http.endpoint.openapi.SwaggerUI
import zio.http.codec.HttpCodec
import zio.schema.Schema
import zio.schema.DeriveSchema
import zio.json.ast.Json
import zio.http.codec.HttpContentCodec
import zio.http.codec.HttpCodecError

// TODO:
// [x] Query Parameters
// [ ] Path Parameters
// [x] Request Body
// [x] Response Body
// [x] Exampels
// [x] Error Types
// [x] Basic Auth
// [ ] Auth with Context
// [x] Generate OpenAPI specs
// [ ] Setup Hot-Reloading
//

case class PushSettings(
    pushToken: String,
    pushActive: Boolean
)

given Schema[PushSettings] = DeriveSchema.gen[PushSettings]

object Endpoints {
  val greetEndpoint = Endpoint(
    GET / "greet"
  )
    .query(
      QueryCodec.query[Option[String]](
        "name"
      ) examples ("empty" -> None, "walter" -> Some("Walter"))
    )
    // ".auth()" is not reflected in the generated OpenAPI spec yet – it neither adds the required header nor the authentication flag.
    // When an "AuthType" is set, the generate route handler is checking if the Headers required by the AuthType exists.
    .auth(AuthType.Basic)
    .out[String](
      mediaType = MediaType.text.plain,
      status = Status.Ok
    ) examplesOut ("default" -> "Hello World!")

  val updatePushSettingsEndpoint = Endpoint(
    PUT / "user" / "push-settings"
  ).in[PushSettings]
    .out[PushSettings](
      mediaType = MediaType.application.json,
      status = Status.Created
    )
    // This definition allows to use every error of type ApiError
    .outErrors[ApiError.UserNotFound.type | ApiError.InvalidPushToken.type](
      HttpCodec.error[ApiError.UserNotFound.type](Status.NotFound),
      HttpCodec.error[ApiError.InvalidPushToken.type](Status.BadRequest)
    )
}

object GreetingServer extends ZIOAppDefault {
  val routes =
    Routes(
      Method.GET / Root -> handler(Response.text("Greetings at your service")),
      Method.GET / "greet-old" -> handler { (req: Request) =>
        val name = req.queryParamToOrElse("name", "World")
        Response.text(s"Hello $name!")
      }
    )

  val authenticatedRoutes = Routes(
    Endpoints.greetEndpoint
      .implementPurely(maybeName => s"Hello ${maybeName.getOrElse("World")}!")
  ) @@ Middleware.basicAuth("test", "test")

  val userRoutes = Routes(
    Endpoints.updatePushSettingsEndpoint.implement(settings =>
      ZIO.log(settings.toString()) *> {
        if (settings.pushToken.length() < 6) ZIO.fail(ApiError.InvalidPushToken)
        else ZIO.succeed(settings)
      }
    )
  )

  val swaggerRoutes = SwaggerUI.routes(
    "docs" / "openapi",
    OpenAPIGen.fromEndpoints(
      "ZIO Http Test",
      "0.0.1",
      Endpoints.greetEndpoint,
      Endpoints.updatePushSettingsEndpoint
    )
  )

  def run = Server
    .serve(routes ++ userRoutes ++ authenticatedRoutes ++ swaggerRoutes)
    .provide(Server.default)
}
