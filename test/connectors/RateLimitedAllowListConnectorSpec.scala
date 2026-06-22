/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import util.{BaseSpec, HttpV2Support}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import models.{AllowListCheckRequest, AllowListCheckResponse}
import play.api.test.Helpers.*
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.json.Json

import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global

class RateLimitedAllowListConnectorSpec extends BaseSpec with HttpV2Support {

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      """
        | microservice {
        |   services {
        |     rate-limited-allow-list  {
        |       protocol = http
        |       host     = foo.bar.com
        |       port     = 1234
        |     }
        |   }
        | }
        | splitter {
        |   serviceName = charities
        |   allowListName = beta
        | }
        | http-verbs.retries.intervals = [10ms,50ms]
        |""".stripMargin
    )
  )

  val connector =
    new RateLimitedAllowListConnectorImpl(
      http = mockHttp,
      config = config,
      servicesConfig = new ServicesConfig(config),
      actorSystem = actorSystem
    )

  given HeaderCarrier = HeaderCarrier()

  private val checkAllowListUrl =
    "http://foo.bar.com:1234/rate-limited-allow-list/services/charities/features/test-feature"

  "RateLimitedAllowListConnector" - {

    "checkAllowList" - {

      "should return true when included is true" in {
        val json    = Json.stringify(Json.toJson(AllowListCheckResponse(included = true)))
        val payload = Json.toJson(AllowListCheckRequest(identifier = "1234567890"))

        givenPostReturns(checkAllowListUrl, payload, HttpResponse(200, json))

        await(connector.checkAllowList("test-feature", "1234567890")) shouldBe true
      }

      "should return false when included is false" in {
        val json    = Json.stringify(Json.toJson(AllowListCheckResponse(included = false)))
        val payload = Json.toJson(AllowListCheckRequest(identifier = "1234567890"))

        givenPostReturns(checkAllowListUrl, payload, HttpResponse(200, json))

        await(connector.checkAllowList("test-feature", "1234567890")) shouldBe false
      }

      "should throw exception when 500 three times" in {
        val payload = Json.toJson(AllowListCheckRequest(identifier = "1234567890"))

        givenPostReturns(checkAllowListUrl, payload, HttpResponse(500, ""))
        givenPostReturns(checkAllowListUrl, payload, HttpResponse(500, ""))
        givenPostReturns(checkAllowListUrl, payload, HttpResponse(500, ""))

        a[Exception] shouldBe thrownBy {
          await(connector.checkAllowList("test-feature", "1234567890"))
        }
      }

      "should retry and succeed on a second attempt" in {
        val json    = Json.stringify(Json.toJson(AllowListCheckResponse(included = true)))
        val payload = Json.toJson(AllowListCheckRequest(identifier = "1234567890"))

        givenPostReturns(checkAllowListUrl, payload, HttpResponse(500, ""))
        givenPostReturns(checkAllowListUrl, payload, HttpResponse(200, json))

        await(connector.checkAllowList("test-feature", "1234567890")) shouldBe true
      }

      "should retry and succeed on a third attempt" in {
        val json    = Json.stringify(Json.toJson(AllowListCheckResponse(included = true)))
        val payload = Json.toJson(AllowListCheckRequest(identifier = "1234567890"))

        givenPostReturns(checkAllowListUrl, payload, HttpResponse(500, ""))
        givenPostReturns(checkAllowListUrl, payload, HttpResponse(500, ""))
        givenPostReturns(checkAllowListUrl, payload, HttpResponse(200, json))

        await(connector.checkAllowList("test-feature", "1234567890")) shouldBe true
      }

    }

  }
}
