import qu.auth.client.AuthClient

object AuthSpec extends App {

  //val a = HelloWorldClient()

  //todo decide if using scalatest or junit (rule) see the cluttering replacing grpccleanup rule

  //fixtures

  //start inprocess server...

  //create client inprocesschannel

  //create client passing in process channel

  //test 1: funzionamento corretto (risposta attesa)

  //test 2: tutte le exception

  //shutdown inprocess server

  //shutdown inprocess channel


  //prof' spec:
  /*
  import auth.ConflictException
import qu.auth.User
import auth.WrongCredentialsException
import java.util.Optional
import java.util.stream.Collectors
import java.util.stream.Stream
  @Test  def testRegisterErrors(): Unit =  { assertThrows(classOf[ConflictException], () => authenticator.register(andrea))
assertThrows(classOf[ConflictException], () => authenticator.register(giovanni))
assertThrows(classOf[ConflictException], () => authenticator.register(stefano))
assertThrows(classOf[IllegalArgumentException], () => authenticator.register(noUser))
assertThrows(classOf[IllegalArgumentException], () => authenticator.register(noPassword))
assertThrows(classOf[IllegalArgumentException], () => authenticator.register(noEmail))
}

    private def credentialsOf(user: User): Nothing =  { return new Nothing(user.getUsername, user.getPassword)
}

    private def allCredentialsOf(user: User): Nothing =  { return Stream.concat(Stream.of(user.getUsername), user.getEmailAddresses.stream).map((it: T) => new Nothing(it, user.getPassword)).collect(Collectors.toList)
}

    private def tokenOf(user: User): Nothing =  { return new Nothing(user.getUsername, Optional.ofNullable(user.getRole).orElse(Role.USER))
}

    @Test  @throws[WrongCredentialsException]
 def testAuthorize(): Unit =  { import scala.collection.JavaConversions._
for (user <- List.of(giovanni, andrea, stefano))  { import scala.collection.JavaConversions._
for (credentials <- allCredentialsOf(user))  { assertEquals(tokenOf(user), authenticator.authorize(credentials))
}
val user2: User = new User(user)
user2.setUsername(user.getUsername + "2")
assertThrows(classOf[WrongCredentialsException], () => authenticator.authorize(credentialsOf(user2)))
val user3: User = new User(user)
user3.setPassword(user.getPassword + "-")
assertThrows(classOf[WrongCredentialsException], () => authenticator.authorize(credentialsOf(user3)))
}
assertThrows(classOf[IllegalArgumentException], () => authenticator.authorize(credentialsOf(noUser)))
assertThrows(classOf[IllegalArgumentException], () => authenticator.authorize(credentialsOf(noPassword)))
assertThrows(classOf[WrongCredentialsException], () => authenticator.authorize(credentialsOf(noEmail)))
}
   */


}
