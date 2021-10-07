package com.example.accesscontrol.rest.api

import play.api.libs.json.{Format, Json}

/**
 * The JSON representation for instances of a sealed family includes a discriminator field,
 * which specify the effective sub-type (a text field, with default name `_type` - fully-qualified name of sub-type).
 * For example: '{"_type": "AttributeValueInt", "value": 1}'
 */
sealed trait AttributeValue
case class AttributeValueString(value: String) extends AttributeValue
case class AttributeValueBool(value: Boolean) extends AttributeValue
case class AttributeValueInt(value: Int) extends AttributeValue

object AttributeValueString {
  /**
   * Format for converting access control attribute value string object to and from JSON.
   *
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[AttributeValueString] = Json.format[AttributeValueString]
}

object AttributeValueBool {
  /**
   * Format for converting access control attribute value bool object to and from JSON.
   *
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[AttributeValueBool] = Json.format[AttributeValueBool]
}

object AttributeValueInt {
  /**
   * Format for converting access control attribute value integer object to and from JSON.
   *
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[AttributeValueInt] = Json.format[AttributeValueInt]
}

object AttributeValue {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}
  /**
   * Format for converting access control attribute value object to and from JSON.
   *
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[AttributeValue] = Format[AttributeValue] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "int"    => JsPath.read[AttributeValueInt].reads(js)
          case "bool"   => JsPath.read[AttributeValueBool].reads(js)
          case "string" => JsPath.read[AttributeValueString].reads(js)
        }
      )
    },
    Writes {
      case bool: AttributeValueBool =>
        JsObject(
          Seq(
            "_type" -> JsString("bool"),
            "value" -> AttributeValueBool.format.writes(bool)
          )
        )
      case string: AttributeValueString =>
        JsObject(
          Seq(
            "_type" -> JsString("string"),
            "value" -> AttributeValueString.format.writes(string)
          )
        )
      case int: AttributeValueInt =>
        JsObject(
          Seq(
            "_type" -> JsString("int"),
            "value" -> AttributeValueInt.format.writes(int)
          )
        )
    }
  )
}

/**
 * The access control attribute class.
 * @param name - attribute name (unique)
 * @param value - attribute value (such types as String, Boolean, Int, etc)
 */
case class Attribute(name: String, value: AttributeValue)

object Attribute {
  /**
   * Format for converting access control attribute object to and from JSON.
   *
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[Attribute] = Json.format[Attribute]
}

/**
 * The access control target class.
 * @param objectType - object type (for example "bicycle")
 * @param objectId - object ID (for example 1)
 * @param action - action name (for example "ride")
 */
case class Target(objectType: String, objectId: Int, action: String)

object Target {
  /**
   * Format for converting access control attribute object to and from JSON.
   *
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[Target] = Json.format[Target]
}

/**
 * The access control request class.
 */
case class AccessControlRequest(targets: Array[Target], attributes: Array[Attribute])

object AccessControlRequest {
  /**
   * Format for converting access control attributes object to and from JSON.
   *
   *
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[AccessControlRequest] = Json.format[AccessControlRequest]
}
