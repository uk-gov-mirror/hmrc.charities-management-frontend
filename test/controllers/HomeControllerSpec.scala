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

import models.requests.UserType
import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContent
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import util.ControllerSpecBase
import config.AppConfig
import play.api.Configuration
import connectors.RateLimitedAllowListConnector
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import controllers.actions.SplitterAction

class HomeControllerSpec extends ControllerSpecBase {

  private val request: FakeRequest[AnyContent] =
    FakeRequest(GET, "/")

  private def appConfig(useRateLimitedAllowList: Boolean): AppConfig =
    AppConfig(
      Configuration.from(
        Map(
          "splitter.trafficSplitEnabled"   -> useRateLimitedAllowList,
          "splitter.allowListName"         -> "beta-test",
          "urls.legacyCharitiesServiceUrl" -> "http://localhost:9020/charities"
        )
      )
    )

  "HomeController landingPage" should {

    "redirect Organisation users to the organisation dashboard" in {
      val result = controller(UserType.Organisation, useRateLimitedAllowList = false, isUserAllowed = None).landingPage(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe
        controllers.routes.CharitiesRepaymentDashboardController.onPageLoad.url
    }

    "redirect Agent users to the agent dashboard" in {
      val result = controller(UserType.Agent, useRateLimitedAllowList = false, isUserAllowed = None).landingPage(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe
        controllers.routes.CharitiesRepaymentDashboardController.onPageLoad.url
    }

    "redirect Individual users to access denied" in {
      val result = controller(UserType.Individual, useRateLimitedAllowList = false, isUserAllowed = None).landingPage(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe
        controllers.routes.AccessDeniedController.onPageLoad.url
    }

    "redirect Organisation users to the organisation dashboard if trafic split is enabled and user is allowed" in {
      val result = controller(UserType.Organisation, useRateLimitedAllowList = true, isUserAllowed = Some(_ => true)).landingPage(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe
        controllers.routes.CharitiesRepaymentDashboardController.onPageLoad.url
    }

    "redirect Agent users to the agent dashboard if trafic split is enabled and user is allowed" in {
      val result = controller(UserType.Agent, useRateLimitedAllowList = true, isUserAllowed = Some(_ => true)).landingPage(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe
        controllers.routes.CharitiesRepaymentDashboardController.onPageLoad.url
    }

    "redirect Individual users to access denied if trafic split is enabled and user is allowed" in {
      val result = controller(UserType.Individual, useRateLimitedAllowList = true, isUserAllowed = Some(_ => true)).landingPage(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe
        controllers.routes.AccessDeniedController.onPageLoad.url
    }

    /*    "redirect Organisation users to the organisation dashboard if trafic split is enabled and user is not allowed" in {
      val result = controller(UserType.Organisation, useRateLimitedAllowList = true, isUserAllowed = Some(_ => false)).landingPage(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe "http://localhost:9020/charities"
    }

    "redirect Agent users to the agent dashboard if trafic split is enabled and user is not allowed" in {
      val result = controller(UserType.Agent, useRateLimitedAllowList = true, isUserAllowed = Some(_ => false)).landingPage(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe "http://localhost:9020/charities"
    }*/

    "redirect Individual users to access denied if trafic split is enabled and user is not allowed" in {
      val result = controller(UserType.Individual, useRateLimitedAllowList = true, isUserAllowed = Some(_ => false)).landingPage(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe
        controllers.routes.AccessDeniedController.onPageLoad.url
    }
  }

  private def controller(userType: UserType, useRateLimitedAllowList: Boolean, isUserAllowed: Option[String => Boolean]): HomeController =
    new HomeController(
      cc,
      authorisedAction(userType),
      new SplitterAction(
        appConfig(useRateLimitedAllowList),
        new RateLimitedAllowListConnector {
          override def checkAllowList(feature: String, charityReference: String)(using hc: HeaderCarrier): Future[Boolean] =
            Future.successful(isUserAllowed.map(_(charityReference)).getOrElse(false))
        }
      )
    )

  private def authorisedAction(userType: UserType) =
    userType match {
      case UserType.Agent        => fakeAgent()
      case UserType.Organisation => fakeOrg()
      case UserType.Individual   => fakeIndividual
    }
}
