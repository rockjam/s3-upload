import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client

object AmazonS3 {

  def init() = {
    val credentials = new ProfileCredentialsProvider().getCredentials()
    val s3Client = new AmazonS3Client(credentials)
    s3Client.setRegion(Region.getRegion(Regions.EU_WEST_1))
    s3Client
  }

}
