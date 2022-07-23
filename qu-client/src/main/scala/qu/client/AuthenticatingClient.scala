package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.Shutdownable
import qu.auth.client.AuthClient

import scala.concurrent.{ExecutionContext, Future}
import qu.model.ValidationUtils.requireNonNullAsInvalid

import java.util.Objects

/**
 * Provides authentication APIs for a Q/U client, separating them for the actual operations-submission ones.
 * @param ip the ip address JWT-based auth server is listening on.
 * @param port the port of JWT-based auth server is listening on.
 * @param username the username of the Q/U client into the shoes of which requests will be performed.
 * @param password the password of the Q/U client into the shoes of which requests will be performed.
 * @tparam U type of the object replicated by Q/U servers on which operations are to be submitted.
 */
case class AuthenticatingClient[U](ip: String,
                                   port: Int,
                                   username: String,
                                   password: String)(implicit ec:ExecutionContext) extends Shutdownable {
  requireNonNullAsInvalid(username)
  requireNonNullAsInvalid(password)

  val authClient = AuthClient(ip, port)

  def register(): Future[Unit] = {
    for {
      _ <- authClient.registerAsync(username, password)
    } yield ()
  }

  def authorize():
  Future[QuClientBuilder[U, JavaTypeable]] = {
    for {
      token <- authClient.authorizeAsync(username, password)
    } yield QuClient.defaultBuilder(token = token)
  }

  override def shutdown(): Future[Unit] = authClient.shutdown()

  override def isShutdown: Boolean = authClient.isShutdown
}
