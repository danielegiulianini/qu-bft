package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.Shutdownable
import qu.auth.client.AuthClient

import scala.concurrent.{ExecutionContext, Future}
import qu.model.ValidationUtils.requireNonNullAsInvalid

import java.util.Objects

//client for providing username (or registering)
//now modeled as normal class, then could think of a builder
case class AuthenticatingClient[U](ip: String,
                                   port: Int,
                                   username: String,
                                   password: String)(implicit ec:ExecutionContext) extends Shutdownable {
  requireNonNullAsInvalid(username)
  requireNonNullAsInvalid(password)

  val authClient = AuthClient(ip, port)

  def register(): Future[Unit] = {
    println("registrering in AithenticatinCLint")
    for {

      _ <- authClient.registerAsync(username, password)
      _ <- Future{println("finished registerting...")}

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
