import zio._
import zio.http._
import zio.http.endpoint._
import zio.http.Method.GET
import zio.http.codec.QueryCodec
import zio.http.codec.PathCodec._
import zio.http.endpoint.openapi.OpenAPIGen
import zio.http.endpoint.openapi.SwaggerUI

object Endpoints {
  val greetEndpoint = Endpoint(
    GET / "greet"
  )
    .query(
      QueryCodec.query[Option[String]](
        "name"
      ) examples ("empty" -> None, "walter" -> Some("Walter"))
    )
    // ".auth()" Seems to have no effect currently since it is not reflected in the generated OpenAPI spec,
    // and is not reflected in the type of the generated routes.
    .auth(AuthType.Basic)
    .out[String](
      mediaType = MediaType.text.plain,
      status = Status.Ok
    ) examplesOut ("default" -> "Hello World!")
}

object GreetingServer extends ZIOAppDefault {
  val routes =
    Routes(
      Method.GET / Root -> handler(Response.text("Greetings at your service")),
      Endpoints.greetEndpoint
        .implementHandler(
          handler((nameOpt: Option[String]) =>
            s"Hello ${nameOpt.getOrElse("World")}!"
          )
        ),
      Method.GET / "greet-old" -> handler { (req: Request) =>
        val name = req.queryParamToOrElse("name", "World")
        Response.text(s"Hello $name!")
      }
    ) @@ Middleware.basicAuth("test", "test")
  val swaggerRoutes = SwaggerUI.routes(
    "docs" / "openapi",
    OpenAPIGen.fromEndpoints(
      "ZIO Http Test",
      "0.0.1",
      Endpoints.greetEndpoint
    )
  )

  def run = Server.serve(routes ++ swaggerRoutes).provide(Server.default)
}
