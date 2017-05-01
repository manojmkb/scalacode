package gov.hhs.cms.dsrs

/**
  * Defines several constants for DynamoDB table and field names that are used across flows
  */
object DSRSConstants {
  // Table names
  val METADATA_TABLE_NAME = "DocumentMetadata"
  val SEARCH_TABLE_NAME = "DocumentMetadataSearch"
  // Table keys
  val METADATA_TABLE_PARTITION_KEY = "dsrsId"

  // Field names
  val DOCUMENT_CATEGORY_CODE_FIELD_NAME = "documentCategoryCode"
  val DOCUMENT_FILENAME_FIELD_NAME = "documentFileName"
  val DOCUMENT_SUBCATEGORY_CODE_FIELD_NAME = "documentSubcategoryCode"
  val DSRS_ID_FIELD_NAME = "dsrsId"
  val FILE_FORMAT_FIELD_NAME = "fileFormatCode"
  val FILE_SIZE_FIELD_NAME = "fileSize"
  val SEARCH_ATTRIBUTE_FIELD_NAME = "searchAttribute"
  val SOURCE_SYSTEM_CODE_FIELD_NAME = "sourceSystemCode"
  val YEAR_FIELD_NAME = "year"
  // Audit field names
  val CREATED_USER_FIELD_NAME = "createdBy"
  val CREATION_DATE_TIME_FIELD_NAME = "creationDateTime"
  val LAST_MODIFIED_USER_FIELD_NAME = "modifiedBy"
  val LAST_MODIFIED_DATE_TIME_FIELD_NAME = "lastModifiedDateTime"
  val DOCUMENT_URI_FIELD_NAME = "documentURI"

  // Configuration file names
  val COMMON_CONFIG_NAME = "COMMON"
  val BUCKET_CONFIG_NAME = "BUCKET"
  val VALIDATION_RULES_CONFIG_NAME = "VALIDATION_RULES"
  val VALIDATION_ERROR_CONFIG_NAME = "VALIDATION_ERROR"
  // Configuration file constants
  val DOCUMENT_RULES_CONFIG = "documentRules"
  val FIELD_RULES_CONFIG = "fieldRules"
  val DATA_TYPE_CONFIG = "dataType"
  val ALLOWED_VALUES_CONFIG = "allowedValues"
  val VALIDATION_RULES_CONFIG = "validationRules"
  val NUMERIC_RULES_CONFIG = "numericRange"
  // Configuration rules
  val AT_LEAST_ONE_IDENTIFIER_TO_STORE_CONFIG_RULE = "atLeastOneIdentifierToStore"
  val REQUIRED_TO_STORE_CONFIG_RULE = "requiredToStore"
  val SEARCH_ATTRIBUTES_CONFIG_RULE = "searchAttributes"
  val UPDATABLE_FIELDS_CONFIG_RULE = "updatableFields"

  // Response field name
  val DSRS_ID_RESPONSE_FIELD_NAME = "dsrsIdentifier"

  // Other constants
  val NUMBER_OF_DIGITS_IN_A_YEAR = 4
}
