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

package controllers.actions

import play.api.mvc.*
import connectors.RateLimitedAllowListConnector
import config.AppConfig
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.requests.AuthorisedRequest
import models.requests.UserType.*

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

@Singleton
class SplitterAction @Inject() (
  appConfig: AppConfig,
  rateLimitedAllowListConnector: RateLimitedAllowListConnector
)(implicit override val executionContext: ExecutionContext)
    extends Logging
    with ActionRefiner[AuthorisedRequest, AuthorisedRequest] {

  override protected def refine[A](request: AuthorisedRequest[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    for {
      isAllowed <-
        if appConfig.useRateLimitedAllowList
        then
          request.charityUser.referenceId match {
            case Some(referenceId) =>
              rateLimitedAllowListConnector.checkAllowList(appConfig.splitterAllowListName, referenceId)
            case None =>
              Future.successful(true)
          }
        else Future.successful(true)
    } yield
      if isAllowed
      then Right(request)
      else {
        val userTypeText = request.charityUser.userType match {
          case Agent => "agent"
          case _     => "org"
        }

        val url = s"${appConfig.legacyCharitiesServiceUrl}/$userTypeText/${request.charityUser.referenceId.get}/at-a-glance?lang=eng"
        logger.info(s"Redirecting to charities legacy service to $url")

        Left(Redirect(url))
      }
  }
}
