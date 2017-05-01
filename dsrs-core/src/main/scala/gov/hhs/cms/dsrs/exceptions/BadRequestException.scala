package gov.hhs.cms.dsrs.exceptions

import gov.hhs.cms.arch.exception.AbstractRuntimeException

/**
  * Created by Charlie Davis on 2/28/17.
  */
class BadRequestException(message: String, exception: Exception)
  extends AbstractRuntimeException(message, exception)
