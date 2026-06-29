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

import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers.*
import uk.gov.hmrc.charitiesmanagementfrontend.stubs.AuthStub
import utils.ComponentSpecHelper

class HomeControllerISpec extends ComponentSpecHelper with AuthStub {
  
  "GET /" should {

    "redirect to charities repayment dashboard for Organisation/Agent" in {
      stubAuthRequest()

      val result = get("/")
      result.status shouldBe SEE_OTHER
      result.header(LOCATION).value shouldBe controllers.routes.CharitiesRepaymentDashboardController.onPageLoad.url
    }
  }

  "redirect Individual user to access denied" in {
    stubIndividualAuthRequest()

    val result = get("/")
    result.status shouldBe SEE_OTHER
    result.header(LOCATION).value shouldBe controllers.routes.AccessDeniedController.onPageLoad.url
  }
}
