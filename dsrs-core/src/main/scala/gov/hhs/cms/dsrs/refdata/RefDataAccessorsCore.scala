package gov.hhs.cms.dsrs.refdata

import gov.hhs.cms.arch.refdata.ReferenceData


trait RefDataAccessorsCore {

  /**
    * Gets the refdata
    * @return refdata from cache
    */
  def getRefData: ReferenceData

}