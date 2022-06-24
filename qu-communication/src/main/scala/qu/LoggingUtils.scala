package qu

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}

object LoggingUtils {

  implicit class AsyncLogger(logger: Logger) {
    def logAsync(level: Level = Level.INFO, msg: String)(implicit ec: ExecutionContext) = Future {
      logger.log(level, msg)
    }
  }

  trait Prefix {
    def prefix: String
  }

  case class PrefixImpl(override val prefix: String) extends Prefix

  implicit class PrefixedLogger(logger: Logger) {
    def logWithPrefix(level: Level = Level.INFO, msg: String)(implicit pref: Prefix) = {
      logger.log(level, pref.prefix + msg)
    }
  }
}