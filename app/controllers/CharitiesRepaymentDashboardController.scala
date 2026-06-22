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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import connectors.ClaimsConnector
import config.AppConfig
import views.html.{CharityRepaymentDashboardAgentView, CharityRepaymentDashboardView}
import controllers.actions.BaseAuthorisedAction
import com.google.inject.name.Named
import models.requests.UserType
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject
import services.PaginationService
import javax.inject.Singleton
import controllers.actions.SplitterAction

@Singleton
class CharitiesRepaymentDashboardController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  @Named("identifyAuth") authorisedAction: BaseAuthorisedAction,
  splitterAction: SplitterAction,
  config: AppConfig,
  claimsConnector: ClaimsConnector,
  organisationView: CharityRepaymentDashboardView,
  agentView: CharityRepaymentDashboardAgentView
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    authorisedAction
      .andThen(splitterAction)
      .async { implicit request =>
        request.charityUser.referenceId match {
          case Some(referenceId) if request.charityUser.userType == UserType.Organisation =>
            for
              orgName           <- claimsConnector.getOrganisationName(referenceId).map(_.organisationName)
              getClaimsResponse <- claimsConnector.retrieveUnsubmittedClaims
            yield Ok(
              organisationView(
                referenceId,
                if getClaimsResponse.claimsCount == 1 then config.makeCharityRepaymentClaimUrl
                else config.makeCharityRepaymentClaimUrl + "?claimId=blank",
                orgName,
                config.giftAidOtherIncomeCommunityBuildingsUrl,
                config.hmrcServicesHomeUrl,
                getClaimsResponse.claimsCount == 1,
                config.hmrcRecognisedSoftwareUrl
              )
            )

          case Some(referenceId) if request.charityUser.userType == UserType.Agent =>
            for
              agentName         <- claimsConnector.getAgentName(referenceId).map(_.agentName)
              getClaimsResponse <- claimsConnector.retrieveUnsubmittedClaims
            yield {
              val currentPage = request.getQueryString("page").flatMap(_.toIntOption).getOrElse(1)
              val paginationResult = PaginationService.paginateClaims(
                allClaims = getClaimsResponse.claimsList,
                currentPage = currentPage,
                baseUrl = routes.CharitiesRepaymentDashboardController.onPageLoad.url
              )
              Ok(
                agentView(
                  referenceId,
                  config.makeCharityRepaymentClaimUrl,
                  config.makeCharityRepaymentClaimAgentUrl + "?claimId=blank",
                  config.deleteAgentClaimUrl + "?claimId={claimId}",
                  agentName,
                  config.giftAidOtherIncomeCommunityBuildingsUrl,
                  config.hmrcServicesHomeUrl,
                  config.hmrcRecognisedSoftwareUrl,
                  paginationViewModel = paginationResult.paginationViewModel,
                  paginationStatus = paginationResult,
                  claims = paginationResult.paginatedData
                )
              )
            }

          case _ =>
            Future.successful(Redirect(controllers.routes.AccessDeniedController.onPageLoad))
        }
      }

}
