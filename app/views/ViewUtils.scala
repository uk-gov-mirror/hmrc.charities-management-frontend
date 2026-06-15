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
import play.api.i18n.Messages
import services.PaginationStatus

import java.time.{Instant, ZoneId, ZonedDateTime}

object ViewUtils {

  def title(form: Form[?], title: String, section: Option[String] = None)(implicit messages: Messages): String =
    titleNoForm(
      title = s"${errorPrefix(form)} ${messages(title)}",
      section = section
    )

  def titleWithErrorOpt(error: Option[?], title: String, section: Option[String] = None)(implicit
    messages: Messages
  ): String =
    titleNoForm(
      title = s"${if (error.nonEmpty) messages("error.browser.title.prefix") + " " else ""}${messages(title)}",
      section = section
    )

  def titleWithPagination(
    form: Form[?],
    title: String,
    pagination: PaginationStatus
  )(implicit
    messages: Messages
  ): String =
    titleNoForm(
      title = s"${errorPrefix(form)} ${messages(title)}${paginationSuffix(pagination)}",
      section = None
    )

  def titleWithPagination(
    title: String,
    pagination: PaginationStatus
  )(implicit
    messages: Messages
  ): String =
    titleNoForm(
      title = s"${messages(title)}${paginationSuffix(pagination)}",
      section = None
    )

  def paginationSuffix(pagination: PaginationStatus)(implicit messages: Messages): String =
    if (pagination.totalPages > 1)
      s" (${messages("site.pagination.page")} ${pagination.currentPage} ${messages("site.pagination.of")} ${pagination.totalPages})"
    else
      ""

  def titleNoForm(title: String, section: Option[String] = None)(implicit messages: Messages): String =
    s"${messages(title)} - ${section.fold("")(messages(_) + " - ")}${messages("service.name")} - ${messages("site.govuk")}"

  private def errorPrefix(form: Form[?])(implicit messages: Messages): String =
    if (form.hasErrors || form.hasGlobalErrors) messages("error.browser.title.prefix") else ""

  def formatClaimDateTime(lastVisitedAt: Long)(implicit messages: Messages): String =
    val date  = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastVisitedAt), ZoneId.of("Europe/London"))
    val today = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("Europe/London")).toLocalDate
    if (date.toLocalDate.isEqual(today)) {
      messages("date.today")
    } else if (date.toLocalDate.isEqual(today.minusDays(1))) {
      messages("date.yesterday")
    } else {
      val day   = f"${date.getDayOfMonth}%02d"
      val month = messages(s"month.${date.getMonthValue}.short")
      s"$day $month ${date.getYear}"
    }

}
