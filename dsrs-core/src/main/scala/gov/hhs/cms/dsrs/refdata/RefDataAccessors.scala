package gov.hhs.cms.dsrs.refdata

import gov.hhs.cms.arch.refdata.ReferenceData

/**
  * Ref data accessors implementation
  * @param refData the ReferenceData to be used
  */
class RefDataAccessors(val refData: ReferenceData) extends RefDataAccessorsCore {
  override def getRefData: ReferenceData = this.refData
}