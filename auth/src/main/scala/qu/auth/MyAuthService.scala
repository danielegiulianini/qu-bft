package qu.auth
import scala.concurrent.Future

class MyAuthService extends AuthGrpc.Auth {

  private val usersByUsername = new Nothing
  private val usersByEmail = new Nothing

  override def register(request: Credentials): Future[RegisterResponse] = ???

  override def authorize(request: Credentials): Future[Token] = ???
}
