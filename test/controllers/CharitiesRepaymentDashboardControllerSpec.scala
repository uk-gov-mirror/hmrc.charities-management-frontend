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

package controllers

import config.AppConfig
import connectors.ClaimsConnector
import models.{ClaimInfo, GetAgentReferenceResponse, GetClaimsResponse, GetOrganisationReferenceResponse}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.Html
import util.ControllerSpecBase
import views.html.{CharityRepaymentDashboardAgentView, CharityRepaymentDashboardView}

import scala.concurrent.Future
import controllers.actions.SplitterAction
import connectors.RateLimitedAllowListConnector
import play.api.Configuration

class CharitiesRepaymentDashboardControllerSpec extends ControllerSpecBase {

  val mockRateLimitedAllowListConnector = mock[RateLimitedAllowListConnector]
  when(mockRateLimitedAllowListConnector.checkAllowList(any(), any())(using any()))
    .thenReturn(Future.successful(true))

  "CharitiesRepaymentDashboardController onPageLoad" should {

    "return 200 OK and render the CharityRepaymentDashboardView for an Organisation user" in {
      val mockConfig    = mock[AppConfig]
      val mockConnector = mock[ClaimsConnector]
      val orgId         = "test-org-123"
      val mockOrgView   = mock[CharityRepaymentDashboardView]
      val mockAgentView = mock[CharityRepaymentDashboardAgentView]

      when(mockConnector.retrieveUnsubmittedClaims(using any()))
        .thenReturn(Future.successful(GetClaimsResponse(claimsList = List.empty, claimsCount = 0)))
      when(mockConnector.getOrganisationName(any())(using any()))
        .thenReturn(Future.successful(GetOrganisationReferenceResponse(Some("Test Org"))))
      when(mockOrgView.apply(eqTo(orgId), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Html("<p>Org View</p>"))

      val controller = new CharitiesRepaymentDashboardController(
        cc,
        fakeOrg(orgId),
        new SplitterAction(mockConfig, mockRateLimitedAllowListConnector),
        mockConfig,
        mockConnector,
        mockOrgView,
        mockAgentView
      )

      val result = controller.onPageLoad(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) must include("Org View")
      verify(mockOrgView).apply(eqTo(orgId), any(), any(), any(), any(), any(), any())(any(), any())
    }

    "redirect to legacy charities service url when the user is not allowed" in {
      val appConfig = AppConfig(
        Configuration.from(
          Map(
            "splitter.trafficSplitEnabled"   -> true,
            "splitter.allowListName"         -> "beta-test",
            "urls.legacyCharitiesServiceUrl" -> "http://localhost:9020/charities"
          )
        )
      )
      val mockConnector                     = mock[ClaimsConnector]
      val orgId                             = "test-org-123"
      val mockOrgView                       = mock[CharityRepaymentDashboardView]
      val mockAgentView                     = mock[CharityRepaymentDashboardAgentView]
      val mockRateLimitedAllowListConnector = mock[RateLimitedAllowListConnector]

      when(mockRateLimitedAllowListConnector.checkAllowList(any(), any())(using any()))
        .thenReturn(Future.successful(false))
      when(mockConnector.retrieveUnsubmittedClaims(using any()))
        .thenReturn(Future.successful(GetClaimsResponse(claimsList = List.empty, claimsCount = 0)))
      when(mockConnector.getOrganisationName(any())(using any()))
        .thenReturn(Future.successful(GetOrganisationReferenceResponse(Some("Test Org"))))
      when(mockOrgView.apply(eqTo(orgId), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Html("<p>Org View</p>"))

      val controller = new CharitiesRepaymentDashboardController(
        cc,
        fakeOrg(orgId),
        new SplitterAction(appConfig, mockRateLimitedAllowListConnector),
        appConfig,
        mockConnector,
        mockOrgView,
        mockAgentView
      )

      val result = controller.onPageLoad(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("http://localhost:9020/charities")
    }

    "pass claimsCount == 1 as true to org view when exactly one claim exists" in {
      val mockConfig    = mock[AppConfig]
      val mockConnector = mock[ClaimsConnector]
      val orgId         = "org-with-one-claim"
      val mockOrgView   = mock[CharityRepaymentDashboardView]
      val mockAgentView = mock[CharityRepaymentDashboardAgentView]

      when(mockConnector.retrieveUnsubmittedClaims(using any()))
        .thenReturn(Future.successful(GetClaimsResponse(claimsList = List(ClaimInfo("c1")), claimsCount = 1)))
      when(mockConnector.getOrganisationName(any())(using any()))
        .thenReturn(Future.successful(GetOrganisationReferenceResponse(None)))
      when(mockOrgView.apply(any(), any(), any(), any(), any(), eqTo(true), any())(any(), any()))
        .thenReturn(Html("<p>One Claim</p>"))

      val controller = new CharitiesRepaymentDashboardController(
        cc,
        fakeOrg(orgId),
        new SplitterAction(mockConfig, mockRateLimitedAllowListConnector),
        mockConfig,
        mockConnector,
        mockOrgView,
        mockAgentView
      )

      val result = controller.onPageLoad(FakeRequest())

      status(result) mustBe OK
      verify(mockOrgView).apply(any(), any(), any(), any(), any(), eqTo(true), any())(any(), any())
    }

    "return 200 OK and render the CharityRepaymentDashboardAgentView for an Agent user" in {
      val mockConfig    = mock[AppConfig]
      val mockConnector = mock[ClaimsConnector]
      val agentId       = "test-agent-456"
      val mockOrgView   = mock[CharityRepaymentDashboardView]
      val mockAgentView = mock[CharityRepaymentDashboardAgentView]

      when(mockConnector.retrieveUnsubmittedClaims(using any()))
        .thenReturn(Future.successful(GetClaimsResponse(claimsList = List.empty, claimsCount = 0)))
      when(mockConnector.getAgentName(any())(using any()))
        .thenReturn(Future.successful(GetAgentReferenceResponse("Agent Name")))
      when(mockAgentView.apply(eqTo(agentId), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Html("<p>Agent View</p>"))

      val controller = new CharitiesRepaymentDashboardController(
        cc,
        fakeAgent(agentId),
        new SplitterAction(mockConfig, mockRateLimitedAllowListConnector),
        mockConfig,
        mockConnector,
        mockOrgView,
        mockAgentView
      )

      val result = controller.onPageLoad(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) must include("Agent View")
    }

    "return 200 OK and render agent view with page parameter when page query string is provided" in {
      val mockConfig    = mock[AppConfig]
      val mockConnector = mock[ClaimsConnector]
      val agentId       = "test-agent-789"
      val mockOrgView   = mock[CharityRepaymentDashboardView]
      val mockAgentView = mock[CharityRepaymentDashboardAgentView]

      val claimsOnMultiplePages = (1 to 15).map(i => ClaimInfo(s"claim-$i")).toList

      when(mockConnector.retrieveUnsubmittedClaims(using any()))
        .thenReturn(Future.successful(GetClaimsResponse(claimsList = claimsOnMultiplePages, claimsCount = 15)))
      when(mockConnector.getAgentName(any())(using any()))
        .thenReturn(Future.successful(GetAgentReferenceResponse("Paged Agent")))
      when(mockAgentView.apply(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Html("<p>Paged Agent View</p>"))

      val controller = new CharitiesRepaymentDashboardController(
        cc,
        fakeAgent(agentId),
        new SplitterAction(mockConfig, mockRateLimitedAllowListConnector),
        mockConfig,
        mockConnector,
        mockOrgView,
        mockAgentView
      )

      val result = controller.onPageLoad(FakeRequest().withFormUrlEncodedBody("page" -> "2"))

      status(result) mustBe OK
    }

    "redirect to AccessDenied when the user is an Individual" in {
      val mockConfig    = mock[AppConfig]
      val mockConnector = mock[ClaimsConnector]
      val mockOrgView   = mock[CharityRepaymentDashboardView]
      val mockAgentView = mock[CharityRepaymentDashboardAgentView]

      val controller = new CharitiesRepaymentDashboardController(
        cc,
        fakeIndividual,
        new SplitterAction(mockConfig, mockRateLimitedAllowListConnector),
        mockConfig,
        mockConnector,
        mockOrgView,
        mockAgentView
      )

      val result = controller.onPageLoad(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.AccessDeniedController.onPageLoad.url
    }
  }
}
