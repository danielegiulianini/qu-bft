package qu

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Provides implicits to empower a [[java.util.logging.Logger]] by implicit conversions, leveraging
 * "Pimp My library" pattern.
 */
object LoggingUtils {

  /**
   * Enrich a [[java.util.logging.Logger]] with async log functionality by implicit conversion.
   *
   * @param logger logger to empower with logging functionalities.
   */
  implicit class AsyncLogger(logger: Logger) {
    /**
     * Logs a message asynchronously, returning a Future. Useful for debugging async code without Future constructor
     * boilerplate.
     *
     * @param level one of the message level identifiers.
     * @param msg   message to log.
     * @param ec    execution context performing the async logging.
     * @return Future performing the log asynchronously.
     */
    def logAsync(level: Level = Level.INFO, msg: String)(implicit ec: ExecutionContext) = Future {
      logger.log(level, msg)
    }
  }

  /**
   * Contains the prefix implicitly used by [[PrefixedLogger]] to prepend log messages.
   * Must be defined in scope for the user to [[PrefixedLogger.logWithPrefix]].
   */
  trait Prefix {
    def prefix: String
  }

  case class PrefixImpl(override val prefix: String) extends Prefix

  /**
   * Enrich a [[java.util.logging.Logger]] with log with prefix functionality by implicit conversion.
   *
   * @param logger logger to empower with logging functionalities.
   */
  implicit class PrefixedLogger(logger: Logger) {
    /**
     * Logs a message prepending a prefix to it. Useful when using at runtime different instances of the same
     * class (like a multi-local-servers scenario and needing to identify precisely corresponding log output.
     *
     * @param level one of the message level identifiers.
     * @param msg   message to log without prefix.
     * @param pref  prefix to prepend to message.
     */
    def logWithPrefix(level: Level = Level.INFO, msg: String)(implicit pref: Prefix) = {
      logger.log(level, "|" + pref.prefix + "|" + msg)
    }
  }
}