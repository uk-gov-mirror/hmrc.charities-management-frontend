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

package controllers.home

import javax.inject.Singleton
import com.google.inject.name.Named
import controllers.actions.BaseAuthorisedAction
import models.requests.UserType.{Agent, Organisation}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.Future
import connectors.RateLimitedAllowListConnector
import config.AppConfig
import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  appConfig: AppConfig,
  rateLimitedAllowListConnector: RateLimitedAllowListConnector,
  @Named("identifyAuth") identifyUser: BaseAuthorisedAction
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def landingPage: Action[AnyContent] = identifyUser.async { implicit request =>
    request.charityUser.userType match {
      case Organisation | Agent =>
        for {
          isAllowed <-
            if appConfig.useRateLimitedAllowList
            then rateLimitedAllowListConnector.checkAllowList(appConfig.splitterAllowListName, request.charityUser.referenceId.get)
            else Future.successful(true)
        } yield
          if isAllowed
          then Redirect(controllers.routes.CharitiesRepaymentDashboardController.onPageLoad)
          else {
            val userTypeText = request.charityUser.userType match {
              case Organisation => "org"
              case Agent        => "agent"
            }

            val url = s"${appConfig.legacyCharitiesServiceUrl}/$userTypeText/${request.charityUser.referenceId}/at-a-glance?lang=eng"
            logger.info(s"Redirecting to charities legacy service to $url")

            Redirect(url)
          }

      case _ =>
        logger.warn(s"Unrecognised user type, redirecting to access denied")
        Future.successful(Redirect(controllers.routes.AccessDeniedController.onPageLoad))
    }
  }
}
