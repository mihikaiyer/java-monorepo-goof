/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/trunk/sakai/sam/src/org/sakaiproject/tool/assessment/services/gradebook/GradebookServiceHelper.java $
   * $Id: GradebookServiceHelper.java 632 2005-07-14 21:22:50Z janderse@umich.edu $
 ***********************************************************************************
 *
 * Copyright (c) 2005 The Regents of the University of Michigan, Trustees of Indiana University,
 *                  Board of Trustees of the Leland Stanford, Jr., University, and The MIT Corporation
 *
   * Licensed under the Educational Community License Version 1.0 (the "License");
 * By obtaining, using and/or copying this Original Work, you agree that you have read,
 * understand, and will comply with the terms and conditions of the Educational Community License.
 * You may obtain a copy of the License at:
 *
 *      http://cvs.sakaiproject.org/licenses/license_1_0.html
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 **********************************************************************************/

package org.sakaiproject.tool.assessment.shared.api.grading;

import org.sakaiproject.tool.assessment.data.ifc.assessment.PublishedAssessmentIfc;
import org.sakaiproject.tool.assessment.data.ifc.grading.AssessmentGradingIfc;

/**
 * The GradebookServiceAPI describes an interface for gradebook information
 * for published assessments.
 *
 * @author Ed Smiley <esmiley@stanford.edu>
 */
public interface GradebookServiceAPI
{
  /**
   *
   * @param publishedAssessment
   * @return
   */
  public boolean addToGradebook(PublishedAssessmentIfc publishedAssessment) ;
  /**
   *
   * @param siteId
   * @param publishedAssessmentId
   */
  public void removeExternalAssessment(String siteId,
                                              String publishedAssessmentId);
  /**
   *
   * @param ag
   * @param agentIdString
   */
  public void updateExternalAssessment(AssessmentGradingIfc ag,
                                              String agentIdString);
  /**
   *
   * @param siteId
   * @return
   */
  public boolean gradebookExists(String siteId);

  /**
   *
   * @param ag
   */
  public void updateExternalAssessmentScore(AssessmentGradingIfc ag) ;

}
