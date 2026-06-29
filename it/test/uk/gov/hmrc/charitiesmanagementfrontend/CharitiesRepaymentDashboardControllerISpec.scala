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

package uk.gov.hmrc.charitiesmanagementfrontend

import models.{ClaimInfo, GetAgentReferenceResponse, GetClaimsResponse, GetOrganisationReferenceResponse}
import org.jsoup.Jsoup
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.charitiesmanagementfrontend.stubs.{AuthStub, ClaimsStub}
import utils.ComponentSpecHelper

class CharitiesRepaymentDashboardControllerISpec extends ComponentSpecHelper with AuthStub with ClaimsStub {

  private val claimId   = "AB123"
  private val reference = "1234567890"
  "GET /manage-charity-repayment-claim" should {

    "return 200 and redirect to org view for Organisation user" in {
      val getClaimsResponse =
        GetClaimsResponse(
          claimsCount = 1,
          claimsList = List(
            ClaimInfo(claimId, Some(reference), Some("Test charity"))
          )
        )
      val getOrgReferenceResponse = GetOrganisationReferenceResponse(
        organisationName = Some("Test charity")
      )
      stubAuthRequest()
      stubRetrieveUnsubmittedClaims(OK, Json.toJson(getClaimsResponse))
      stubGetOrganisationName(reference)(OK, Json.toJson(getOrgReferenceResponse))

      val result = get("/manage-charity-repayment-claim")
      result.status shouldBe OK
      Jsoup.parse(result.body).title should include(msg("charityRepaymentDashboard.title"))
    }

    "return 200 and redirect to agent view for Agent user" in {
      val getClaimsResponse =
        GetClaimsResponse(
          claimsCount = 1,
          claimsList = List(
            ClaimInfo(claimId, Some(reference), Some("Test charity"))
          )
        )
      val getAgentReferenceResponse = GetAgentReferenceResponse(
        agentName = "Test charity"
      )
      stubAgentAuthRequest()
      stubRetrieveUnsubmittedClaims(OK, Json.toJson(getClaimsResponse))
      stubGetAgentName(reference)(OK, Json.toJson(getAgentReferenceResponse))

      val result = get("/manage-charity-repayment-claim")
      result.status shouldBe OK
      Jsoup.parse(result.body).title should include(msg("charityRepaymentDashboardAgent.title"))
    }
  }
}
