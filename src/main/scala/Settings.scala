import com.typesafe.config.{ConfigFactory, Config}

object Settings {

  private val config: Config = ConfigFactory.load()

  val accessKeyId = config.getString("amazon.access-key-id")
  val secretAccessKey = config.getString("amazon.secret-access-key")
  val bucketName = config.getString("amazon.bucket-name")
  val region = config.getString("amazon.region")

}
