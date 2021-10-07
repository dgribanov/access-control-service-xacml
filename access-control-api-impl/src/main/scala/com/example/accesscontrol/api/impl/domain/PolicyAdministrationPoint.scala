package com.example.accesscontrol.api.impl.domain

import play.api.libs.json.{Format, JsError, JsSuccess, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

object PolicyAdministrationPoint {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  case class PolicyCollectionParsingError(errorMessage: String) extends DomainError

  def buildPolicyCollection(): Future[Either[PolicyCollectionParsingError, PolicyCollection]] = {
    Future {
      Json.fromJson[PolicyCollection](PolicyRepository.config) match {
        case JsSuccess(policyCollection, _) => Right(policyCollection)
        case JsError.Message(errMsg)        => Left(PolicyCollectionParsingError(errMsg))
      }
    }
  }
}

trait WithTargetType {
  val target: TargetType
}

object PolicyCollection {
  implicit val format: Format[PolicyCollection] = Json.format[PolicyCollection]
}

case class PolicyCollection(policySets: Array[PolicySet])

object PolicySet {
  implicit val format: Format[PolicySet] = Json.format[PolicySet]
}

case class PolicySet(target: TargetType, combiningAlgorithm: CombiningAlgorithm, policies: Array[Policy]) extends WithTargetType

sealed trait TargetType
case class ObjectTypeTarget(value: String) extends TargetType
case class ActionTypeTarget(value: String) extends TargetType
case class AttributeTypeTarget(value: String) extends TargetType

object ObjectTypeTarget {
  implicit val format: Format[ObjectTypeTarget] = Json.format[ObjectTypeTarget]
}

object ActionTypeTarget {
  implicit val format: Format[ActionTypeTarget] = Json.format[ActionTypeTarget]
}

object AttributeTypeTarget {
  implicit val format: Format[AttributeTypeTarget] = Json.format[AttributeTypeTarget]
}

object TargetType {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[TargetType] = Format[TargetType] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "ObjectTypeTarget"    => JsPath.read[ObjectTypeTarget].reads(js)
          case "ActionTypeTarget"    => JsPath.read[ActionTypeTarget].reads(js)
          case "AttributeTypeTarget" => JsPath.read[AttributeTypeTarget].reads(js)
        }
      )
    },
    Writes {
      case target: ObjectTypeTarget =>
        JsObject(
          Seq(
            "_type" -> JsString("ObjectTypeTarget"),
            "id"    -> ObjectTypeTarget.format.writes(target)
          )
        )
      case target: ActionTypeTarget =>
        JsObject(
          Seq(
            "_type" -> JsString("ActionTypeTarget"),
            "value" -> ActionTypeTarget.format.writes(target)
          )
        )
      case target: AttributeTypeTarget =>
        JsObject(
          Seq(
            "_type" -> JsString("AttributeTypeTarget"),
            "value" -> AttributeTypeTarget.format.writes(target)
          )
        )
    }
  )
}

sealed trait CombiningAlgorithm
case class DenyOverride(algorithm: String) extends CombiningAlgorithm
case class PermitOverride(algorithm: String) extends CombiningAlgorithm

object DenyOverride {
  implicit val format: Format[DenyOverride] = Json.format[DenyOverride]
}

object PermitOverride {
  implicit val format: Format[PermitOverride] = Json.format[PermitOverride]
}

object CombiningAlgorithm {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[CombiningAlgorithm] = Format[CombiningAlgorithm] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "algorithm").read[String].reads(js)
      valueType.fold(
        _ => JsError("algorithm undefined or incorrect"),
        {
          case "deny-override"   => JsPath.read[DenyOverride].reads(js)
          case "permit-override" => JsPath.read[PermitOverride].reads(js)
        }
      )
    },
    Writes {
      case _: DenyOverride =>
        JsObject(
          Seq(
            "algorithm" -> JsString("deny-override"),
          )
        )
      case _: PermitOverride =>
        JsObject(
          Seq(
            "algorithm" -> JsString("permit-override"),
          )
        )
    }
  )
}

object Policy {
  implicit val format: Format[Policy] = Json.format[Policy]
}

case class Policy(target: TargetType, combiningAlgorithm: CombiningAlgorithm, rules: Array[Rule]) extends WithTargetType

object Rule {
  implicit val format: Format[Rule] = Json.format[Rule]
}

case class Rule(target: TargetType, condition: Condition, positiveEffect: PositiveEffect, negativeEffect: NegativeEffect)

sealed trait Condition
case class CompareCondition(operation: String, leftOperand: ExpressionParameterValue, rightOperand: ExpressionParameterValue) extends Condition
case class CompositeCondition(predicate: String, leftCondition: Condition, rightCondition: Condition) extends Condition

object CompareCondition {
  implicit val format: Format[CompareCondition] = Json.format[CompareCondition]
}

object CompositeCondition {
  implicit val format: Format[CompositeCondition] = Json.format[CompositeCondition]
}

