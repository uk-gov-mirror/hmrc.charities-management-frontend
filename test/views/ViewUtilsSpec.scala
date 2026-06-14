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

package views

import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.{Lang, Messages, MessagesApi}
import services.PaginationStatus
import util.ViewSpec

import java.time.{Instant, ZoneId, ZonedDateTime}

class ViewUtilsSpec extends ViewSpec {

  private def emptyForm: Form[String] = Form(single("field" -> text))
  private def formWithError: Form[String] =
    emptyForm.withError("field", "error.required")

  private val serviceName = messages("service.name")
  private val govuk       = messages("site.govuk")
  private val errorPrefix = messages("error.browser.title.prefix")

  "ViewUtils.titleNoForm" should {
    "build title without section" in {
      val result = ViewUtils.titleNoForm("My Page")(messages)
      result mustBe s"My Page - $serviceName - $govuk"
    }

    "build title with section" in {
      val sectionKey = "charityRepaymentDashboard.heading"
      val result     = ViewUtils.titleNoForm("My Page", Some(sectionKey))(messages)
      result mustBe s"My Page - ${messages(sectionKey)} - $serviceName - $govuk"
    }
  }

  "ViewUtils.title (with Form)" should {
    "not include error prefix when form has no errors" in {
      val result = ViewUtils.title(emptyForm, "charityRepaymentDashboard.title")(messages)
      result must not include errorPrefix
    }

    "include error prefix when form has errors" in {
      val result = ViewUtils.title(formWithError, "charityRepaymentDashboard.title")(messages)
      result must include(errorPrefix)
    }
  }

  "ViewUtils.titleWithErrorOpt" should {
    "not include error prefix when error option is None" in {
      val result = ViewUtils.titleWithErrorOpt(None, "charityRepaymentDashboard.title")(messages)
      result must not include errorPrefix
    }

    "include error prefix when error option is Some" in {
      val result = ViewUtils.titleWithErrorOpt(Some("an error"), "charityRepaymentDashboard.title")(messages)
      result must include(errorPrefix)
    }
  }

  "ViewUtils.paginationSuffix" should {
    "return empty string when there is only 1 page" in {
      val pagination = new PaginationStatus {
        val totalRecords = 5
        val currentPage  = 1
        val totalPages   = 1
      }
      ViewUtils.paginationSuffix(pagination)(messages) mustBe ""
    }

    "return page suffix when there are multiple pages" in {
      val pagination = new PaginationStatus {
        val totalRecords = 25
        val currentPage  = 2
        val totalPages   = 3
      }
      val result = ViewUtils.paginationSuffix(pagination)(messages)
      result must include(messages("site.pagination.page"))
      result must include("2")
      result must include(messages("site.pagination.of"))
      result must include("3")
    }
  }

  "ViewUtils.titleWithPagination (with Form)" should {
    "include pagination suffix when multiple pages" in {
      val pagination = new PaginationStatus {
        val totalRecords = 25
        val currentPage  = 2
        val totalPages   = 3
      }
      val result = ViewUtils.titleWithPagination(emptyForm, "charityRepaymentDashboard.title", pagination)(messages)
      result must include(messages("site.pagination.page"))
      result must include("2")
    }

    "not include pagination suffix when single page" in {
      val pagination = new PaginationStatus {
        val totalRecords = 5
        val currentPage  = 1
        val totalPages   = 1
      }
      val result = ViewUtils.titleWithPagination(emptyForm, "charityRepaymentDashboard.title", pagination)(messages)
      result must not include messages("site.pagination.page")
    }
  }

  "ViewUtils.titleWithPagination (without Form)" should {
    "include pagination suffix when multiple pages" in {
      val pagination = new PaginationStatus {
        val totalRecords = 25
        val currentPage  = 2
        val totalPages   = 3
      }
      val result = ViewUtils.titleWithPagination("charityRepaymentDashboard.title", pagination)(messages)
      result must include(messages("site.pagination.page"))
      result must include("2")
    }
  }

  "ViewUtils.formatClaimDateTime" should {
    "return today message for a timestamp from today" in {
      val nowMillis = Instant.now().toEpochMilli
      val result    = ViewUtils.formatClaimDateTime(nowMillis)(messages)
      result mustBe messages("date.today")
    }

    "return yesterday message for a timestamp from yesterday" in {
      val yesterdayMillis =
        ZonedDateTime
          .now(ZoneId.of("Europe/London"))
          .minusDays(1)
          .toInstant
          .toEpochMilli
      val result = ViewUtils.formatClaimDateTime(yesterdayMillis)(messages)
      result mustBe messages("date.yesterday")
    }

    "return formatted date for older timestamps" in {
      // 1 Jan 2024
      val millis = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/London")).toInstant.toEpochMilli
      val result = ViewUtils.formatClaimDateTime(millis)(messages)
      result mustBe "01 Jan 2024"
    }

    "return formatted date for a specific known date" in {
      // 15 Jun 2023
      val millis = ZonedDateTime.of(2023, 6, 15, 9, 30, 0, 0, ZoneId.of("Europe/London")).toInstant.toEpochMilli
      val result = ViewUtils.formatClaimDateTime(millis)(messages)
      result mustBe "15 Jun 2023"
    }

    "return Welsh month names when Welsh messages are used" in {
      val messagesApi = app.injector.instanceOf[MessagesApi]
      given Messages  = messagesApi.preferred(Seq(Lang("cy")))

      val millis =
        ZonedDateTime
          .of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/London"))
          .toInstant
          .toEpochMilli

      ViewUtils.formatClaimDateTime(millis) mustBe "01 Ion 2024"
    }
  }
}
