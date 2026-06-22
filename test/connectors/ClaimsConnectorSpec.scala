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
import models.{GetAgentReferenceResponse, GetClaimsResponse, GetOrganisationReferenceResponse}
import play.api.test.Helpers.*
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.json.Json

import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global

class ClaimsConnectorSpec extends BaseSpec with HttpV2Support {

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      """
        |microservice {
        |   services {
        |     charities-claims {
        |       protocol = http
        |       host     = foo.bar.com
        |       port     = 1234
        |       context-path = "/charities-claims"
        |     }
        |   }
        |}
        |http-verbs.retries.intervals = [10ms,50ms]
        |""".stripMargin
    )
  )

  val connector =
    new ClaimsConnectorImpl(
      http = mockHttp,
      config = config,
      servicesConfig = new ServicesConfig(config),
      actorSystem = actorSystem
    )

  given HeaderCarrier = HeaderCarrier()

  private val baseClaimsUrl =
    "http://foo.bar.com:1234/charities-claims/claims?claimSubmitted=false"

  private val orgUrl =
    "http://foo.bar.com:1234/charities-claims/charities/organisations/123456"

  private val agentUrl =
    "http://foo.bar.com:1234/charities-claims/charities/agents/AGENT123"

  "ClaimsConnector" - {

    "retrieveUnsubmittedClaims" - {

      "should return claims when 200" in {
        val json =
          """
            |{
            |  "claimsCount": 0,
            |  "claimsList": []
            |}
            |""".stripMargin
        val expected = Json.parse(json).as[GetClaimsResponse]

        givenGetReturns(baseClaimsUrl, HttpResponse(200, json))

        await(connector.retrieveUnsubmittedClaims) shouldEqual expected
      }

      "should throw exception when 500" in {
        givenGetReturns(baseClaimsUrl, HttpResponse(500, ""))

        a[Exception] shouldBe thrownBy {
          await(connector.retrieveUnsubmittedClaims)
        }
      }

      "should retry and succeed on second attempt" in {
        val json =
          """
            |{
            |  "claimsCount": 0,
            |  "claimsList": []
            |}
            |""".stripMargin
        val expected = Json.parse(json).as[GetClaimsResponse]

        givenGetReturns(baseClaimsUrl, HttpResponse(500, ""))
        givenGetReturns(baseClaimsUrl, HttpResponse(200, json))

        await(connector.retrieveUnsubmittedClaims) shouldEqual expected
      }

      "should retry and succeed on third attempt" in {
        val json =
          """
            |{
            |  "claimsCount": 0,
            |  "claimsList": []
            |}
            |""".stripMargin
        val expected = Json.parse(json).as[GetClaimsResponse]

        givenGetReturns(baseClaimsUrl, HttpResponse(500, ""))
        givenGetReturns(baseClaimsUrl, HttpResponse(499, ""))
        givenGetReturns(baseClaimsUrl, HttpResponse(200, json))

        await(connector.retrieveUnsubmittedClaims) shouldEqual expected
      }

      "should fail after retries exhausted" in {
        givenGetReturns(baseClaimsUrl, HttpResponse(500, ""))
        givenGetReturns(baseClaimsUrl, HttpResponse(500, ""))
        givenGetReturns(baseClaimsUrl, HttpResponse(500, ""))

        a[Exception] shouldBe thrownBy {
          await(connector.retrieveUnsubmittedClaims)
        }
      }

      "should throw exception when JSON is invalid" in {
        givenGetReturns(baseClaimsUrl, HttpResponse(200, """{ invalid json }"""))

        a[Exception] shouldBe thrownBy {
          await(connector.retrieveUnsubmittedClaims)
        }
      }
    }

    "getOrganisationName" - {

      "should return organisation name when 200" in {
        val json     = """{ "organisationName": "Test Org" }"""
        val expected = Json.parse(json).as[GetOrganisationReferenceResponse]

        givenGetReturns(orgUrl, HttpResponse(200, json))

        await(connector.getOrganisationName("123456")) shouldEqual expected
      }

      "should throw exception when 404" in {
        givenGetReturns(orgUrl, HttpResponse(404, ""))

        a[Exception] shouldBe thrownBy {
          await(connector.getOrganisationName("123456"))
        }
      }

      "should throw exception when 400 with valid error body" in {
        val errorJson = """{ "errorCode": "BAD_REQUEST" }"""

        givenGetReturns(orgUrl, HttpResponse(400, errorJson))

        a[Exception] shouldBe thrownBy {
          await(connector.getOrganisationName("123456"))
        }
      }
    }

    "getAgentName" - {

      "should return agent name when 200" in {
        val json     = """{ "agentName": "Agent Smith" }"""
        val expected = Json.parse(json).as[GetAgentReferenceResponse]

        givenGetReturns(agentUrl, HttpResponse(200, json))

        await(connector.getAgentName("AGENT123")) shouldBe expected
      }

      "should throw exception when 500" in {
        givenGetReturns(agentUrl, HttpResponse(500, ""))

        a[Exception] shouldBe thrownBy {
          await(connector.getAgentName("AGENT123"))
        }
      }

      "should retry and succeed on third attempt" in {
        val json     = """{ "agentName": "Agent Smith" }"""
        val expected = Json.parse(json).as[GetAgentReferenceResponse]

        givenGetReturns(agentUrl, HttpResponse(500, ""))
        givenGetReturns(agentUrl, HttpResponse(499, ""))
        givenGetReturns(agentUrl, HttpResponse(200, json))

        await(connector.getAgentName("AGENT123")) shouldBe expected
      }
    }
  }
}