object Condition {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[Condition] = Format[Condition] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "CompareCondition"   => JsPath.read[CompareCondition].reads(js)
          case "CompositeCondition" => JsPath.read[CompositeCondition].reads(js)
        }
      )
    },
    Writes {
      case condition: CompareCondition =>
        JsObject(
          Seq(
            "_type"        -> JsString("CompareCondition"),
            "operation"    -> CompareCondition.format.writes(condition),
            "leftOperand"  -> CompareCondition.format.writes(condition),
            "rightOperand" -> CompareCondition.format.writes(condition)
          )
        )
      case condition: CompositeCondition =>
        JsObject(
          Seq(
            "_type"          -> JsString("CompositeCondition"),
            "predicate"      -> CompositeCondition.format.writes(condition),
            "leftCondition"  -> CompositeCondition.format.writes(condition),
            "rightCondition" -> CompositeCondition.format.writes(condition)
          )
        )
    }
  )
}

sealed trait Effect
case class PositiveEffect(decision: String) extends Effect
case class NegativeEffect(decision: String) extends Effect

object PositiveEffect {
  implicit val format: Format[PositiveEffect] = Json.format[PositiveEffect]
}

object NegativeEffect {
  implicit val format: Format[NegativeEffect] = Json.format[NegativeEffect]
}

sealed trait ExpressionParameterValue
case class AttributeParameterValue(id: String) extends ExpressionParameterValue
case class BoolParameterValue(value: Boolean) extends ExpressionParameterValue
case class IntParameterValue(value: Int) extends ExpressionParameterValue
case class StringParameterValue(value: String) extends ExpressionParameterValue

object ExpressionParameterValue {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[ExpressionParameterValue] = Format[ExpressionParameterValue] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "AttributeValue" => JsPath.read[AttributeParameterValue].reads(js)
          case "BoolValue"      => JsPath.read[BoolParameterValue].reads(js)
          case "IntValue"       => JsPath.read[IntParameterValue].reads(js)
          case "StringValue"    => JsPath.read[StringParameterValue].reads(js)
        }
      )
    },
    Writes {
      case attr: AttributeParameterValue =>
        JsObject(
          Seq(
            "_type" -> JsString("AttributeValue"),
            "id"    -> AttributeParameterValue.format.writes(attr)
          )
        )
      case boolVal: BoolParameterValue =>
        JsObject(
          Seq(
            "_type" -> JsString("BoolValue"),
            "value" -> BoolParameterValue.format.writes(boolVal)
          )
        )
      case intVal: IntParameterValue =>
        JsObject(
          Seq(
            "_type" -> JsString("IntValue"),
            "value" -> IntParameterValue.format.writes(intVal)
          )
        )
      case stringVal: StringParameterValue =>
        JsObject(
          Seq(
            "_type" -> JsString("StringValue"),
            "value" -> StringParameterValue.format.writes(stringVal)
          )
        )
    }
  )
}

object AttributeParameterValue {
  implicit val format: Format[AttributeParameterValue] = Json.format[AttributeParameterValue]
}

object BoolParameterValue {
  implicit val format: Format[BoolParameterValue] = Json.format[BoolParameterValue]
}

object IntParameterValue {
  implicit val format: Format[IntParameterValue] = Json.format[IntParameterValue]
}

object StringParameterValue {
  implicit val format: Format[StringParameterValue] = Json.format[StringParameterValue]
}

object PolicyRepository {
  val config: JsValue = Json.parse("""
  {"policySets": [
    {
      "_type": "PolicySet",
      "target": {
        "_type": "ObjectTypeTarget",
        "value": "bicycle"
      },
      "combiningAlgorithm": {
        "_type": "CombiningAlgorithm",
        "algorithm": "deny-override"
      },
      "policies": [{
        "_type": "Policy",
        "target": {
          "_type": "ActionTypeTarget",
          "value": "ride"
        },
        "combiningAlgorithm": {
          "_type": "CombiningAlgorithm",
          "algorithm": "deny-override"
        },
        "rules": [{
          "_type": "Rule",
          "target": {
            "_type": "AttributeTypeTarget",
            "value": "permissionToRideBicycle"
          },
          "positiveEffect": {
            "_type": "PositiveEffect",
            "decision": "Permit"
          },
          "negativeEffect": {
            "_type": "NegativeEffect",
            "decision": "Deny"
          },
          "condition": {
            "_type": "CompareCondition",
            "operation": "eq",
            "leftOperand": {
              "_type": "AttributeValue",
              "id": "permissionToRideBicycle"
            },
            "rightOperand": {
              "_type": "BoolValue",
              "value": true
            }
          }
        }]
      },
      {
        "_type": "Policy",
        "target": {
          "_type": "ActionTypeTarget",
          "value": "rent"
        },
        "combiningAlgorithm": {
          "_type": "CombiningAlgorithm",
          "algorithm": "permit-override"
        },
        "rules": [{
          "_type": "Rule",
          "target": {
            "_type": "AttributeTypeTarget",
            "value": "permissionToRentBicycle"
          },
          "positiveEffect": {
            "_type": "PositiveEffect",
            "decision": "Permit"
          },
          "negativeEffect": {
            "_type": "NegativeEffect",
            "decision": "Deny"
          },
          "condition": {
            "_type": "CompositeCondition",
            "predicate": "and",
            "leftCondition": {
              "_type": "CompareCondition",
              "operation": "eq",
              "leftOperand": {
                "_type": "AttributeValue",
                "id": "permissionToRentBicycle"
              },
              "rightOperand": {
                "_type": "BoolValue",
                "value": true
              }
            },
            "rightCondition": {
              "_type": "CompositeCondition",
              "predicate": "or",
              "leftCondition": {
                "_type": "CompareCondition",
                "operation": "gte",
                "leftOperand": {
                  "_type": "AttributeValue",
                  "id": "personAge"
                },
                "rightOperand": {
                  "_type": "IntValue",
                  "value": 18
                }
              },
              "rightCondition": {
                "_type": "CompareCondition",
                "operation": "eq",
                "leftOperand": {
                  "_type": "AttributeValue",
                  "id": "bicycleType"
                },
                "rightOperand": {
                  "_type": "StringValue",
                  "value": "tricycle"
                }
              }
            }
          }
        }]
      }]
    },
    {
      "_type": "PolicySet",
      "target": {
        "_type": "ObjectTypeTarget",
        "value": "skateboard"
      },
      "combiningAlgorithm": {
        "_type": "CombiningAlgorithm",
        "algorithm": "deny-override"
      },
      "policies": [{
        "_type": "Policy",
        "target": {
          "_type": "ActionTypeTarget",
          "value": "ride"
        },
        "combiningAlgorithm": {
          "_type": "CombiningAlgorithm",
          "algorithm": "deny-override"
        },
        "rules": [{
          "_type": "Rule",
          "target": {
            "_type": "AttributeTypeTarget",
            "value": "permissionToRideSkateboard"
          },
          "positiveEffect": {
            "_type": "PositiveEffect",
            "decision": "Permit"
          },
          "negativeEffect": {
            "_type": "NegativeEffect",
            "decision": "Deny"
          },
          "condition": {
            "_type": "CompareCondition",
            "operation": "eq",
            "leftOperand": {
              "_type": "AttributeValue",
              "id": "permissionToRideSkateboard"
            },
            "rightOperand": {
              "_type": "BoolValue",
              "value": true
            }
          }
        },
        {
          "_type": "Rule",
          "target": {
            "_type": "AttributeTypeTarget",
            "value": "placeType"
          },
          "positiveEffect": {
            "_type": "PositiveEffect",
            "decision": "Deny"
          },
          "negativeEffect": {
            "_type": "NegativeEffect",
            "decision": "Permit"
          },
          "condition": {
            "_type": "CompareCondition",
            "operation": "eq",
            "leftOperand": {
              "_type": "AttributeValue",
              "id": "placeType"
            },
            "rightOperand": {
              "_type": "StringValue",
              "value": "office"
            }
          }
        }]
      }]
    },
    {
      "_type": "PolicySet",
      "target": {
        "_type": "ObjectTypeTarget",
        "value": "scooter"
      },
      "combiningAlgorithm": {
        "_type": "CombiningAlgorithm",
        "algorithm": "permit-override"
      },
      "policies": [{
        "_type": "Policy",
        "target": {
          "_type": "ActionTypeTarget",
          "value": "ride"
        },
        "combiningAlgorithm": {
          "_type": "CombiningAlgorithm",
          "algorithm": "permit-override"
        },
        "rules": [{
          "_type": "Rule",
          "target": {
            "_type": "AttributeTypeTarget",
            "value": "permissionToRideScooter"
          },
          "positiveEffect": {
            "_type": "PositiveEffect",
            "decision": "Permit"
          },
          "negativeEffect": {
            "_type": "NegativeEffect",
            "decision": "Deny"
          },
          "condition": {
            "_type": "CompareCondition",
            "operation": "eq",
            "leftOperand": {
              "_type": "AttributeValue",
              "id": "permissionToRideScooter"
            },
            "rightOperand": {
              "_type": "BoolValue",
              "value": true
            }
          }
        },
        {
          "_type": "Rule",
          "target": {
            "_type": "AttributeTypeTarget",
            "value": "profession"
          },
          "positiveEffect": {
            "_type": "PositiveEffect",
            "decision": "Permit"
          },
          "negativeEffect": {
            "_type": "NegativeEffect",
            "decision": "Deny"
          },
          "condition": {
            "_type": "CompareCondition",
            "operation": "eq",
            "leftOperand": {
              "_type": "AttributeValue",
              "id": "profession"
            },
            "rightOperand": {
              "_type": "StringValue",
              "value": "courier"
            }
          }
        }]
      }]
    }
  ]}
  """)
}